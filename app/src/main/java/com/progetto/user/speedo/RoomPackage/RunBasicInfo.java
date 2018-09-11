package com.progetto.user.speedo.RoomPackage;

import android.arch.persistence.room.Entity;

/**
 * Tabella con le informazioni di base di una corsa
 */
@Entity
public class RunBasicInfo extends SocialRunBasicInfo {

    /* true se la corsa è finita, false altrimenti */
    private Boolean ready;

    public Boolean isReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    @Override
    public String toString() {
        return "RunBasicInfo{" +
                "run_id=" + run_id +
                ", distance=" + distance +
                ", runningTime=" + runningTime +
                ", speedAvg=" + speedAvg +
                ", ready=" + ready +
                ", date=" + date +
                '}';
    }

}
