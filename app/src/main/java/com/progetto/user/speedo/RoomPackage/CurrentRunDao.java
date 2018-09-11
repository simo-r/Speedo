package com.progetto.user.speedo.RoomPackage;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Dao per le operazioni locali
 */
@Dao
public interface CurrentRunDao {

    @Insert
    void insertUpdates(CurrentRun currentRun);

    @Query("UPDATE RunBasicInfo SET distance = :dist, " +
            "runningTime = :runTime, speedAvg = :speedAvg, ready = 1 WHERE run_id = :k")
    void updateRunBasicInfo(long k, float dist, long runTime, float speedAvg);

    @Query("SELECT * FROM RunBasicInfo WHERE run_id = :runId")
    RunBasicInfo getRunBasicInfo(Long runId);

    @Query("SELECT * FROM CurrentRun WHERE extRun_id = :runId ORDER BY id ASC")
    LiveData<List<CurrentRun>> getCurrentRunValues(long runId);

    @Query("SELECT * FROM CurrentRun WHERE extRun_id = :runId")
    List<CurrentRun> getSelectedRunValues(long runId);

    @Insert
    long insertNewRun(RunBasicInfo runBasicInfo);

    @Query("SELECT run_id FROM RunBasicInfo ORDER BY run_id DESC LIMIT 1 ")
    long getLastRunId();

    @Query("SELECT run_id,ready FROM RunBasicInfo ORDER BY run_id DESC LIMIT 1 ")
    ReadyRunBasicInfo getResumeRun();

    @Query("SELECT time FROM CurrentRun WHERE extRun_id = :extkey_id ORDER BY id ASC LIMIT 1")
    long getInitRunTime(long extkey_id);

    @Query("SELECT * FROM RunBasicInfo WHERE ready = 1 ORDER BY run_id DESC")
    LiveData<List<RunBasicInfo>> getReadyRuns();

    @Query("DELETE FROM RunBasicInfo WHERE run_id = :key")
    void removeRun(Long key);


    @Query("SELECT COUNT(run_id) AS runCount," +
            " SUM(distance) as distance, AVG(speedAvg)" +
            " as speedAvg, SUM(runningTime) as runningTime" +
            " FROM RunBasicInfo WHERE ready = 1")
    LiveData<Stats> getStatistics();

    @Query("DELETE FROM RunBasicInfo WHERE run_id = (SELECT run_id FROM RunBasicInfo WHERE ready = 0 ORDER BY run_id DESC LIMIT 1)")
    int deletePendingRun();
}

