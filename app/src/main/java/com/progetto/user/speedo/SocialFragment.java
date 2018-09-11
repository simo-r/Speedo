package com.progetto.user.speedo;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.progetto.user.speedo.RoomPackage.SocialRunBasicInfo;
import com.progetto.user.speedo.RoomPackage.SocialStats;

import java.util.List;

/**
 * Fragment relativo alle corse social ricevute
 * via wi fi direct
 */
public class SocialFragment extends Fragment {
    private static final String TAG = "SOCIAL-FRAGMENT";

    private SocialViewModel svm;
    private SocialRecyclerViewAdapter mRecViewAdapter;
    private ItemTouchHelper itemTouchHelper;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecView;

    /* Ritorna una nuova istanza del fragment */
    static SocialFragment newInstance() {
        return new SocialFragment();
    }

    /**
     * Crea la callback per lo swipe delle card view,
     * inizializza il view model e effettua il resume se è
     * avvenuto un cambio di configurazione (rotazione dello schermo)
     *
     * @param savedInstanceState bundle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        ActivityProvider activityProvider = new ActivityProvider() {
            @Override
            public FragmentActivity getActivity() {
                return SocialFragment.this.getActivity();
            }
        };
        mRecViewAdapter = new SocialRecyclerViewAdapter(activityProvider);
        itemTouchHelper = createCardViewSwipeCallBack();
        svm = ViewModelProviders.of(this).get(SocialViewModel.class);
        CustomSocialObserver customSocialObserver = new CustomSocialObserver();
        svm.resume(this, customSocialObserver);

        Log.d(TAG, "onCreate: ");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View v = inflater.inflate(R.layout.social_item_list, container, false);
        mRecView = v.findViewById(R.id.my_recycler_view);
        CoordinatorLayout mCoordinationLayout = v.findViewById(R.id.coord_layout);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecViewAdapter.setWeakCoordLayout(mCoordinationLayout);
        mRecView.setHasFixedSize(true);
        mRecView.setLayoutManager(mLayoutManager);
        mRecViewAdapter.setLastSelectedViewPosition(svm.getLastSelectedView());
        itemTouchHelper.attachToRecyclerView(mRecView);
        mRecView.setAdapter(mRecViewAdapter);
        final TextView sentCounterText = v.findViewById(R.id.sent_count_run);
        final TextView receivedCounterText = v.findViewById(R.id.received_count_run);
        CustomSocialStatsObserver customSocialStatsObserver =
                new CustomSocialStatsObserver(sentCounterText, receivedCounterText);
        svm.getExposedSocialStatistics().observe(this, customSocialStatsObserver);
        Log.d(TAG, "onCreateView: ");
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        int position = svm.getLastSelectedView();
        if (position < 0) position = svm.getRecyclerViewPosition();
        mLayoutManager.scrollToPosition(position);
        Log.d(TAG, "onResume: ");
    }

    @Override
    public void onPause() {
        super.onPause();
        int currPosition = mLayoutManager.findFirstVisibleItemPosition();
        if (currPosition == RecyclerView.NO_POSITION) currPosition = 0;
        svm.setRecyclerViewPosition(currPosition);
        svm.setLastSelectedView(mRecViewAdapter.getLastSelectedViewPosition());
        Log.d(TAG, "onPause: ");
    }

    /* Gestisce lo swipe destro della card view per la delete */
    private ItemTouchHelper createCardViewSwipeCallBack() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        Log.d(TAG, "onMove: ");
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        if (viewHolder instanceof SocialRecyclerViewAdapter.SocialViewHolder) {
                            SocialRecyclerViewAdapter.SocialViewHolder vh =
                                    ((SocialRecyclerViewAdapter.SocialViewHolder) viewHolder);
                            long key = vh.getKey();
                            MainActivity activity = (MainActivity) getActivity();
                            if (activity != null) activity.closeActionMode();
                            mRecViewAdapter.removeStatsData(viewHolder.getAdapterPosition(), key);
                        }
                        Log.d(TAG, "onSwiped: ");
                    }
                };
        return new ItemTouchHelper(simpleItemTouchCallback);
    }


    public void scrollToTop() {
        Log.d(TAG, "scrollToTop: ");
        if (mRecView != null) {
            mRecView.smoothScrollToPosition(0);
        }
    }

    private class CustomSocialObserver implements Observer<List<SocialRunBasicInfo>> {

        @Override
        public void onChanged(@Nullable List<SocialRunBasicInfo> runs) {
            if (runs != null) {
                mRecViewAdapter.setRuns(runs);
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) activity.closeActionMode();
                if (mLayoutManager != null) {
                    mLayoutManager.scrollToPosition(0);
                }
                Log.d(TAG, "onChanged: SCROLL");
            }
        }
    }

    private class CustomSocialStatsObserver implements Observer<SocialStats> {
        private final TextView sentCounterText;
        private final TextView receivedCounterText;

        CustomSocialStatsObserver(TextView sentCounterText, TextView receivedCounterText) {
            this.sentCounterText = sentCounterText;
            this.receivedCounterText = receivedCounterText;
        }

        @Override
        public void onChanged(@Nullable SocialStats socialStats) {
            if (socialStats != null) {
                sentCounterText.setText(String.valueOf(socialStats.getSentCounter()));
                receivedCounterText.setText(String.valueOf(socialStats.getReceivedCounter()));
            }
        }
    }

}
