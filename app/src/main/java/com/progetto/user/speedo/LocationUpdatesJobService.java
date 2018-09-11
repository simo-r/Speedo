package com.progetto.user.speedo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.CurrentRun;
import com.progetto.user.speedo.RoomPackage.CurrentRunDao;
import com.progetto.user.speedo.RoomPackage.RunBasicInfo;

/**
 * Service per l'aggiornamento delle informazioni
 * nel db riguardanti la corsa corrente
 */
public class LocationUpdatesJobService extends JobIntentService {
    private static final String TAG = "LOCATION-JOB-SERVICE";

    static final int JOB_ID = 1239;
    static final String ACTION = "com.progetto.user.speedo.UPDATES_DATABASE";
    static final String ACTION_FIRST =
            "com.progetto.user.speedo.FIRST";
    static final String ACTION_END = "com.progetto.user.speedo.RUN_END";

    private static final String KEY_ROW_ID = "com.progetto.user.speedo.ROW_ID";


    private static long extkey_id = -1;
    private static CurrentRunDao currentRunDao;
    private static CurrentRun tmpCR;
    private static SharedPreferences sp;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        currentRunDao = db.currentRunDao();
        if (tmpCR == null) {
            tmpCR = new CurrentRun();
        }
        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            Bundle tmpBundle = intent.getBundleExtra(LocationBroadcastReceiver.KEY_NEW_VALUES);
            switch (action) {
                case ACTION_END:
                    Log.d(TAG, "onHandleWork: CONTEXT " + getApplicationContext().toString());
                    clearPreferences(sp);
                    float distance = tmpBundle.getFloat(LocationBroadcastReceiver.KEY_NEW_DISTANCE);
                    float speedAvg = tmpBundle.getFloat(LocationBroadcastReceiver.KEY_SPEED_AVG);
                    long time = tmpBundle.getLong(LocationBroadcastReceiver.KEY_NEW_TIME) -
                            currentRunDao.getInitRunTime(extkey_id);
                    currentRunDao.updateRunBasicInfo(extkey_id, distance, time, speedAvg);
                    break;
                default:
                    double tmpLat = tmpBundle.getDouble(LocationBroadcastReceiver.KEY_NEW_LAT);
                    double tmpLng = tmpBundle.getDouble(LocationBroadcastReceiver.KEY_NEW_LNG);
                    float tmpSpeed = tmpBundle.getFloat(LocationBroadcastReceiver.KEY_NEW_SPEED);
                    long tmpTime = tmpBundle.getLong(LocationBroadcastReceiver.KEY_NEW_TIME);
                    float tmpDistance = tmpBundle.getFloat(LocationBroadcastReceiver.KEY_NEW_DISTANCE);
                    float tmpSpeedAvg = tmpBundle.getFloat(LocationBroadcastReceiver.KEY_SPEED_AVG);
                    if (action.equals(ACTION_FIRST)) {
                        Log.d(TAG, "onHandleWork: FIRST TIME " + extkey_id);
                        RunBasicInfo newRunBasicInfo = new RunBasicInfo();
                        newRunBasicInfo.setReady(false);
                        newRunBasicInfo.setDate(tmpTime);
                        extkey_id = currentRunDao.insertNewRun(newRunBasicInfo);
                        sp.edit().putLong(KEY_ROW_ID, extkey_id).apply();
                    }
                    if (extkey_id == -1) {
                        extkey_id = sp.getLong(KEY_ROW_ID, -1);
                    }
                    tmpCR.setLatitude(tmpLat);
                    tmpCR.setLongitude(tmpLng);
                    tmpCR.setSpeed(tmpSpeed);
                    tmpCR.setTime(tmpTime);
                    tmpCR.setCurrent_distance(tmpDistance);
                    tmpCR.setExtRun_id(extkey_id);
                    tmpCR.setSpeed_avg(tmpSpeedAvg);
                    currentRunDao.insertUpdates(tmpCR);
                    Log.d(TAG, "onHandleWork: NUOVI VALORI: " + tmpCR.toString());
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    public static void clearPreferences(SharedPreferences sp) {
        sp.edit()
                .putBoolean(RunFragment.KEY_ISRUNNING, false)
                .remove(LocationBroadcastReceiver.KEY_NEW_DISTANCE)
                .remove(LocationBroadcastReceiver.KEY_NEW_VALUES)
                .remove(LocationBroadcastReceiver.KEY_NEW_LAT)
                .remove(LocationBroadcastReceiver.KEY_NEW_LNG)
                .remove(LocationBroadcastReceiver.KEY_INIT_TIME)
                .remove(LocationBroadcastReceiver.KEY_SPEED_COUNT)
                .remove(LocationBroadcastReceiver.KEY_SPEED_SUM)
                .apply();
    }

}
