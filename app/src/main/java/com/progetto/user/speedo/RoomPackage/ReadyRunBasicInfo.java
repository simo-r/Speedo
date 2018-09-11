package com.progetto.user.speedo.RoomPackage;

/**
 * Classe che contiene chiave e stato di una corsa
 */
public class ReadyRunBasicInfo {

    private Long run_id;

    private Boolean ready;

    public Long getRun_id() {
        return run_id;
    }

    public void setRun_id(Long run_id) {
        this.run_id = run_id;
    }

    public Boolean isReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    @Override
    public String toString() {
        return "ReadyRunBasicInfo{" +
                "run_id=" + run_id +
                ", ready=" + ready +
                '}';
    }
}
