package com.progetto.user.speedo.RoomPackage;

/**
 * Dati da visualizzare nel tab stats
 */
public class Stats {

    private int runCount;

    private float distance;

    private long runningTime;

    private float speedAvg;

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

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

    @Override
    public String toString() {
        return "Stats{" +
                "runCount=" + runCount +
                ", distance=" + distance +
                ", runningTime=" + runningTime +
                ", speedAvg=" + speedAvg +
                '}';
    }
}
