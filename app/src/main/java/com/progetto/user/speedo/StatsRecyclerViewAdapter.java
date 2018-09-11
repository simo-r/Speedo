package com.progetto.user.speedo;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.progetto.user.speedo.RoomPackage.AppDatabase;
import com.progetto.user.speedo.RoomPackage.CurrentRunDao;
import com.progetto.user.speedo.RoomPackage.RunBasicInfo;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/* Adapter per la recycler view che mostra le corse dell'utente */
class StatsRecyclerViewAdapter extends RecyclerView.Adapter<StatsRecyclerViewAdapter.StatsViewHolder>
        implements ActionMode.Callback {
    private static final String TAG = "STATS-VA";

    private final ActivityProvider activityProvider;

    private List<RunBasicInfo> runs;
    private final CurrentRunDao currentRunDao;
    private WeakReference<CoordinatorLayout> weakCoordLayout;
    private final Executor executor;
    private final int CARD_BG_COLOR;
    private final int CARD_SLCT_BG_COLOR;

    private int lastSelectedViewPosition = -1;
    private long lastSelectedViewKey = -1;
    private WeakReference<StatsViewHolder> lastSelectedViewHolder;

    StatsRecyclerViewAdapter(ActivityProvider activityProvider) {
        this.activityProvider = activityProvider;
        FragmentActivity activity = activityProvider.getActivity();
        AppDatabase db = AppDatabase.getDatabase(activity.getApplicationContext());
        currentRunDao = db.currentRunDao();
        CARD_BG_COLOR = activity.getResources().getColor(R.color.cardViewBackground);
        CARD_SLCT_BG_COLOR = activity.getResources().getColor(R.color.cardViewSelectedBackground);
        executor = Executors.newSingleThreadExecutor();
    }

    @NonNull
    @Override
    public StatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View tmp = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.stats_item_list_content, parent, false);
        return new StatsViewHolder(tmp);
    }

    @Override
    public void onBindViewHolder(@NonNull StatsViewHolder holder, int position) {
        if (runs != null) {
            RunBasicInfo tmp = runs.get(position);

            holder.key = tmp.getRun_id();
            holder.mDateView.setText(getFormattedDate(tmp.getDate()));
            holder.mDurationView.setText(getFormattedTime(tmp.getRunningTime()));
            holder.mDistanceView.setText(getFormattedDistance(tmp.getDistance()));
            holder.mSpeedView.setText(getFormattedSpeed(tmp.getSpeedAvg()));
            // Altrimenti quando ricicla mi mette il background selezionato in quella riciclata
            if (position == lastSelectedViewPosition) {
                Log.d(TAG, "onBindViewHolder: E' POSITION");
                showActionMode(holder);
            } else {
                Log.d(TAG, "onBindViewHolder: NON è POSITION");
                holder.mCardView.setSelected(false);
                holder.mCardView.setCardBackgroundColor(CARD_BG_COLOR);
            }
        }
    }

    static String getFormattedDistance(float distance) {
        return String.format(Locale.getDefault(), "%.2f", distance);
    }

    static String getFormattedSpeed(float speed) {
        return String.format(Locale.getDefault(), "%.2f", speed);
    }

    static CharSequence getFormattedDate(long date) {
        return DateFormat.format(
                RunFragment.DATE_FORMATTER, date);
    }

    static String getFormattedTime(Long runningTime) {
        long hours = TimeUnit.MILLISECONDS.toHours(runningTime);
        long fk_minutes = TimeUnit.MILLISECONDS.toMinutes(runningTime);
        long minutes = fk_minutes -
                TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(runningTime) -
                TimeUnit.MINUTES.toSeconds(fk_minutes);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public int getItemCount() {
        int res = 0;
        if (runs != null) res = runs.size();
        return res;
    }

    /**
     * @param runs lista delle nuove corse
     */
    public void setRuns(List<RunBasicInfo> runs) {

        for (RunBasicInfo i : runs) {
            Log.d(TAG, "setRuns: " + i.toString());
        }
        if (this.runs == null) {
            Log.d(TAG, "setRuns: NULL");
            this.runs = runs;
        } else {
            Log.d(TAG, "setRuns: NOT NULL");
            this.runs.addAll(0, runs);
        }
        notifyItemRangeInserted(0, runs.size());

    }


    /**
     * Imposta il coord layout per abilitare lo swipe to dismiss
     * della snackbar
     *
     * @param weakCoordLayout coordinator layout della vista corrente
     */
    public void setWeakCoordLayout(CoordinatorLayout weakCoordLayout) {
        this.weakCoordLayout = new WeakReference<>(weakCoordLayout);
    }

    /* VIEW HOLDER */

    class StatsViewHolder extends RecyclerView.ViewHolder
            implements View.OnLongClickListener, View.OnClickListener {
        private long key;
        private final TextView mSpeedView;
        private final TextView mDistanceView;
        private final TextView mDurationView;
        private final TextView mDateView;
        private final CardView mCardView;
        private final FloatingActionButton mMapButton;

        StatsViewHolder(View view) {
            super(view);
            mCardView = view.findViewById(R.id.card_view);
            mSpeedView = view.findViewById(R.id.speedTextView);
            mDistanceView = view.findViewById(R.id.distanceTextView);
            mDurationView = view.findViewById(R.id.durationTextView);
            mDateView = view.findViewById(R.id.dateText);
            mMapButton = view.findViewById(R.id.mapButton);
            mCardView.setOnLongClickListener(this);
            mMapButton.setOnClickListener(this);
        }

        @Override
        public boolean onLongClick(View view) {
            return view == mCardView && showActionMode(this);
        }

        /**
         * Se viene premuta la view della mappa crea la mappa relativa alla corsa
         * associata a questo view holder
         *
         * @param view view
         */
        @Override
        public void onClick(View view) {
            if (view == mMapButton) {
                FragmentActivity activity = activityProvider.getActivity();
                if (activity != null) {
                    MapCreatorTask mapCreatorTask = new MapCreatorTask(activityProvider.getActivity(), true);
                    mapCreatorTask.execute(key);
                }
            }
        }

        public long getKey() {
            return key;
        }

    }

    private boolean showActionMode(StatsViewHolder viewHolder) {
        MainActivity activity = (MainActivity) activityProvider.getActivity();
        if (activity == null) return false;

        if (lastSelectedViewPosition != -1) {
            activity.closeActionMode();
        }
        initLastSelectedView(viewHolder);
        activity.showActionMode(this);
        Log.d(TAG, "onLongClick: ");
        return true;
    }

    /**
     * Elimina dalla vista e dal database l'elemento in position
     * e con chiave key
     *
     * @param position posizione dell'elemento da eliminare
     * @param key      chiave nel db dell'elemento da elimianre
     */
    void removeStatsData(int position, final long key) {
        Log.d(TAG, "removeStatsData: POSITION " + position + " SELECTED " + lastSelectedViewPosition);
        runs.remove(position);
        /* Senza queste due call non fa le animazioni */
        notifyItemRemoved(position);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                currentRunDao.removeRun(key);
                /* Posso fare show perché accoda sul thread UI l'operazione */
                CoordinatorLayout coordinatorLayout = weakCoordLayout.get();
                if (coordinatorLayout != null) {
                    Snackbar.make(coordinatorLayout,
                            R.string.run_deleted, Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    public int getLastSelectedViewPosition() {
        return lastSelectedViewPosition;
    }

    public void setLastSelectedViewPosition(int lastSelectedViewPosition) {
        this.lastSelectedViewPosition = lastSelectedViewPosition;
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.run_action_menu, menu);
        Log.d(TAG, "onCreateActionMode: ");
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        Log.d(TAG, "onPrepareActionMode: ");
        // Non consumato
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.direct:
                MainActivity activity = (MainActivity) activityProvider.getActivity();
                if (activity == null) {
                    Log.d(TAG, "onActionItemClicked: NULL ACTIVITY");
                    actionMode.finish();
                    return true;
                }
                activity.enableWifiDirectShare(this.lastSelectedViewKey);
                actionMode.finish();
                Log.d(TAG, "onActionItemClicked: DIRECT");
                return true;
            case R.id.share:
                Log.d(TAG, "onActionItemClicked: SHARE");
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                String content = getShareContent(lastSelectedViewHolder);
                sendIntent.putExtra(Intent.EXTRA_TEXT, content);
                sendIntent.setType("text/plain");
                FragmentActivity fragmentActivity = activityProvider.getActivity();
                if (fragmentActivity != null) {
                    fragmentActivity.startActivity(sendIntent);
                }
                actionMode.finish();
                return true;
            case R.id.delete:
                if (lastSelectedViewPosition < runs.size()) {
                    Log.d(TAG, "onClick: PIù PICCOLO");
                    removeStatsData(lastSelectedViewPosition, lastSelectedViewKey);
                }
                actionMode.finish();
                Log.d(TAG, "onActionItemClicked: DELETE");
                return true;
            default:
                Log.d(TAG, "onActionItemClicked: DEFAULT");
                return false;

        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        Log.d(TAG, "onDestroyActionMode: ");
        clearLastSelectedView();
    }

    /**
     * Estrapola i dati da condividere attraverso le app di messagistica.
     *
     * @param lastSelectedViewHolder view holder da cui ricavare i dati da condividere
     * @return stringa contenente i dati da condividere
     */
    private String getShareContent(WeakReference<StatsViewHolder> lastSelectedViewHolder) {
        Context context = activityProvider.getActivity();
        if (context == null) return null;
        if (lastSelectedViewHolder == null) return null;
        StatsViewHolder statsViewHolder = lastSelectedViewHolder.get();
        if (statsViewHolder == null) return null;
        CharSequence date = statsViewHolder.mDateView.getText();
        CharSequence time = statsViewHolder.mDurationView.getText();
        CharSequence distance = statsViewHolder.mDistanceView.getText();
        CharSequence speedAvg = statsViewHolder.mSpeedView.getText();
        return context.getString(R.string.share_content, date, time, distance, speedAvg);
    }

    /**
     * Elimina le informazioni che riguardano l'ultima vista selezionata
     */
    private void clearLastSelectedView() {
        if (lastSelectedViewHolder != null) {
            StatsViewHolder selectedViewHolder = lastSelectedViewHolder.get();
            if (selectedViewHolder != null) {
                Log.d(TAG, "clearLastSelectedView: NOT NULL");
                selectedViewHolder.mCardView.setSelected(false);
                selectedViewHolder.mCardView.setCardBackgroundColor(CARD_BG_COLOR);

            } else {
                Log.d(TAG, "clearLastSelectedView: E' NULLO VIEWHOLDER");
            }
        } else {
            Log.d(TAG, "clearLastSelectedView: E' NULLO REFERENCE");
        }
        lastSelectedViewPosition = -1;
        lastSelectedViewKey = -1;
    }

    /**
     * Inizializza le informazioni che riguardano l'ultima vista selezionata
     *
     * @param viewHolder view holder della vista selezionata
     */
    private void initLastSelectedView(StatsViewHolder viewHolder) {
        Log.d(TAG, "initLastSelectedView: ");
        lastSelectedViewPosition = viewHolder.getAdapterPosition();
        lastSelectedViewKey = viewHolder.key;
        lastSelectedViewHolder = new WeakReference<>(viewHolder);
        viewHolder.mCardView.setSelected(true);
        viewHolder.mCardView.setCardBackgroundColor(CARD_SLCT_BG_COLOR);
    }

}
