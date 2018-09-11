package com.progetto.user.speedo;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.util.Log;

import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.SocialRunBasicInfo;
import com.progetto.user.speedo.RoomPackage.SocialRunDao;
import com.progetto.user.speedo.RoomPackage.SocialStats;

import java.util.List;

/**
 * View model che contiene i dati della vista SocialFragment
 */
public class SocialViewModel extends AndroidViewModel {
    private static final String TAG = "SOCIAL-VM";

    private int recyclerViewPosition = 0;
    private int lastSelectedView = -1;

    private LiveData<List<SocialRunBasicInfo>> socialRunValues;
    private final LiveData<SocialStats> exposedSocialStatistics;
    private final SocialRunDao socialRunDao;


    public SocialViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application.getApplicationContext());
        socialRunDao = db.socialRunDao();
        exposedSocialStatistics = socialRunDao.getSocialStatistics();
    }

    public LiveData<SocialStats> getExposedSocialStatistics() {
        return exposedSocialStatistics;
    }

    /**
     * Inizializza un observer per i valori delle corse social
     *
     * @param socialFragment lifecycler owner
     * @param observer       callback
     */
    public void resume(SocialFragment socialFragment, Observer<List<SocialRunBasicInfo>> observer) {
        if (socialRunValues == null) {
            socialRunValues = socialRunDao.getSocialRuns();
        }
        socialRunValues.observe(socialFragment, observer);
        Log.d(TAG, "resume: ");
    }

    public int getRecyclerViewPosition() {
        return recyclerViewPosition;
    }

    public void setRecyclerViewPosition(int recyclerViewPosition) {
        this.recyclerViewPosition = recyclerViewPosition;
    }

    public void setLastSelectedView(int lastSelectedView) {
        this.lastSelectedView = lastSelectedView;
    }

    public int getLastSelectedView() {
        return this.lastSelectedView;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared: ");
    }
}
