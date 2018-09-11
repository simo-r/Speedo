package com.progetto.user.speedo;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.Collection;


/**
 * Gestisce lo switch tra i vari fragment e le operazioni via wifi direct
 */
public class MainActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener,
        WifiP2pManager.ConnectionInfoListener, WifiP2pManager.ChannelListener
        , SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "MAIN-ACTIVITY";

    public static final int ACCESS_FINE_LOCATION_PERMISSION_ID = 1235;
    public static final int REQUEST_CHECK_SETTINGS_ID = 1234;

    private static final String PEERS_DIALOG_NAME = "PEERS_DIALOG";
    private static final String KEY_PEERS_DIALOG_SHOWN = "com.progetto.user.speedo.PEERS_DIALOG_SHOWN";

    private static final int WIFI_P2P_PORT = 7564;
    private long wifiDataKey;

    private BottomNavigationView navigation;
    private ViewPager mPager;
    private CustomStatePagerAdapter mAdapter;

    private SharedPreferences sp;

    private boolean isWifiP2pEnabled = false;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private IntentFilter intentFilter;
    private WifiDirectBroadcastReceiver receiver;
    private boolean isSending = false;

    private CustomPeerDialog customPeerDialog;
    private ActionMode actionMode;

    /**
     * Listener per la navigation view, serve a caricare correttamente i vari fragment
     */
    private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Log.d(TAG, "onNavigationItemSelected: " + mPager.getCurrentItem());
            switch (item.getItemId()) {
                case R.id.navigation_profile:
                    mPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_run:
                    mPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_social:
                    mPager.setCurrentItem(2);
                    return true;
            }

            return false;
        }
    };

    /**
     * Listener per lo swipe tra i vari fragment nel view pager
     */
    private final ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            Log.d(TAG, "onPageSelected: PRIMA ");
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) return;
            View v = getCurrentFocus();
            if (v != null) {
                v.getWindowToken();
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
            if (actionMode != null) actionMode.finish();
            Log.d(TAG, "onPageSelected: " + position +
                    " CURR ITEM " + mPager.getCurrentItem() +
                    " CHECKED " + navigation.getMenu().getItem(position).isChecked());
            navigation.getMenu().getItem(position).setChecked(true);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    /**
     * Listener per lo scroll automatico in cima alla recycler view
     * di ogni fragment
     */
    private final BottomNavigationView.OnNavigationItemReselectedListener
            mOnNavigationItemReselectedListener = new BottomNavigationView.
            OnNavigationItemReselectedListener() {
        @Override
        public void onNavigationItemReselected(@NonNull MenuItem item) {
            Log.d(TAG, "onNavigationItemReselected: ");
            Fragment f;
            switch (item.getItemId()) {
                case R.id.navigation_profile:
                    f = mAdapter.getFragment(mPager.getCurrentItem());
                    if (f != null && (f instanceof StatsFragment)) {
                        Log.d(TAG, "onNavigationItemReselected: DENTRO");
                        ((StatsFragment) f).scrollToTop();
                    }
                    break;
                case R.id.navigation_social:
                    f = mAdapter.getFragment(mPager.getCurrentItem());
                    if (f != null && (f instanceof SocialFragment)) {
                        Log.d(TAG, "onNavigationItemReselected: DENTRO");
                        ((SocialFragment) f).scrollToTop();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            boolean wasDialogShowing = savedInstanceState.getBoolean(KEY_PEERS_DIALOG_SHOWN, false);
            if (wasDialogShowing) {
                // Recupera il PEERS_DIALOG se questo era mostrato prima della rotazione
                Fragment f = getSupportFragmentManager().findFragmentByTag(PEERS_DIALOG_NAME);
                if (f instanceof CustomPeerDialog) {
                    customPeerDialog = (CustomPeerDialog) f;
                    Log.d(TAG, "onCreate: L'HO RITROVATO");
                }
            }

        }
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            Log.d(TAG, "onCreate: ACTION BAR NOT NULL");
            actionBar.setDisplayShowHomeEnabled(true);
        }
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        scheduleAlarmManager();
        navigation = findViewById(R.id.navigation);
        String[] tabNames = new String[3];
        tabNames[0] = getString(R.string.tab_profile);
        tabNames[1] = getString(R.string.tab_run);
        tabNames[2] = getString(R.string.tab_social);
        mAdapter = new CustomStatePagerAdapter(getSupportFragmentManager(), tabNames);
        mPager = findViewById(R.id.viewPager);
        mPager.setAdapter(mAdapter);
        mPager.addOnPageChangeListener(mOnPageChangeListener);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setOnNavigationItemReselectedListener(mOnNavigationItemReselectedListener);
        initWifiDirectBroadcast();
        Log.d(TAG, "onCreate: ");
    }

    /**
     * Inizializza le componenenti che servono per il broadcast receiver
     * del wifi direct
     */
    private void initWifiDirectBroadcast() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
    }

    /**
     * Starta il service per schedulare il prossimo allarme
     * che servirà ad eliminare le corse ricevute via wifi direct
     */
    private void scheduleAlarmManager() {
        if (!sp.getBoolean(DeleteScheduledService.KEY_ALARM_SET, false)) {
            Intent scheduleIntent = new Intent(this, DeleteScheduledService.class);
            scheduleIntent.setAction(DeleteScheduledService.ACTION_INIT);
            startService(scheduleIntent);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WifiDirectBroadcastReceiver(manager, channel, this);
        // Di default è true cioè vuol dire che non è stato mai settato nel caso del receiver
        checkStartFromNotification();
        sp.registerOnSharedPreferenceChangeListener(this);
        updateWifiDirectTransferState();
        registerReceiver(receiver, intentFilter);
        Log.d(TAG, "onResume: TAB ");
    }

    /**
     * Se prima che l'app ricevesse onReceive il trasferimento
     * in wifi direct è terminato setta lo status a pronto
     * e rimuove il gruppo che si era formato
     */
    private void updateWifiDirectTransferState() {
        int wifiDirectTransferState = sp.getInt(P2pTransferService.KEY_IS_TRANSFERRING, -1);
        Log.d(TAG, "onSharedPreferenceChanged: DENTRO 1 STATE " + wifiDirectTransferState);
        if (wifiDirectTransferState == 1) {
            // Ho finito e si può ricominciare
            sp.edit().putInt(P2pTransferService.KEY_IS_TRANSFERRING, 2).apply();
            if (manager != null && channel != null) {
                Log.d(TAG, "onSharedPreferenceChanged: DENTRO 2");
                manager.removeGroup(channel, null);
            }
        }
    }

    /**
     * Controlla se l'activity è stata startata
     * da un intent ricevuto da una notifica
     */
    private void checkStartFromNotification() {
        Log.d(TAG, "checkStartFromNotification:");
        Intent startIntent = getIntent();
        if (startIntent == null) return;
        String action = startIntent.getAction();
        if (action == null || !action.equals(P2pTransferService.ACTION_NOTY_CLICK)) return;
        // Work-around per evitare il re-delivery
        startIntent.setAction("");
        Bundle extra = startIntent.getExtras();
        if (extra == null)
            Log.d(TAG, "onCreate: POLLO BUNDLE NULL");
        else {
            int tab = extra.getInt(P2pTransferService.KEY_START_FROM_NOTY, -1);
            if (tab == -1) {
                Log.d(TAG, "onCreate: POLISSIMO");
            } else if (tab == 1 && navigation.getSelectedItemId() != R.id.navigation_run) {
                Log.d(TAG, "checkStartFromNotification: run");
                navigation.setSelectedItemId(R.id.navigation_run);
            } else if (tab == 2 && navigation.getSelectedItemId() != R.id.navigation_social) {
                Log.d(TAG, "checkStartFromNotification: social ");
                navigation.setSelectedItemId(R.id.navigation_social);
            }
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        unregisterReceiver(receiver);
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }


    public void setIsWifiP2pEnabled(boolean isEnabled) {
        isWifiP2pEnabled = isEnabled;
    }


    /**
     * Rende visibile e inizia il processo di discovery
     * dei peers in wifi direct
     *
     * @param key chiave della corsa da condividere
     */
    public void enableWifiDirectShare(long key) {
        if (!isWifiP2pEnabled) {
            Toast.makeText(this, R.string.wifi_direct_disabled,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        wifiDataKey = key;
        WifiP2pManager.ActionListener discoverActionListener = new CustomDiscoverActionListener();

        manager.discoverPeers(channel, discoverActionListener);
    }


    /**
     * Richiede la connessione con il peer selezionato
     * ed identificato da config.deviceAddress
     *
     * @param config configurazione corrente della connessione con il peer
     */
    public void connectToDevice(WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isSending = true;
                Log.d(TAG, "onSuccess: CONNESSO CON IL PEER");
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this,
                        getFailureReason(getApplicationContext(), reason),
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "onFailure: FAILED CONNESSIONE CON IL PEER " + getFailureReason(getApplicationContext(), reason));
            }
        });
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        // Chiamo la lista e notifico l'adapter sse il dialog è mostrato
        if (!isDialogShowing()) {
            Log.d(TAG, "onPeersAvailable: INVISIBLE");
            return;
        }
        Log.d(TAG, "onPeersAvailable: VISIBLE");
        Collection<WifiP2pDevice> refreshedPeers = peerList.getDeviceList();
        customPeerDialog.refreshPeers(refreshedPeers);

    }

    /**
     * Gestisce chi è il GO della connessione p2p e chi
     * deve inviare/ricevere la corsa
     *
     * @param wifiP2pInfo info sulla connessione con il peer
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        int isTransferring = sp.getInt(P2pTransferService.KEY_IS_TRANSFERRING, -1);
        Log.d(TAG, "onConnectionInfoAvailable: STATE " + isTransferring);

        // Se isTransferring == 2 vuol dire che ho finito il trasf. prec., -1 se sono receiver oppure mai fatto.
        if (wifiP2pInfo.groupFormed && (isTransferring == 2 || isTransferring == -1)) {
            Intent intentService = new Intent(this, P2pTransferService.class);
            if (wifiP2pInfo.isGroupOwner) {
                intentService.putExtra(P2pTransferService.EXTRAS_GROUP_OWNER_PORT, WIFI_P2P_PORT);
                if (isSending) {
                    Log.d(TAG, "onConnectionInfoAvailable: GROUP FORMED AND OWNER SENDER");
                    intentService.setAction(P2pTransferService.ACTION_GO_SEND)
                            .putExtra(P2pTransferService.EXTRAS_RUN_KEY, wifiDataKey);
                } else {
                    Log.d(TAG, "onConnectionInfoAvailable: GROUP FORMED AND OWNER RECEIVER");
                    intentService.setAction(P2pTransferService.ACTION_GO_RECEIVE);
                }

            } else {
                intentService.putExtra(P2pTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        wifiP2pInfo.groupOwnerAddress.getHostAddress())
                        .putExtra(P2pTransferService.EXTRAS_GROUP_OWNER_PORT, WIFI_P2P_PORT);
                if (isSending) {
                    Log.d(TAG, "onConnectionInfoAvailable: GROUP FORMED SENDER");
                    intentService.setAction(P2pTransferService.ACTION_SEND)
                            .putExtra(P2pTransferService.EXTRAS_RUN_KEY, wifiDataKey);

                } else {
                    Log.d(TAG, "onConnectionInfoAvailable: GROUP FORMED RECEIVER");
                    intentService.setAction(P2pTransferService.ACTION_RECEIVE);
                }

            }
            sp.edit().putInt(P2pTransferService.KEY_IS_TRANSFERRING, 0).apply();
            isSending = false;
            P2pTransferService.enqueueWork(getApplicationContext(), P2pTransferService.class, P2pTransferService.JOB_ID, intentService);
        }
    }

    /**
     * Mostra una snackbar che permette di rimuovere la richiesta di
     * connessione in wifi direct con l'utente
     *
     * @param device device con cui è in pairing
     */
    public void showDisconnectSnackbar(final WifiP2pDevice device) {
        CoordinatorLayout coordinatorLayout = findViewById(R.id.coord_layout);
        if (coordinatorLayout != null) {
            String deviceName;
            if (device != null) deviceName = device.deviceName;
            else deviceName = "Anon";
            Snackbar test = Snackbar.make(coordinatorLayout, getString(R.string.connecting, deviceName), Snackbar.LENGTH_LONG);
            test.setAction(R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (device == null
                            || device.status == WifiP2pDevice.CONNECTED) {
                        disconnect();
                    } else if (device.status == WifiP2pDevice.AVAILABLE
                            || device.status == WifiP2pDevice.INVITED) {
                        cancelConnectRequest();
                    }
                }
            });
            test.show();
        } else {
            Log.d(TAG, "onConnectionInfoAvailable: COORD NULL");
        }
    }

    /**
     * Aggiorna (nel caso in cui sia stato già startato il service) o aggiunge
     * una notifica nel caso in cui avvenga una disconnessione con il peer
     */
    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getApplication()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notification = new NotificationCompat.
                Builder(getApplicationContext(), LocationBroadcastReceiver.CH_ID);
        notification.setContentTitle(getString(R.string.app_name));
        notification.setOngoing(false);
        notification.setContentText(getString(R.string.abort_share));
        notification.setSmallIcon(R.drawable.ic_run_notification);
        notification.setPriority(NotificationCompat.PRIORITY_HIGH);
        notification.setProgress(0, 0, false);
        if (notificationManager != null) {
            notificationManager.notify(P2pTransferService.NOTIFICATION_ID, notification.build());
        }
    }

    /**
     * Rimuove il gruppo formatosi in wifi direct
     */
    private void disconnect() {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Update notifica subito
                updateNotification();
                stopService(new Intent(getApplicationContext(), P2pTransferService.class));
                sp.edit().putInt(P2pTransferService.KEY_IS_TRANSFERRING, 2).apply();
                Toast.makeText(MainActivity.this,
                        R.string.abort_share, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onSuccess: GRUPPO RIMOSSSO");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "onSuccess: GRUPPO NON RIMOSSSO");
                Toast.makeText(MainActivity.this,
                        R.string.abort_share_fail,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Cancella la richiesta di connessione in wifi direct
     */
    private void cancelConnectRequest() {
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                updateNotification();
                stopService(new Intent(getApplicationContext(), P2pTransferService.class));
                sp.edit().putInt(P2pTransferService.KEY_IS_TRANSFERRING, 2).apply();
                Toast.makeText(MainActivity.this, R.string.abort_share,
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onSuccess: CANCEL CONNECT");
            }

            @Override
            public void onFailure(int reasonCode) {
                // Provo a vedere se è stato creato il gruppo nel frattempo
                disconnect();
                Log.d(TAG, "onFailure: ABORT CANCEL CONNECT");
            }
        });

    }

    private static String getFailureReason(Context context, int reasonCode) {
        String reason;
        switch (reasonCode) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                reason = context.getString(R.string.direct_not_supported);
                break;
            case WifiP2pManager.ERROR:
                reason = context.getString(R.string.direct_internal_error);
                break;
            case WifiP2pManager.BUSY:
                reason = context.getString(R.string.direct_busy);
                break;
            default:
                reason = context.getString(R.string.direct_unknown_error);
                break;
        }
        return reason;

    }

    @Override
    public void onChannelDisconnected() {
        Toast.makeText(this,
                R.string.channel_disconnect,
                Toast.LENGTH_LONG).show();
    }

    /**
     * Setta lo stato del trasferimento a 2 (terminato, si può ricominciare)
     * nel momento in cui il service del trasferimento in p2p è terminato
     *
     * @param sharedPreferences shared preferences
     * @param s                 nome della preferenza cambiata
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.d(TAG, "onSharedPreferenceChanged: ");
        if (s.equals(P2pTransferService.KEY_IS_TRANSFERRING)) {
            int wifiDirectTransferState = sharedPreferences.getInt(P2pTransferService.KEY_IS_TRANSFERRING, -1);
            Log.d(TAG, "onSharedPreferenceChanged: DENTRO 1 STATE " + wifiDirectTransferState);
            if (wifiDirectTransferState == 1) {
                // Ho finito e si può ricominciare
                sharedPreferences.edit().putInt(P2pTransferService.KEY_IS_TRANSFERRING, 2).apply();
                if (manager != null && channel != null) {
                    Log.d(TAG, "onSharedPreferenceChanged: DENTRO 2");
                    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        // TODO [DEBUG]
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "onSuccess: RIMOSSO SHARED CHANGED");
                        }

                        @Override
                        public void onFailure(int i) {
                            Log.d(TAG, "onSuccess: NOT RIMOSSO SHARED CHANGED");
                        }
                    });
                }
            }
        }
    }

    /**
     * @return true se il CustomPeerDialog è in foreground, false altrimenti
     */
    private boolean isDialogShowing() {
        if (customPeerDialog == null) {
            Log.d(TAG, "isDialogShowing: CUSTOM DIALOG NULLO");
            return false;
        }
        Dialog d = customPeerDialog.getDialog();
        if (d == null) Log.d(TAG, "isDialogShowing: DIALOG NULLO ");
        if (d != null && !d.isShowing()) Log.d(TAG, "isDialogShowing: NON SI MOSTRA");
        if (d != null && d.isShowing()) Log.d(TAG, "isDialogShowing: APPOSTO");
        return d != null && d.isShowing();
    }


    public void showActionMode(ActionMode.Callback callback) {
        actionMode = startActionMode(callback);
    }

    public void closeActionMode() {
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
    }

    private class CustomDiscoverActionListener implements WifiP2pManager.ActionListener {

        @Override
        public void onSuccess() {
            customPeerDialog = CustomPeerDialog.newInstance();
            customPeerDialog.show(getSupportFragmentManager(), PEERS_DIALOG_NAME);
            Log.d(TAG, "onSuccess: DISCOVER PEERS");
        }

        @Override
        public void onFailure(int reasonCode) {
            String reason = getFailureReason(getApplicationContext(), reasonCode);
            Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show();
            Log.d(TAG, "onFailure: DISCOVER PEERS" + reason);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: " + isDialogShowing());
        outState.putBoolean(KEY_PEERS_DIALOG_SHOWN, isDialogShowing());
    }
}













