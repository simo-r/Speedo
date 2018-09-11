package com.progetto.user.speedo.RoomPackage;

import android.arch.persistence.room.Entity;

/**
 * Tabella contenente le informazioni sul tragitto effettuato ricevute
 * via wifi direct, eredita tutti i campi.
 */
@Entity(inheritSuperIndices = true)
public class SocialCurrentRun extends CurrentRun {

}
