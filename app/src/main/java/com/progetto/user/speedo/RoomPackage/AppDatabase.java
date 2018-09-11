package com.progetto.user.speedo.RoomPackage;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.util.Log;

@Database(entities = {CurrentRun.class, RunBasicInfo.class, SocialRunBasicInfo.class, SocialCurrentRun.class, SocialStats.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "APP-DATABASE";
    private static AppDatabase INSTANCE;

    /* Dao per le operazioni locali */
    public abstract CurrentRunDao currentRunDao();

    /* Dao per le operazioni via wifi direct */
    public abstract SocialRunDao socialRunDao();

    /* Singleton istance con context del primo chiamante */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Log.d(TAG, "getDatabase:");
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "SpeedoDB")
                            .build();

                }
            }
        }
        return INSTANCE;
    }
}
