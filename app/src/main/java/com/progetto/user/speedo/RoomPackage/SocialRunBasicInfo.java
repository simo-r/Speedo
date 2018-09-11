package com.progetto.user.speedo.RoomPackage;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Tabella contenente le informazioni di base di una corsa ricevute via wifi direct
 */
@Entity
public class SocialRunBasicInfo {

    @PrimaryKey(autoGenerate = true)
    Long run_id;

    Float distance;

    Long runningTime;

    Float speedAvg;

    Long date;

    private long receiveDate;

    private String senderName;

    public Long getRun_id() {
        return run_id;
    }

    public void setRun_id(Long run_id) {
        this.run_id = run_id;
    }

    public Float getDistance() {
        return distance;
    }

    public void setDistance(Float distance) {
        this.distance = distance;
    }

    public Long getRunningTime() {
        return runningTime;
    }

    public void setRunningTime(Long runningTime) {
        this.runningTime = runningTime;
    }

    public Float getSpeedAvg() {
        return speedAvg;
    }

    public void setSpeedAvg(Float speedAvg) {
        this.speedAvg = speedAvg;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public long getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(long receiveDate) {
        this.receiveDate = receiveDate;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Override
    public String toString() {
        return "RunBasicInfo{" +
                "run_id=" + run_id +
                ", distance=" + distance +
                ", runningTime=" + runningTime +
                ", speedAvg=" + speedAvg +
                ", date=" + date +
                '}';
    }
}
