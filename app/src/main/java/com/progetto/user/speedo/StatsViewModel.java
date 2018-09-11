package com.progetto.user.speedo;

import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;
import android.util.Log;

import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.CurrentRunDao;
import com.progetto.user.speedo.RoomPackage.RunBasicInfo;
import com.progetto.user.speedo.RoomPackage.Stats;

import java.util.List;

/**
 * View model delle corse dell'utente
 */
public class StatsViewModel extends AndroidViewModel {
    private static final String TAG = "STATS-VIEW-MODEL";

    private int oldSize = 0;
    private int recyclerViewPosition = 0;
    private int lastSelectedView = -1;

    private LiveData<List<RunBasicInfo>> readyRunValues;
    private LiveData<List<RunBasicInfo>> exposedReadyRunValues;
    private final LiveData<Stats> exposedStatistics;

    private final CurrentRunDao currentRunDao;
    private final CustomTransformationFunction customTransformationFunction;

    public StatsViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application.getApplicationContext());
        currentRunDao = db.currentRunDao();
        customTransformationFunction = new CustomTransformationFunction();
        exposedStatistics = currentRunDao.getStatistics();
    }

    public LiveData<Stats> getExposedStatistics() {
        return exposedStatistics;
    }

    /**
     * Inizializza l'owner con la relativa callback
     *
     * @param statsFragment owner
     * @param observer      callback
     */
    public void resume(StatsFragment statsFragment, Observer<List<RunBasicInfo>> observer) {
        if (exposedReadyRunValues == null || readyRunValues == null) {
            readyRunValues = currentRunDao.getReadyRuns();
            Log.d(TAG, "resume: DENTRO");
        } else {
            oldSize = 0;
            exposedReadyRunValues = null;
        }
        exposedReadyRunValues = Transformations.switchMap(readyRunValues, customTransformationFunction);
        exposedReadyRunValues.observe(statsFragment, observer);
        Log.d(TAG, "resume: FUORI");
    }

    /**
     * @return ultima posizione conosciuta della recylcer view dell'owner
     */
    public int getRecyclerViewPosition() {
        return recyclerViewPosition;
    }

    /**
     * @param recyclerViewPosition ultima posizione conosciuta della recylcer view dell'owner
     */
    public void setRecyclerViewPosition(int recyclerViewPosition) {
        this.recyclerViewPosition = recyclerViewPosition;
    }

    /**
     * @param lastSelectedView ultima posizione conosciuta dell'elelemento
     *                         selezionato nella recycler view dell'owner
     */
    public void setLastSelectedView(int lastSelectedView) {
        this.lastSelectedView = lastSelectedView;
    }

    /**
     * @return ultima posizione conosciuta dell'elemento selezionato
     * nella recycler view dell'owner
     */
    public int getLastSelectedView() {
        return this.lastSelectedView;
    }

    /**
     * Trasforma l'input, prelevando solo gli elementi che
     * non sono ancora stati aggiunti alla lista
     */
    private class CustomTransformationFunction implements Function<List<RunBasicInfo>, LiveData<List<RunBasicInfo>>> {
        @Override
        public LiveData<List<RunBasicInfo>> apply(List<RunBasicInfo> input) {
            MutableLiveData<List<RunBasicInfo>> tmpResult = new MutableLiveData<>();
            Log.d(TAG, "apply: INPUT SIZE " + input.size() + " OLD SIZE " + oldSize);
            List<RunBasicInfo> tmp = null;
            if (input.size() > oldSize) {
                int newElements = input.size() - oldSize;
                Log.d(TAG, "apply: NEW ELEMENTS " + newElements);
                tmp = input.subList(0, newElements);
            }
            oldSize = input.size();
            tmpResult.setValue(tmp);
            return tmpResult;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared: ");
    }
}
