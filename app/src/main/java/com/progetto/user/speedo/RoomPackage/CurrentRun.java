package com.progetto.user.speedo.RoomPackage;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(foreignKeys = @ForeignKey(entity = RunBasicInfo.class, parentColumns = "run_id",
        childColumns = "extRun_id", onDelete = CASCADE), indices = @Index("extRun_id"))
public class CurrentRun {
    @PrimaryKey(autoGenerate = true)
    private Long id;

    private double latitude;

    private double longitude;

    private float speed;

    private float speed_avg;

    private long time;

    private float current_distance;

    private Long extRun_id;

    public Long getId() {
        return id;
    }

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

    public void setExtRun_id(Long extRun_id) {
        this.extRun_id = extRun_id;
    }

    public float getSpeed_avg() {
        return speed_avg;
    }

    public void setSpeed_avg(float speed_avg) {
        this.speed_avg = speed_avg;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExtRun_id() {
        return extRun_id;
    }

    @Override
    public String toString() {
        return "CurrentRun{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", speed=" + speed +
                ", speed_avg=" + speed_avg +
                ", time=" + time +
                ", distance=" + current_distance +
                ", extRun_id=" + extRun_id +
                '}';
    }
}


