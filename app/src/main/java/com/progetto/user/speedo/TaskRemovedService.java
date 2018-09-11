package com.progetto.user.speedo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.CurrentRunDao;

/**
 * Service che detecta l'eliminazione dell'app
 * dal task manager. Lascia l'app in uno stato
 * consistente eliminando eventuali notifiche.
 */
public class TaskRemovedService extends Service {
    private static final String TAG = "TASK-REMOVED-SERVICE";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: ");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        return START_NOT_STICKY;
    }

    /**
     * Ripulisce lo stato corrente lasciando l'app in uno stato consistente
     *
     * @param rootIntent intent con il quale il service è stato startato
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (sp.getBoolean(RunFragment.KEY_ISRUNNING, true)) {
            removeLocationUpdates();
            LocationUpdatesJobService.clearPreferences(sp);
            removeNotification();
            Log.d(TAG, "onTaskRemoved: ");
        }

    }

    private void removeLocationUpdates() {
        Intent intent = new Intent(getApplicationContext(), LocationBroadcastReceiver.class);
        intent.setAction(LocationBroadcastReceiver.ACTION_UPDATES);
        // Ricostruisco lo stesso pending intent con il quale ho startato la corsa
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), RunFragment.LOCATION_BROADCAST_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (pendingIntent == null) Log.d(TAG, "removeLocationUpdates: PENDING NULLO");
        FusedLocationProviderClient mFusedLocationClient =
                LocationServices.getFusedLocationProviderClient(getApplicationContext());
        mFusedLocationClient.removeLocationUpdates(pendingIntent);
        if (pendingIntent != null) {
            /*  Teoricamente non sarebbe necessario ma spesso consegnava il pending intent
                contenente l'ultima locazione prima di rimuovere gli updates
             */
            pendingIntent.cancel();
        }
        Log.d(TAG, "removeLocationUpdates: REMOVED LOCATION UPDATES");
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(LocationBroadcastReceiver.NOTIFICATION_ID);
            notificationManager.cancel(P2pTransferService.NOTIFICATION_ID);
            Log.d(TAG, "removeNotification: RKMOSSA");
        }
        Log.d(TAG, "removeNotification: ");
    }

}
