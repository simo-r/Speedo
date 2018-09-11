package com.progetto.user.speedo;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.SocialRunDao;

/**
 * Uso un IntentService che è soggetto alle restrizioni
 * (API 26+) perché non è essenziale tenere il database
 * aggiornato anche se l'app non è in foreground.
 * Non ho neanche bisogno di ri-settare l'allarme quando
 * il telefono viene riavviato, l'importante è che venga settato
 * quando l'app riparte.
 * Elimina le corse social ricevute più vecchie di DELETE_OFFSET.
 */

public class DeleteScheduledService extends IntentService {
    private static final String TAG = "DELETE-SERVICE";

    private static final int DELETE_OFFSET = 24*60*60*1000;

    public static final String ACTION_INIT = "com.progetto.user.speedo.INIT";
    private static final String ACTION_ALARM = "com.progetto.user.speedo.ALARM";
    static final String KEY_ALARM_SET = "com.progetto.user.speedo.ALARM_SET";
    private SocialRunDao socialRunDao;

    public DeleteScheduledService(String name) {
        super(name);
    }

    public DeleteScheduledService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        socialRunDao = db.socialRunDao();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case ACTION_INIT:
                setNextAlarm();
                Log.d(TAG, "onHandleIntent: INIT");
                break;
            case ACTION_ALARM:
                int deleted = socialRunDao.deleteExpiredRun(System.currentTimeMillis());
                setNextAlarm();
                Log.d(TAG, "onHandleIntent: ALARM NUM OF DELETED RECORDs: " + deleted);
                break;
            default:
                Log.d(TAG, "onHandleIntent: DEFAULT");
                break;
        }
    }

    /**
     * Imposta il nuovo allarme al prossimo record da eliminare oppure a
     * DELETE_OFFSET nel caso in cui non ci sia alcun record.
     */
    private void setNextAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent alarmIntent = new Intent(this, DeleteScheduledService.class);
            long nextAlarm = socialRunDao.getFirstRecordToDeleteTime();
            Log.d(TAG, "setNextAlarm: STARTING TIME " + StatsRecyclerViewAdapter.getFormattedDate(nextAlarm));
            PendingIntent pendingIntent;
            if (nextAlarm != 0) {
                Log.d(TAG, "setNextAlarm: DELETE");
                alarmIntent.setAction(ACTION_ALARM);
                pendingIntent = PendingIntent.getService(this, 0, alarmIntent, 0);
                nextAlarm = nextAlarm + DELETE_OFFSET;
                alarmManager.set(AlarmManager.RTC, nextAlarm, pendingIntent);
                Log.d(TAG, "setNextAlarm: NEXT DELETE ALARM: " + StatsRecyclerViewAdapter.getFormattedDate(nextAlarm));
            } else {
                alarmIntent.setAction(ACTION_INIT);
                pendingIntent = PendingIntent.getService(this, 0, alarmIntent, 0);
                nextAlarm = System.currentTimeMillis() + DELETE_OFFSET;
                alarmManager.set(AlarmManager.RTC, nextAlarm, pendingIntent);
                Log.d(TAG, "setNextAlarm: INIT: " + StatsRecyclerViewAdapter.getFormattedDate(nextAlarm));
            }
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                    .putBoolean(KEY_ALARM_SET, true).apply();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

}
