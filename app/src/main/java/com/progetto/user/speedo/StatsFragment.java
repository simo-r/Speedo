package com.progetto.user.speedo;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.progetto.user.speedo.RoomPackage.RunBasicInfo;
import com.progetto.user.speedo.RoomPackage.Stats;

import java.util.List;

/**
 * Fragment che mostra le corse già effettuate dall'utente
 */
public class StatsFragment extends Fragment {
    private static final String TAG = "STATS-FRAGMENT";

    private static final String KEY_USER_STATE = "com.progetto.user.speedo.USER_EDIT";
    public static final String KEY_USERNAME = "com.progetto.user.speedo.USERNAME";

    private static final int MAX_USERNAME_CHARS = 10;
    private StatsViewModel svm;
    private StatsRecyclerViewAdapter mRecViewAdapter;
    private ItemTouchHelper itemTouchHelper;
    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecView;
    private EditText username;

    static StatsFragment newInstance() {
        return new StatsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityProvider activityProvider = new ActivityProvider() {
            @Override
            public FragmentActivity getActivity() {
                return StatsFragment.this.getActivity();
            }
        };
        mRecViewAdapter = new StatsRecyclerViewAdapter(activityProvider);
        itemTouchHelper = createCardViewSwipeCallBack();
        svm = ViewModelProviders.of(this).get(StatsViewModel.class);
        svm.resume(this, new Observer<List<RunBasicInfo>>() {
            @Override
            public void onChanged(@Nullable List<RunBasicInfo> runs) {
                if (runs != null && runs.size() > 0) {
                    mRecViewAdapter.setRuns(runs);
                    if (mLayoutManager != null) {
                        mLayoutManager.scrollToPosition(0);
                    }

                    Log.d(TAG, "onChanged: SCROLL");
                }
            }
        });
        Log.d(TAG, "onCreate: ");
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.stats_item_list, container, false);
        final TextView runCount = v.findViewById(R.id.run_count);
        final TextView distanceSum = v.findViewById(R.id.km_count);
        final TextView speedAvg = v.findViewById(R.id.speed_count);
        final TextView timeSum = v.findViewById(R.id.time_count);
        final ImageButton editUsername = v.findViewById(R.id.editUsername);
        final ImageButton cancelUsername = v.findViewById(R.id.cancelUsername);
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        mRecView = v.findViewById(R.id.my_recycler_view);
        CoordinatorLayout mCoordinationLayout = v.findViewById(R.id.coord_layout);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecViewAdapter.setWeakCoordLayout(mCoordinationLayout);
        mRecView.setHasFixedSize(true);
        mRecView.setLayoutManager(mLayoutManager);
        mRecViewAdapter.setLastSelectedViewPosition(svm.getLastSelectedView());
        itemTouchHelper.attachToRecyclerView(mRecView);
        mRecView.setAdapter(mRecViewAdapter);
        CustomStatisticsObserver customStatisticsObserver =
                new CustomStatisticsObserver(runCount, distanceSum, speedAvg, timeSum);
        svm.getExposedStatistics().observe(this, customStatisticsObserver);
        username = v.findViewById(R.id.username);
        if (savedInstanceState != null) {
            boolean wasEnabled = savedInstanceState.getBoolean(KEY_USER_STATE, false);
            if (wasEnabled) {
                Log.d(TAG, "onCreateView: BUNDLE");
                editUsername(cancelUsername, editUsername);
            }
        }

        String currUsername = sp.getString(KEY_USERNAME, null);
        if (currUsername != null) {
            Log.d(TAG, "onCreateView: COSA SENZA SENSO");
            username.setText(currUsername);
        }

        CustomEditOnClickListener customEditOnClickListener =
                new CustomEditOnClickListener(cancelUsername, editUsername);
        editUsername.setOnClickListener(customEditOnClickListener);

        CustomCancelOnClickListener customCancelOnClickListener =
                new CustomCancelOnClickListener(cancelUsername, editUsername, sp);
        cancelUsername.setOnClickListener(customCancelOnClickListener);
        Log.d(TAG, "onCreateView: ");
        return v;
    }


    private void editUsername(ImageButton cancelUsername, ImageButton editUsername) {
        cancelUsername.setVisibility(View.VISIBLE);
        this.username.setEnabled(true);
        editUsername.setImageResource(R.drawable.ic_done);
        this.username.requestFocus();
        this.username.setSelection(this.username.getText().length());
        FragmentActivity activity = getActivity();
        if (activity == null) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void clearEditUsername(ImageButton cancelUsername, ImageButton editUsername) {
        cancelUsername.setVisibility(View.GONE);
        username.setEnabled(false);
        editUsername.setImageResource(R.drawable.ic_edit);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated: ");

    }

    @Override
    public void onResume() {
        super.onResume();
        int position = svm.getLastSelectedView();
        if (position < 0) {
            position = svm.getRecyclerViewPosition();
        }
        mLayoutManager.scrollToPosition(position);
        Log.d(TAG, "onResume: " + position);
    }

    @Override
    public void onPause() {
        super.onPause();
        int currPosition = mLayoutManager.findFirstVisibleItemPosition();
        if (currPosition == RecyclerView.NO_POSITION) currPosition = 0;
        svm.setRecyclerViewPosition(currPosition);
        svm.setLastSelectedView(mRecViewAdapter.getLastSelectedViewPosition());
        Log.d(TAG, "onPause: POSITION " + svm.getRecyclerViewPosition());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_USER_STATE, username.isEnabled());
        Log.d(TAG, "onSaveInstanceState: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    public void scrollToTop() {
        Log.d(TAG, "scroolToTop: ");
        if (mRecView != null) {
            mRecView.smoothScrollToPosition(0);
        }
    }

    /* Crea la callback per l'eliminazione della view quando riceve lo swipe destro */
    private ItemTouchHelper createCardViewSwipeCallBack() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                Log.d(TAG, "onMove: ");
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                if (viewHolder instanceof StatsRecyclerViewAdapter.StatsViewHolder) {
                    StatsRecyclerViewAdapter.StatsViewHolder vh = ((StatsRecyclerViewAdapter.StatsViewHolder) viewHolder);
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

    /**
     * Observer che aggiorna le statistiche dell'utente
     */
    private class CustomStatisticsObserver implements Observer<Stats> {
        private final TextView runCount;
        private final TextView distanceSum;
        private final TextView speedAvg;
        private final TextView timeSum;

        CustomStatisticsObserver(TextView runCount, TextView distanceSum, TextView speedAvg, TextView timeSum) {
            this.runCount = runCount;
            this.distanceSum = distanceSum;
            this.speedAvg = speedAvg;
            this.timeSum = timeSum;
        }

        @Override
        public void onChanged(@Nullable Stats stats) {
            if (stats != null && runCount != null) {
                runCount.setText(String.valueOf(stats.getRunCount()));
                distanceSum.setText(StatsRecyclerViewAdapter.getFormattedDistance(stats.getDistance()));
                speedAvg.setText(StatsRecyclerViewAdapter.getFormattedSpeed(stats.getSpeedAvg()));
                timeSum.setText(String.valueOf(
                        StatsRecyclerViewAdapter.getFormattedTime(stats.getRunningTime())));

            }
        }
    }


    /**
     * OnClickListener per il bottone di edit dell'username
     */
    private class CustomEditOnClickListener implements View.OnClickListener {

        private final ImageButton cancelUsername;
        private final ImageButton editUsername;

        CustomEditOnClickListener(ImageButton cancelUsername, ImageButton editUsername) {
            this.cancelUsername = cancelUsername;
            this.editUsername = editUsername;
        }

        @Override
        public void onClick(View view) {
            if (username.isEnabled()) {
                if (username.getText().length() > MAX_USERNAME_CHARS || username.getText().length() < 1
                        || username.getText().toString().replace(" ", "").length() == 0) {
                    Toast.makeText(getContext(),
                            R.string.max_username_char,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                clearEditUsername(cancelUsername, editUsername);
                Log.d(TAG, "onClick: STO INSERENDO");
                PreferenceManager.getDefaultSharedPreferences(
                        getContext()).edit().putString(KEY_USERNAME,
                        username.getText().toString()).apply();

            } else {
                editUsername(cancelUsername, editUsername);
            }
        }
    }

    /**
     * OnClickListener per il bottone di reset dell'username
     */
    private class CustomCancelOnClickListener implements View.OnClickListener {
        private final ImageButton cancelUsername;
        private final ImageButton editUsername;
        private final SharedPreferences sp;

        CustomCancelOnClickListener(ImageButton cancelUsername, ImageButton editUsername, SharedPreferences sp) {
            this.cancelUsername = cancelUsername;
            this.editUsername = editUsername;
            this.sp = sp;
        }

        @Override
        public void onClick(View view) {
            String currentUsername = sp.getString(KEY_USERNAME, null);
            if (currentUsername != null) {
                username.setText(currentUsername);
            } else {
                username.getText().clear();
                username.setHint(R.string.username);
            }
            clearEditUsername(cancelUsername, editUsername);
        }
    }
}
