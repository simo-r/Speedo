package com.progetto.user.speedo.RoomPackage;

/**
 * Classe per la condivisione delle informazioni via wifi direct
 */
public class P2pCurrentRun {

    private double latitude;

    private double longitude;

    private float speed;

    private float speed_avg;

    private long time;

    private float current_distance;


    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed_avg() {
        return speed_avg;
    }

    public void setSpeed_avg(float speed_avg) {
        this.speed_avg = speed_avg;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getCurrent_distance() {
        return current_distance;
    }

    public void setCurrent_distance(float current_distance) {
        this.current_distance = current_distance;
    }

    @Override
    public String toString() {
        return "P2pCurrentRun{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", speed=" + speed +
                ", speed_avg=" + speed_avg +
                ", time=" + time +
                ", current_distance=" + current_distance +
                '}';
    }

}
