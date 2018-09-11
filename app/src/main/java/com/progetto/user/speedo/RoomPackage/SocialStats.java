package com.progetto.user.speedo.RoomPackage;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Contatori sulle corse inviate e ricevute via wifi direct
 */
@Entity
public class SocialStats {

    @PrimaryKey(autoGenerate = true)
    private int stat_id;

    private int sentCounter;

    private int receivedCounter;

    public int getStat_id() {
        return stat_id;
    }

    public void setStat_id(int stat_id) {
        this.stat_id = stat_id;
    }

    public int getSentCounter() {
        return sentCounter;
    }

    public void setSentCounter(int sentCounter) {
        this.sentCounter = sentCounter;
    }

    public int getReceivedCounter() {
        return receivedCounter;
    }

    public void setReceivedCounter(int receivedCounter) {
        this.receivedCounter = receivedCounter;
    }

    @Override
    public String toString() {
        return "SocialStats{" +
                "key=" + stat_id +
                ", sentCounter=" + sentCounter +
                ", receivedCounter=" + receivedCounter +
                '}';
    }
}
