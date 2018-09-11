package com.progetto.user.speedo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.P2pCurrentRun;
import com.progetto.user.speedo.RoomPackage.P2pRunBasicInfo;
import com.progetto.user.speedo.RoomPackage.SocialCurrentRun;
import com.progetto.user.speedo.RoomPackage.SocialRunBasicInfo;
import com.progetto.user.speedo.RoomPackage.SocialRunDao;
import com.progetto.user.speedo.RoomPackage.SocialStats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Servizio per la condivisione di una corsa via wifi direct.
 * 4 possibili modalità:
 * - Group owner sender / receiver
 * - Client sender / receiver
 */
public class P2pTransferService extends JobIntentService {
    private static final String TAG = "P2PTRANSFERSERVICE";

    static final int JOB_ID = 9725;
    static final int NOTIFICATION_ID = 7686;
    private static final int NOTIFICATION_STACKBUILDER_ID = 4441;

    static final String ACTION_NOTY_CLICK = "com.progetto.user.speedo.NOTY_CLICK";
    static final String KEY_START_FROM_NOTY = "com.progetto.user.speedo.TAB";

    public static final String ACTION_GO_SEND = "com.progetto.user.speedo.GO_SEND";
    public static final String ACTION_SEND = "com.progetto.user.speedo.SEND";
    public static final String ACTION_RECEIVE = "com.progetto.user.speedo.RECEIVE";
    public static final String ACTION_GO_RECEIVE = "com.progetto.user.speedo.GO_RECEIVE";
    public static final String EXTRAS_RUN_KEY = "run_key";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "group_owner_address";
    public static final String EXTRAS_GROUP_OWNER_PORT = "group_owner_port";

    // -1 : First time
    // 0 : Sto trasferendo
    // 1 : Ho finito di trasferire
    // 2 : Sono stato notificato e posso ricominciare
    public static final String KEY_IS_TRANSFERRING = "com.progetto.user.speedo.IS_TRANSFERRING";

    private static final int SOCKET_TIMEOUT = 15000;
    private static final int USER_TIMEOUT = 10000;
    private static final int SERVER_TIMEOUT = 30000;

    private NotificationCompat.Builder notification;
    private NotificationManager notificationManager;
    private SocialRunDao socialRunDao;


    @Override
    public void onCreate() {
        super.onCreate();
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        socialRunDao = db.socialRunDao();
        notification = new NotificationCompat.Builder(getApplicationContext(), LocationBroadcastReceiver.CH_ID);
        notificationManager = (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        Log.d(TAG, "onCreate: ");
    }

    @Override
    protected void onHandleWork(@Nullable Intent intent) {
        String msg = getString(R.string.error);
        showNoty();
        String action;
        if (intent != null) {
            action = intent.getAction();
            if (action != null) {
                Bundle extra = intent.getExtras();
                if (extra != null) {
                    if (action.equals(ACTION_GO_SEND) || action.equals(ACTION_GO_RECEIVE)) {
                        Log.d(TAG, "onHandleWork: SERVER RECEIVER OR SENDER");
                        msg = groupOwnerMode(extra, action);
                    } else if (action.equals(ACTION_RECEIVE) || action.equals(ACTION_SEND)) {
                        Log.d(TAG, "onHandleWork: CLIENT RECEIVER OR SENDER");
                        msg = groupClientMode(extra, action);

                    } else {
                        Log.d(TAG, "onHandleWork: UN'ACTION SCONOSCIUTA " + action);
                    }
                }
            }
        }
        updateNoty(msg);
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt(KEY_IS_TRANSFERRING, 1).apply();
    }

    /**
     * Client della connessione in wifi direct
     *
     * @param extra  info sulla connessione
     * @param action azione (mittente o destinatario)
     * @return risultato della operazione
     */
    private String groupClientMode(Bundle extra, String action) {
        String host = extra.getString(EXTRAS_GROUP_OWNER_ADDRESS);
        int port = extra.getInt(EXTRAS_GROUP_OWNER_PORT);
        Socket socket = null;
        try {
            socket = connectToServer(host, port);
            switch (action) {
                case ACTION_RECEIVE:
                    Log.d(TAG, "groupClientMode: RECEIVER");
                    receiveData(socket);
                    break;
                case ACTION_SEND:
                    Log.d(TAG, "groupClientMode: SENDER");
                    long runId = extra.getLong(EXTRAS_RUN_KEY);
                    Pair<P2pRunBasicInfo, List<P2pCurrentRun>> data = retrieveDataFromDb(runId);
                    sendData(socket, data);
                    break;
            }
        } catch (IOException e) {
            Log.d(TAG, "groupClientMode: EXCEPTION " + e.getMessage());
            String msg = e.getMessage();
            if (msg == null || msg.equals("")) msg = getString(R.string.error);
            return msg;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return getString(R.string.success_share);
    }

    /**
     * Server della connessione in wifi direct
     *
     * @param extra  info sulal connessione
     * @param action azione (mittente o destinatario)
     * @return risultato della operazione
     */
    private String groupOwnerMode(Bundle extra, String action) {
        int port = extra.getInt(EXTRAS_GROUP_OWNER_PORT);
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            Log.d(TAG, "groupOwnerMode: SERVER SOCKET APERTA");
            serverSocket.setSoTimeout(SERVER_TIMEOUT);
            Socket client = serverSocket.accept();
            switch (action) {
                case ACTION_GO_RECEIVE:
                    receiveData(client);
                    break;

                case ACTION_GO_SEND:
                    long runId = extra.getLong(EXTRAS_RUN_KEY);
                    Pair<P2pRunBasicInfo, List<P2pCurrentRun>> data = retrieveDataFromDb(runId);
                    sendData(client, data);
                    break;

            }

        } catch (IOException e) {
            Log.d(TAG, "groupOwnerMode: EXCEPTION " + e.getMessage());
            String msg = e.getMessage();
            if (msg == null || msg.equals("")) msg = getString(R.string.error);
            return msg;
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return getString(R.string.success_share);
    }

    /**
     * Preleva le info base e dettagliate della corsa
     * da inviare via wifi direct dal database
     *
     * @param runId Chiave primaria ed esterna della corsa da condividere
     * @return ritorna una coppia contenente le info base e dettagliate della corsa
     */

    private Pair<P2pRunBasicInfo, List<P2pCurrentRun>> retrieveDataFromDb(long runId) throws IOException {
        P2pRunBasicInfo runBasicInfo = socialRunDao.getP2pRunBasicInfo(runId);
        if (runBasicInfo == null) {
            Log.d(TAG, "retrieveDataFromDb: RUNBASICINFO NULL");
            throw new IOException(getString(R.string.share_retrieve_error));
        }
        List<P2pCurrentRun> currentRunList = socialRunDao.getP2pCurrentRun(runId);
        if (currentRunList == null || currentRunList.size() == 0) {
            Log.d(TAG, "retrieveDataFromDb: CURRENTRUNLIST NULLO O VUOTO");
            throw new IOException(getString(R.string.share_retrieve_error));
        }
        return Pair.create(runBasicInfo, currentRunList);
    }

    /**
     * Riceve i dati condivisi
     *
     * @param socket socket da dove leggere i dati
     * @throws IOException se la lettura fallisce o non viene letto nulla
     */
    private void receiveData(Socket socket) throws IOException {
        socket.setSoTimeout(SOCKET_TIMEOUT);
        InputStream inputStream = socket.getInputStream();
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        String senderUsername = r.readLine();
        String jsonRunBasicInfo = r.readLine();
        String jsonCurrentRuns = r.readLine();
        Log.d(TAG, "receiveData: JSON BASIC " + jsonRunBasicInfo + " JSON RUNS " + jsonCurrentRuns);
        if (jsonRunBasicInfo == null || jsonCurrentRuns == null)
            throw new IOException(getString(R.string.share_receive_error));
        Gson gson = new Gson();
        SocialRunBasicInfo runBasicInfo = gson.fromJson(jsonRunBasicInfo, SocialRunBasicInfo.class);
        Type listType = new TypeToken<ArrayList<SocialCurrentRun>>() {
        }.getType();
        List<SocialCurrentRun> currentRuns = gson.fromJson(jsonCurrentRuns, listType);
        runBasicInfo.setSenderName(senderUsername);
        runBasicInfo.setReceiveDate(System.currentTimeMillis());
        insertSocialRun(runBasicInfo, currentRuns);
        Log.d(TAG, "receiveData: TMP3 " + runBasicInfo.toString() + " TMP4 " + currentRuns.toString());
    }

    /**
     * Inserisce le informazioni ricevute all'interno del db
     *
     * @param runBasicInfo informazioni base sulla corsa
     * @param currentRuns  informazioni dettagliate sulla corsa
     */
    private void insertSocialRun(SocialRunBasicInfo runBasicInfo, List<SocialCurrentRun> currentRuns) {
        increaseReceivedCounter();
        long key = socialRunDao.insertNewSocialRun(runBasicInfo);
        for (SocialCurrentRun s : currentRuns) {
            s.setExtRun_id(key);
        }
        socialRunDao.insertSocialRunInfo(currentRuns);
    }

    /**
     * Invia i dati della corsa da condividere sulla socket
     *
     * @param socket socket su cui mandare i dati
     * @param data   dati da inviare
     * @throws IOException se errore nell'invio
     */
    private void sendData(Socket socket, Pair<P2pRunBasicInfo, List<P2pCurrentRun>> data) throws IOException {
        String username = getUsername();
        OutputStream ostream = socket.getOutputStream();
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(ostream));
        Gson gson = new Gson();
        String tmp = gson.toJson(data.first);
        String tmp2 = gson.toJson(data.second);
        Log.d(TAG, "sendData: TMP " + tmp + " TMP2 " + tmp2);
        w.write(username);
        w.newLine();
        w.write(tmp);
        w.newLine();
        w.write(tmp2);
        w.newLine();
        w.flush();
        increaseSentCounter();
        Log.d(TAG, "sendData: DATI INVIATI");
    }

    /**
     * Incrementa il contatore delle corse social inviate
     */
    private void increaseSentCounter() {
        int recordAlreadyExists = socialRunDao.increaseSentSocialCounter();
        if (recordAlreadyExists == 0) {
            Log.d(TAG, "increaseSentCounter: FIRST TIME");
            SocialStats firstSocialStats = new SocialStats();
            firstSocialStats.setReceivedCounter(0);
            firstSocialStats.setSentCounter(1);
            socialRunDao.insertFirstSocialStat(firstSocialStats);
        } else {
            Log.d(TAG, "increaseSentCounter: NOT FIRST TIME");
        }
    }

    /**
     * Incrementa il contatore delle corse social ricevute
     */
    private void increaseReceivedCounter() {
        int recordAlreadyExists = socialRunDao.increaseReceivedSocialCounter();
        if (recordAlreadyExists == 0) {
            Log.d(TAG, "increaseReceivedCounter: FIRST TIME");
            SocialStats firstSocialStats = new SocialStats();
            firstSocialStats.setReceivedCounter(1);
            firstSocialStats.setSentCounter(0);
            socialRunDao.insertFirstSocialStat(firstSocialStats);
        }
    }

    /**
     * @param host ip dell'host
     * @param port porta dell'host
     * @return socket connessa con il server
     * @throws IOException nel caso di errore nella connessione
     */
    private Socket connectToServer(String host, int port) throws IOException {
        Socket socket = new Socket();
        Log.d(TAG, "connectToServer: APERTURA SOCKET CLIENT");
        socket.bind(null);
        socket.setSoTimeout(SOCKET_TIMEOUT);
        // Questo serve per aspettare che il device group owner ritorni all'app
        // altrimenti la connect da ECONNREFUSED perché non c'è alcuna server socket
        try {
            Thread.sleep(USER_TIMEOUT);
        } catch (InterruptedException e) {
            // nvm
        }
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        socket.connect(socketAddress, SOCKET_TIMEOUT);
        Log.d(TAG, "connectToServer: Socket connessa: " + socket.isConnected());
        return socket;
    }

    /**
     * Mostra la notifica della condivisione
     */
    private void showNoty() {
        notification.setContentTitle("Speedo");
        notification.setOngoing(true);
        notification.setContentText("Condivisione in corso");
        notification.setSmallIcon(R.drawable.ic_run_notification);
        notification.setPriority(NotificationCompat.PRIORITY_HIGH);
        notification.setProgress(0, 0, true);
        notificationManager.notify(NOTIFICATION_ID, notification.build());
    }

    /**
     * Aggiorna la notifica della condivisione
     *
     * @param content contenuto della notifica
     */
    private void updateNoty(String content) {
        notification.setContentText(content);
        notification.setOngoing(false);
        notification.setProgress(0, 0, false);
        if (content.equals(getString(R.string.success_share))) {
            notification.setAutoCancel(true);
            P2pTransferService.notyOnClick(notification, getApplicationContext(), 2, NOTIFICATION_STACKBUILDER_ID);
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build());
    }

    /**
     * Aggiunge il pending intent che starta l'activity
     * alla notifica. Il pending intent contiene anche tutto il back stack
     * dell'applicazione qualora ci fosse.
     *
     * @param notification buildere della notifica
     * @param mContext     contex della notifica
     * @param tab          tab da aprire ( 0 : profilo, 1 : run, 2 : social )
     */
    public static void notyOnClick(NotificationCompat.Builder notification, Context mContext, int tab, int requestCode) {
        Log.d(TAG, "notyOnClick: TAB " + tab);
        Intent resultIntent = new Intent(mContext, MainActivity.class);
        resultIntent.setAction(ACTION_NOTY_CLICK)
                .putExtra(KEY_START_FROM_NOTY, tab)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(resultPendingIntent);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy:");
    }

    private String getUsername() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(StatsFragment.KEY_USERNAME, "Anon");
    }
}
