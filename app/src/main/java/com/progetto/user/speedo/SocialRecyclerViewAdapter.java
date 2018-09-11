package com.progetto.user.speedo;

import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
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
import com.progetto.user.speedo.RoomPackage.SocialRunBasicInfo;
import com.progetto.user.speedo.RoomPackage.SocialRunDao;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Adapter per la recycler view che mostra le corse ricevute da altri utenti
 */
public class SocialRecyclerViewAdapter extends RecyclerView.Adapter<SocialRecyclerViewAdapter.SocialViewHolder>
        implements ActionMode.Callback {
    private static final String TAG = "SOCIAL-VA";

    private final ActivityProvider activityProvider;

    private List<SocialRunBasicInfo> runs;
    private final SocialRunDao socialRunDao;
    private WeakReference<CoordinatorLayout> weakCoordLayout;
    private final Executor executor;

    private final int CARD_BG_COLOR;
    private final int CARD_SLCT_BG_COLOR;

    private int lastSelectedViewPosition = -1;
    private long lastSelectedViewKey = -1;
    private WeakReference<SocialViewHolder> lastSelectedViewHolder;

    SocialRecyclerViewAdapter(ActivityProvider activityProvider) {
        this.activityProvider = activityProvider;
        FragmentActivity activity = activityProvider.getActivity();
        AppDatabase db = AppDatabase.getDatabase(activity.getApplicationContext());
        socialRunDao = db.socialRunDao();
        CARD_BG_COLOR = activity.getResources().getColor(R.color.cardViewBackground);
        CARD_SLCT_BG_COLOR = activity.getResources().getColor(R.color.cardViewSelectedBackground);
        executor = Executors.newSingleThreadExecutor();
    }

    @NonNull
    @Override
    public SocialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View tmp = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.social_item_list_content, parent, false);
        return new SocialViewHolder(tmp);
    }

    @Override
    public void onBindViewHolder(@NonNull SocialViewHolder holder, int position) {
        if (runs != null) {
            SocialRunBasicInfo tmp = runs.get(position);
            holder.key = tmp.getRun_id();
            holder.mDateView.setText(StatsRecyclerViewAdapter.getFormattedDate(tmp.getDate()));
            holder.mDurationView.setText(StatsRecyclerViewAdapter.getFormattedTime(tmp.getRunningTime()));
            holder.mDistanceView.setText(StatsRecyclerViewAdapter.getFormattedDistance(tmp.getDistance()));
            holder.mSpeedView.setText(StatsRecyclerViewAdapter.getFormattedSpeed(tmp.getSpeedAvg()));
            holder.mReceiveDateView.setText(StatsRecyclerViewAdapter.getFormattedDate(tmp.getReceiveDate()));
            holder.mSenderName.setText(tmp.getSenderName());

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

    @Override
    public int getItemCount() {
        int res = 0;
        if (runs != null) res = runs.size();
        return res;
    }

    /**
     * Aggiorna i dati dell'adapter
     *
     * @param runs dati da visualizzare
     */
    public void setRuns(List<SocialRunBasicInfo> runs) {
        for (SocialRunBasicInfo i : runs) {
            Log.d(TAG, "setRuns: " + i.toString());
        }
        this.runs = runs;
        notifyDataSetChanged();

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

    class SocialViewHolder extends RecyclerView.ViewHolder
            implements View.OnLongClickListener, View.OnClickListener {

        private long key;
        private final TextView mSpeedView;
        private final TextView mDistanceView;
        private final TextView mDurationView;
        private final TextView mDateView;
        private final CardView mCardView;
        private final FloatingActionButton mMapButton;
        private final TextView mReceiveDateView;
        private final TextView mSenderName;

        SocialViewHolder(View view) {
            super(view);
            mCardView = view.findViewById(R.id.card_view);
            mSpeedView = view.findViewById(R.id.speedTextView);
            mDistanceView = view.findViewById(R.id.distanceTextView);
            mDurationView = view.findViewById(R.id.durationTextView);
            mDateView = view.findViewById(R.id.dateText);
            mMapButton = view.findViewById(R.id.mapButton);
            mReceiveDateView = view.findViewById(R.id.receive_date);
            mSenderName = view.findViewById(R.id.sender_name);
            mCardView.setOnLongClickListener(this);
            mMapButton.setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {
            if (view == mMapButton) {
                FragmentActivity activity = activityProvider.getActivity();
                if (activity != null) {
                    MapCreatorTask mapCreatorTask =
                            new MapCreatorTask(activityProvider.getActivity(), false);
                    mapCreatorTask.execute(key);
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            return view == mCardView && showActionMode(this);
        }

        public long getKey() {
            return key;
        }
    }

    /**
     * Apre la CAB relativa al view holder passato
     *
     * @param viewHolder vista a cui associare la cab
     * @return true se è stata mostrata la cab, false se l'activity è nulla
     */
    private boolean showActionMode(SocialViewHolder viewHolder) {
        MainActivity activity = (MainActivity) activityProvider.getActivity();
        if (activity == null) return false;

        if (lastSelectedViewPosition != -1) {
            activity.closeActionMode();
        }
        initLastSelectedView(viewHolder);
        activity.showActionMode(this);
        Log.d(TAG, "showActionMode: ");
        return true;
    }

    /**
     * Elimina dalla vista e dal database l'elemento in position
     * e con chiave key
     *
     * @param position posizione dell'elemento da eliminare
     * @param key      chiave nel db dell'elemento da eliminare, -1 se è già stato eliminato
     */
    void removeStatsData(int position, final long key) {
        Log.d(TAG, "removeStatsData: ");
        runs.remove(position);
        /* Senza queste due call non fa le animazioni */
        notifyItemRemoved(position);
        if (key != -1) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    socialRunDao.removeRun(key);
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
    }

    public int getLastSelectedViewPosition() {
        return lastSelectedViewPosition;
    }

    public void setLastSelectedViewPosition(int lastSelectedViewPosition) {
        this.lastSelectedViewPosition = lastSelectedViewPosition;
    }

    /**
     * Rimuove le info riguardanti l'ultimo view holder selezionato
     */
    private void clearLastSelectedView() {
        if (lastSelectedViewHolder != null) {
            SocialViewHolder selectedViewHolder = lastSelectedViewHolder.get();
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
     * Inizializza le info riguardanti l'ultimo view holder selezionato
     *
     * @param viewHolder vista selezionata
     */
    private void initLastSelectedView(SocialViewHolder viewHolder) {
        Log.d(TAG, "initLastSelectedView: ");
        lastSelectedViewPosition = viewHolder.getAdapterPosition();
        lastSelectedViewKey = viewHolder.key;
        lastSelectedViewHolder = new WeakReference<>(viewHolder);
        viewHolder.mCardView.setSelected(true);
        viewHolder.mCardView.setCardBackgroundColor(CARD_SLCT_BG_COLOR);
    }

    /* CAB CALLBACK */

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.social_action_menu, menu);
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

}
