package com.progetto.user.speedo;

import android.app.Application;
import android.app.PendingIntent;
import android.arch.core.util.Function;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.CurrentRun;
import com.progetto.user.speedo.RoomPackage.CurrentRunDao;
import com.progetto.user.speedo.RoomPackage.ReadyRunBasicInfo;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * View model che contiene i dati della vista RunFragment
 */
public class RunningViewModel extends AndroidViewModel {
    private static final String TAG = "RUNNING-VIEW-MODEL";

    private boolean isRunning;

    private final CurrentRunDao currentRunDao;

    private long lastIndex;
    private LiveData<List<CurrentRun>> currentRunValues;
    private LiveData<MapUpdatedInfo> exposedRunValues;

    private final CustomTransformationFunction customTransformationFunction;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private PendingIntent pendingLocationIntent;

    private final Executor executor;

    /* Coordinate della BoundingBox */
    private double minLat = Integer.MAX_VALUE;
    private double maxLat = Integer.MIN_VALUE;
    private double minLong = Integer.MAX_VALUE;
    private double maxLong = Integer.MIN_VALUE;

    public RunningViewModel(@NonNull Application application) {
        super(application);
        isRunning = false;
        lastIndex = 0;
        AppDatabase db = AppDatabase.getDatabase(application.getApplicationContext());
        currentRunDao = db.currentRunDao();

        customTransformationFunction = new CustomTransformationFunction();
        executor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "costructor: ");
    }

    /**
     * Imposta la chiave esterna del database alla corsa corrente,
     * setta le callback per ricevere l'update dai live-data
     *
     * @param runFragment        owner
     * @param currentRunObserver callback
     */
    public void setCurrentExternalKey(final RunFragment runFragment, final Observer<MapUpdatedInfo> currentRunObserver) {
        CurrentRunSetterTask task = new CurrentRunSetterTask(runFragment, currentRunObserver);
        executor.execute(task);

    }


    LiveData<MapUpdatedInfo> getExposedRunValues() {
        return exposedRunValues;
    }

    /**
     * Accoda al thread l'esecuzione del task per l'update delle
     * informazioni sulla locazione
     */
    private MutableLiveData<MapUpdatedInfo> getTrasformedValues(final List<CurrentRun> input) {
        MutableLiveData<MapUpdatedInfo> tmpResult = new MutableLiveData<>();
        CurrentRunUpdaterTask task = new CurrentRunUpdaterTask(input, tmpResult);
        executor.execute(task);
        Log.d(TAG, "run: ABCD RETURNING VALUE");
        return tmpResult;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared: ABCDE");
    }


    /**
     * Nel caso in cui venga chiamata la oncleared() e l'utente
     * stia correndo inizializza i livedata per poter visualizzare
     * le informazioni
     *
     * @param runFragment        owner
     * @param currentRunObserver callback
     */
    public void resumeRun(final RunFragment runFragment, final Observer<MapUpdatedInfo> currentRunObserver) {

        if (exposedRunValues == null || currentRunValues == null) {
            /* Sse il modello è stato onCleared*/
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    lastIndex = 0;
                    ReadyRunBasicInfo resumeRun = currentRunDao.getResumeRun();
                    long key = 1;
                    if (resumeRun != null) {
                        key = resumeRun.getRun_id();
                        if (resumeRun.isReady()) key++;
                        Log.d(TAG, "run: ULTIMO ID NO READY " + resumeRun.toString());
                    }
                    currentRunValues = currentRunDao.getCurrentRunValues(key);
                    exposedRunValues = Transformations.switchMap(currentRunValues, customTransformationFunction);
                    exposedRunValues.observe(runFragment, currentRunObserver);
                    Log.d(TAG, "resume run:  LAST KEY: " + lastIndex + "CURRENT ID: " + key);
                }
            });
        } else {
            lastIndex = 0;
            /*
              Devo reinit a causa dell'implementazione della
              polyline che nella ondetach nulla tutto.
             */
            exposedRunValues = null;
            exposedRunValues = Transformations.switchMap(currentRunValues, customTransformationFunction);
            exposedRunValues.observe(runFragment, currentRunObserver);
            Log.d(TAG, "resumeRun: ");
        }

    }


    /* Task per l'update delle informazioni riguardante la corsa corrente*/
    private class CurrentRunUpdaterTask implements Runnable {

        private final MutableLiveData<MapUpdatedInfo> tmpResult;
        private final List<CurrentRun> input;

        CurrentRunUpdaterTask(List<CurrentRun> input, MutableLiveData<MapUpdatedInfo> tmpResult) {
            this.input = input;
            this.tmpResult = tmpResult;
        }

        @Override
        public void run() {
            MapUpdatedInfo info = new MapUpdatedInfo();
            List<GeoPoint> geoPoints = new ArrayList<>();
            double currentLat;
            double currentLng;
            for (CurrentRun i : input.subList(Long.valueOf(lastIndex).intValue(), input.size())) {
                Log.d(TAG, "getTrasformedValues: ABCDE " + i.toString() + " INDEX " + lastIndex + " ITERAZONE: ");
                currentLat = i.getLatitude();
                currentLng = i.getLongitude();
                /* Non mi fa riciclare i geopoint e neanche tutto il resto.*/
                geoPoints.add(new GeoPoint(currentLat, currentLng));
                info.addSpeed(i.getSpeed());
                info.addDate(i.getTime());
                info.addDistance(i.getCurrent_distance());
                info.addSpeedAvg(i.getSpeed_avg());
                updateLatLng(currentLat, currentLng);
            }
            for (GeoPoint i : geoPoints) {
                Log.d(TAG, "run: GEOPOINTS " + i.toString());
            }
            info.setGeoPoints(geoPoints);
            if (input.size() > 0) {
                lastIndex = input.size() - 1;
                info.setBounds(maxLat, maxLong, minLat, minLong);
                Log.d(TAG, "getTrasformedValues: NEW INDEX " + lastIndex);
            }
            /*
                Non posso ottimizzare in spazio perché eseguendo postValue, anche se sono
                in single thread (+ main) non so quando verrà eseguito realmente e quindi potrei
                avere questo + main thread che lavorano su info
             */
            tmpResult.postValue(info);
            Log.d(TAG, "run: POSTED VALUES");
        }

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
    }

    /* Task per inizializzare una nuova corsa */
    private class CurrentRunSetterTask implements Runnable {

        private final RunFragment runFragment;
        private final Observer<MapUpdatedInfo> currentRunObserver;

        private CurrentRunSetterTask(RunFragment runFragment, Observer<MapUpdatedInfo> currentRunObserver) {
            this.runFragment = runFragment;
            this.currentRunObserver = currentRunObserver;
        }

        @Override
        public void run() {
            resetValues();
            /* +1 perché l'aggiunta della corsa al db viene fatta dopo dal JI-service */
            long currentId = currentRunDao.getLastRunId() + 1;
            currentRunValues = currentRunDao.getCurrentRunValues(currentId);
            exposedRunValues = Transformations.switchMap(currentRunValues, customTransformationFunction);
            exposedRunValues.observe(runFragment, currentRunObserver);
            Log.d(TAG, "run SETTER:  LAST KEY: " + lastIndex + "CURRENT ID: " + currentId);
        }

        private void resetValues() {
            minLat = Integer.MAX_VALUE;
            maxLat = Integer.MIN_VALUE;
            minLong = Integer.MAX_VALUE;
            maxLong = Integer.MIN_VALUE;
            lastIndex = 0;
        }
    }

    private class CustomTransformationFunction implements Function<List<CurrentRun>, LiveData<MapUpdatedInfo>> {

        @Override
        public LiveData<MapUpdatedInfo> apply(List<CurrentRun> input) {
            return getTrasformedValues(input);
        }
    }


    public FusedLocationProviderClient getmFusedLocationClient() {
        return mFusedLocationClient;
    }

    public void setmFusedLocationClient(FusedLocationProviderClient mFusedLocationClient) {
        this.mFusedLocationClient = mFusedLocationClient;
    }

    public LocationRequest getmLocationRequest() {
        return mLocationRequest;
    }

    public void setmLocationRequest(LocationRequest mLocationRequest) {
        this.mLocationRequest = mLocationRequest;
    }

    public PendingIntent getPendingLocationIntent() {
        return pendingLocationIntent;
    }

    public void setPendingLocationIntent(PendingIntent pendingLocationIntent) {
        this.pendingLocationIntent = pendingLocationIntent;
    }
}
