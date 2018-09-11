package com.progetto.user.speedo.RoomPackage;

/**
 * Classe per la condivisione delle informazioni via wifi direct
 */
public class P2pRunBasicInfo {

    private float distance;

    private long runningTime;

    private float speedAvg;

    private long date;

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public long getRunningTime() {
        return runningTime;
    }

    public void setRunningTime(long runningTime) {
        this.runningTime = runningTime;
    }

    public float getSpeedAvg() {
        return speedAvg;
    }

    public void setSpeedAvg(float speedAvg) {
        this.speedAvg = speedAvg;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "P2pRunBasicInfo{" +
                "distance=" + distance +
                ", runningTime=" + runningTime +
                ", speedAvg=" + speedAvg +
                ", date=" + date +
                '}';
    }
}
