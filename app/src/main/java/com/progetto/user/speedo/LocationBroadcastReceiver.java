package com.progetto.user.speedo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.LocationResult;


/**
 * Riceve ed elabora le informazioni riguardanti la nuova locazione
 * dell'utente
 */
public class LocationBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "LOCATION-BROADCAST";

    private static final int DISTANCE_SCALE = 1000;
    public static final String CH_ID = "com.progetto.user.speedo.CH_ID";
    private static final int NOTIFICATION_STACKBUILDER_ID = 5513;
    static final String ACTION_UPDATES =
            "com.progetto.user.speedo.UPDATES";

    static final String KEY_SPEEDO_GROUP = "com.progetto.user.speedo.NOTIFICATION_GROUP_SPEEDO";
    private static SharedPreferences sp;
    private float[] mNewValues;
    private static NotificationCompat.Builder notification;
    private static NotificationManager notificationManager;


    static final int NOTIFICATION_ID = 1236;
    static final String KEY_NEW_LAT = "com.progetto.user.speedo.NEW_LAT";
    static final String KEY_NEW_LNG = "com.progetto.user.speedo.NEW_LNG";
    static final String KEY_NEW_DISTANCE = "com.progetto.user.speedo.NEW_DISTANCE";
    static final String KEY_INIT_TIME = "com.progetto.user.speedo.INIT_TIME";
    static final String KEY_NEW_SPEED = "com.progetto.user.speedo.NEW_SPEED";
    static final String KEY_NEW_TIME = "com.progetto.user.speedo.NEW_TIME";
    static final String KEY_NEW_VALUES = "com.progetto.user.speedo.NEW_VALUES";

    static final String KEY_SPEED_SUM = "com.progetto.user.speedo.SPEED_SUM";
    static final String KEY_SPEED_COUNT = "com.progetto.user.speedo.SPEED_COUNT";
    static final String KEY_SPEED_AVG = "com.progetto.user.speedo.SPEED_AVG";

    private long initTime;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            checkNullable(context);
            if (ACTION_UPDATES.equals(action)) {
                LocationResult res = LocationResult.extractResult(intent);
                if (res != null) {
                    Location loc = res.getLastLocation();
                    if (save(context, loc)) {
                        updateNoty(context);
                    } else {
                        showNoty(context);
                    }
                    Log.d(TAG,
                            "onReceive: LAT " +
                                    loc.getLatitude() +
                                    " , LONG " + loc.getLongitude() +
                                    " , SPEED " + loc.getSpeed() +
                                    " , ACCURACY " + loc.getAccuracy() +
                                    " CONTEXT " + context);
                }
            } else if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_REBOOT.equals(action)) {
                sp.edit().putBoolean(DeleteScheduledService.KEY_ALARM_SET, false).apply();
                Log.d(TAG, "onReceive: ACTION BOOT COMPLETED");
            }
        }
    }

    /**
     * Visto che viene ricreato un nuovo BR ogni volta
     * usando delle variabili statiche e non legate all'istanza
     * ho la speranza di non allocare ad ogni broadcast gli stessi oggetti
     *
     * @param context contesto del BR
     */
    private void checkNullable(Context context) {
        if (mNewValues == null)
            mNewValues = new float[2];
        if (sp == null)
            sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (notification == null)
            notification = new NotificationCompat.Builder(context, CH_ID);
        if (notificationManager == null)
            notificationManager = (NotificationManager) context.
                    getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Aggiorna le informazioni riguardanti la corsa corrente
     * e starta il JI-service per effettuare le scritture sul db
     *
     * @param mContext  contesto dell'app
     * @param mLocation ultima location
     * @return true se NON è la prima volta, false altrimenti
     */
    private boolean save(Context mContext, Location mLocation) {
        boolean someDistance = false;
        double oldLat = Double.longBitsToDouble(sp.getLong(KEY_NEW_LAT, 0));
        double oldLng = Double.longBitsToDouble(sp.getLong(KEY_NEW_LNG, 0));
        float oldSpeedSum = sp.getFloat(KEY_SPEED_SUM, 0);
        int oldSpeedCount = sp.getInt(KEY_SPEED_COUNT, 0);
        double newLat = mLocation.getLatitude();
        double newLng = mLocation.getLongitude();
        float newSpeed = mLocation.getSpeed();
        oldSpeedSum += newSpeed;
        oldSpeedCount++;
        float currentAvgSpeed = oldSpeedSum / oldSpeedCount;
        long newTime = System.currentTimeMillis();
        float currentDistance = 0;
        if (oldLat != 0 && oldLng != 0) {
            float[] results = new float[1];
            Location.distanceBetween(oldLat, oldLng, newLat, newLng, results);
            currentDistance = sp.getFloat(KEY_NEW_DISTANCE, 0) + (results[0] / DISTANCE_SCALE);
        }
        mNewValues[0] = currentDistance;
        mNewValues[1] = currentAvgSpeed;
        sp.edit().putLong(KEY_NEW_LAT, Double.doubleToLongBits(newLat))
                .putLong(KEY_NEW_LNG, Double.doubleToLongBits(newLng))
                .putFloat(KEY_NEW_DISTANCE, currentDistance)
                .putFloat(KEY_SPEED_AVG, currentAvgSpeed)
                .putFloat(KEY_SPEED_SUM, oldSpeedSum)
                .putInt(KEY_SPEED_COUNT, oldSpeedCount)
                .apply();
        Intent jobIntent = new Intent();
        if (currentDistance != 0) {
            jobIntent.setAction(LocationUpdatesJobService.ACTION);
            someDistance = true;
        } else {
            // Se è la prima volta creo la nuova corsa e inizializzo il tempo
            initTime = newTime;
            jobIntent.setAction(LocationUpdatesJobService.ACTION_FIRST);
        }
        Bundle tmpBundle = new Bundle();
        tmpBundle.putDouble(KEY_NEW_LAT, newLat);
        tmpBundle.putDouble(KEY_NEW_LNG, newLng);
        tmpBundle.putFloat(KEY_NEW_SPEED, newSpeed);
        tmpBundle.putLong(KEY_NEW_TIME, newTime);
        tmpBundle.putFloat(KEY_SPEED_AVG, currentAvgSpeed);
        tmpBundle.putFloat(KEY_NEW_DISTANCE, currentDistance);
        jobIntent.putExtra(KEY_NEW_VALUES, tmpBundle);
        LocationUpdatesJobService.enqueueWork(mContext,
                LocationUpdatesJobService.class,
                LocationUpdatesJobService.JOB_ID, jobIntent);
        Log.d(TAG, "save: LAT: " + newLat + " LONG: " + newLng +
                " VALUES: " + mNewValues[0] + " , " + mNewValues[1]);
        return someDistance;
    }


    /**
     * Mostra la notifica della corsa
     */
    private void showNoty(Context mContext) {
        Resources res = mContext.getResources();
        String smallContent = res.getString(R.string.run_notification, mNewValues[0]);
        String bigContent = res.getString(R.string.run_notification_bt, mNewValues[0], mNewValues[1]);
        P2pTransferService.notyOnClick(notification, mContext,
                1, NOTIFICATION_STACKBUILDER_ID);

        notification.setContentTitle(res.getString(R.string.app_name));
        notification.setContentText(smallContent);
        notification.setSmallIcon(R.drawable.ic_run_notification);
        notification.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(bigContent));
        notification.setOngoing(true);
        notification.setUsesChronometer(true);
        notification.setWhen(initTime);
        notification.setPriority(NotificationCompat.PRIORITY_HIGH);
        notificationManager.notify(NOTIFICATION_ID, notification.build());
        Log.d(TAG, "showNoty: ");
    }

    /**
     * Aggiorna la notifica della corsa
     */
    private void updateNoty(Context mContext) {
        Resources res = mContext.getResources();
        String smallContent = res.getString(R.string.run_notification, mNewValues[0]);
        String bigContent = res.getString(R.string.run_notification_bt, mNewValues[0], mNewValues[1]);
        notification.setContentText(smallContent);
        notification.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(bigContent));
        notificationManager.notify(NOTIFICATION_ID, notification.build());
        Log.d(TAG, "updateNoty: ");
    }

}
