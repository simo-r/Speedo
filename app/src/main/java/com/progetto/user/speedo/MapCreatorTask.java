package com.progetto.user.speedo;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;

import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.CurrentRun;

import org.osmdroid.util.GeoPoint;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * AsyncTask per l'elaborazione e la creazione della mappa da mostrare
 * nel caso di una corsa già effettuata (stats or social)
 */
class MapCreatorTask extends AsyncTask<Long, Void, MapUpdatedInfo> {

    private static final String MAP_DIALOG_NAME = "MAP_DIALOG";

    private final WeakReference<FragmentActivity> activityWeakReference;
    private final boolean whichDao;


    /* Coordinate della BoundingBox */
    private double minLat = Integer.MAX_VALUE;
    private double maxLat = Integer.MIN_VALUE;
    private double minLong = Integer.MAX_VALUE;
    private double maxLong = Integer.MIN_VALUE;


    /**
     * @param activity activity corrente
     * @param whichDao true se il chiamante è Stats Adapater, false se è Social Adapter
     */
    MapCreatorTask(FragmentActivity activity, boolean whichDao) {
        this.activityWeakReference = new WeakReference<>(activity);
        this.whichDao = whichDao;
    }


    /**
     * Preleva ed elabora le informazioni della corsa da mostrare
     * all'interno della mappa.
     *
     * @param extKey chiave esterna della corsa da mostrare
     * @return il tragitto e le informazioni sulla corsa da mostrare nella mappa,
     * null altrimenti
     */
    @Override
    protected MapUpdatedInfo doInBackground(Long... extKey) {
        MapUpdatedInfo info = new MapUpdatedInfo();
        List<? extends CurrentRun> selectedRun;
        FragmentActivity activity = activityWeakReference.get();
        if (activity == null) return null;
        AppDatabase db = AppDatabase.getDatabase(activity.getApplicationContext());
        if (whichDao) {
            selectedRun = db.currentRunDao().getSelectedRunValues(extKey[0]);
        } else {
            selectedRun = db.socialRunDao().getSelectedRunValues(extKey[0]);
        }

        List<GeoPoint> geoPoints = new ArrayList<>();
        double currentLat;
        double currentLng;
        for (CurrentRun i : selectedRun) {
            if (isCancelled()) return null;
            currentLat = i.getLatitude();
            currentLng = i.getLongitude();
            GeoPoint tmpPoint = new GeoPoint(currentLat, currentLng);
            geoPoints.add(tmpPoint);
            info.addSpeed(i.getSpeed());
            info.addDate(i.getTime());
            info.addDistance(i.getCurrent_distance());
            info.addSpeedAvg(i.getSpeed_avg());
            updateLatLng(currentLat, currentLng);
        }
        info.setPoints(geoPoints);
        if (selectedRun.size() > 0) {
            info.setBounds(maxLat, maxLong, minLat, minLong);
        }
        return info;
    }

    /**
     * Aggiorna le coordinate della bounding box all'interno della quale
     * si trovano tutti i punti in cui l'utente è passato
     *
     * @param currentLat latitudine corrente
     * @param currentLng longitudine corrente
     */
    private void updateLatLng(double currentLat, double currentLng) {
        if (currentLat < minLat)
            minLat = currentLat;
        if (currentLat > maxLat)
            maxLat = currentLat;
        if (currentLng < minLong)
            minLong = currentLng;
        if (currentLng > maxLong)
            maxLong = currentLng;
    }

    /**
     * Mostra le informazioni della corsa all'interno della mappa
     *
     * @param item informazioni da mostrare nella mappa
     */
    @Override
    protected void onPostExecute(MapUpdatedInfo item) {
        FragmentActivity activity = activityWeakReference.get();
        if (item == null || isCancelled() || activity == null) return;
        CustomMapDialog newFragment = CustomMapDialog.newInstance();
        newFragment.setData(item);
        newFragment.show(activity.getSupportFragmentManager(), MAP_DIALOG_NAME);
    }
}
