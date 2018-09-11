package com.progetto.user.speedo.RoomPackage;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Dao per la condivisione di informazioni via wifi direct
 * e gestione del tab social
 */
@Dao
public interface SocialRunDao {

    @Query("SELECT distance,runningTime,speedAvg,date FROM RunBasicInfo WHERE run_id = :key AND ready = 1")
    P2pRunBasicInfo getP2pRunBasicInfo(long key);

    @Query("SELECT latitude,longitude,speed,speed_avg,time,current_distance FROM CurrentRun WHERE extRun_id = :key")
    List<P2pCurrentRun> getP2pCurrentRun(long key);

    @Insert
    long insertNewSocialRun(SocialRunBasicInfo runBasicInfo);

    @Insert
    void insertSocialRunInfo(List<SocialCurrentRun> currentRun);

    @Query("SELECT * FROM SocialRunBasicInfo ORDER BY run_id DESC")
    LiveData<List<SocialRunBasicInfo>> getSocialRuns();

    @Query("DELETE FROM SocialRunBasicInfo WHERE run_id = :key")
    void removeRun(long key);

    @Query("SELECT * FROM SocialCurrentRun WHERE extRun_id = :runId")
    List<SocialCurrentRun> getSelectedRunValues(Long runId);

    @Query("UPDATE SocialStats SET sentCounter = sentCounter + 1")
    int increaseSentSocialCounter();

    @Query("UPDATE SocialStats SET receivedCounter = receivedCounter + 1")
    int increaseReceivedSocialCounter();

    @Insert
    void insertFirstSocialStat(SocialStats stats);

    @Query("SELECT * FROM SocialStats ORDER BY stat_id DESC LIMIT 1")
    LiveData<SocialStats> getSocialStatistics();

    @Query("SELECT receiveDate FROM SocialRunBasicInfo ORDER BY run_id ASC LIMIT 1")
    long getFirstRecordToDeleteTime();

    @Query("DELETE FROM SocialRunBasicInfo WHERE receiveDate <= ( :currentTime - 24*60*60*1000)")
    int deleteExpiredRun(long currentTime);

}
