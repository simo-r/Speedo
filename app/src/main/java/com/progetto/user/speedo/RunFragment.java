package com.progetto.user.speedo;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

/**
 * Fragment che si occupa di iniziare o fermare
 * la corsa dell'utente
 */
public class RunFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "RUN-FRAGMENT";

    /* Zoom massimo della mappa perché non ci sono tile con zoom > 19*/
    static final double MAX_ZOOM = 19d;
    static final String KEY_ISRUNNING = "com.progetto.user.speedo.KEY_ISRUNNING";
    static final int LOCATION_BROADCAST_ID = 33333;
    public static final String DATE_FORMATTER = "EEE, d MMM yyyy HH:mm:ss";
    private Context mContext;

    private TextView runDistance;
    private TextView runSpeed;
    private FloatingActionButton runButton;
    private SharedPreferences sp;
    private RunningViewModel rvm;
    private MapView runMap;
    private OverlayManager mapOverlayManager;
    private IMapController controller;
    private Observer<MapUpdatedInfo> currentRunObserver;

    /* Ritorna nuova istanza del fragment */
    static RunFragment newInstance() {
        return new RunFragment();
    }

    /**
     * Observer per live-data riguardanti la corsa corrente,
     * aggiorna le view del fragment e la mappa
     */
    private class RunObserver implements Observer<MapUpdatedInfo> {

        @Override
        public void onChanged(@Nullable MapUpdatedInfo item) {
            if (item == null) return;
            Polyline currentPolyLine = item.getPolyline();
            BoundingBox currentBB = item.getBoundingBox();
            if (currentPolyLine == null || currentBB == null) return;
            ArrayList<GeoPoint> points = currentPolyLine.getPoints();
            if (points == null || points.size() == 0) return;
            runDistance.setText(StatsRecyclerViewAdapter.getFormattedDistance(item.getDistance(points.size() - 1)));
            runSpeed.setText(StatsRecyclerViewAdapter.getFormattedSpeed(item.getSpeedAvg(points.size() - 1)));
            Log.d(TAG, "onChanged: DISTANCE " + item.getDistance(points.size() - 1) + " SPEED " + item.getSpeedAvg(points.size() - 1));
            CustomMapDialog.updateMap(item, currentPolyLine, currentBB, mContext, runMap);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        Log.d(TAG, "onAttach: ");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rvm = ViewModelProviders.of(this).get(RunningViewModel.class);

        currentRunObserver = new RunObserver();
        Log.d(TAG, "onCreate:");
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        /* Configura la mappa prima dell'inflation*/
        Configuration.getInstance().load(mContext.getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext()));
        View v = inflater.inflate(R.layout.running_content, container, false);
        runSpeed = v.findViewById(R.id.speedTextView);
        runDistance = v.findViewById(R.id.distanceTextView);
        runButton = v.findViewById(R.id.buttonRun);
        runMap = v.findViewById(R.id.mapView);
        runButton.setOnClickListener(this);
        if (rvm.isRunning()) runButton.setImageResource(R.drawable.ic_walk_icon);
        setMap();
        return v;
    }

    private void setMap() {
        runMap.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        /* Per evitare known-bug di osm e bad performance */
        runMap.setMaxZoomLevel(MAX_ZOOM);
        runMap.setMultiTouchControls(true);
        runMap.setBuiltInZoomControls(false);
        /* Disabilita hw acceleration (potrebbe causare problemi di rendering) */
        runMap.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mapOverlayManager = runMap.getOverlayManager();
        controller = runMap.getController();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        checkRunning();
        Log.d(TAG, "onActivityCreated: ");
    }

    /**
     * Controlla se nell'attivazione precedente l'utente stava correndo
     * e sono stato killato
     */
    private void checkRunning() {
        FragmentActivity currentActivity = getActivity();
        if (currentActivity != null) {
            /* Teoricamente non può essere mai null perché viene chiamato in onActivityCreated */
            sp = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean isRunning = sp.getBoolean(KEY_ISRUNNING, false);
            Log.d(TAG, "checkRunning: ISRUNNING " + isRunning + " CONTEXT " + mContext.toString());
            rvm.setRunning(isRunning);
            if (rvm.isRunning()) {
                createFusedLocationClient();
                createLocationRequest();
                rvm.resumeRun(this, currentRunObserver);
                runButton.setImageResource(R.drawable.ic_walk_icon);
                Log.d(TAG, "checkRunning: isRunning");
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        runMap.onResume();
        Log.d(TAG, "onResume:");
    }

    @Override
    public void onPause() {
        super.onPause();
        runMap.onPause();
        sp.edit().putBoolean(KEY_ISRUNNING, rvm.isRunning()).apply();
        Log.d(TAG, "onPause: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        runMap.onDetach();
        Log.d(TAG, "onDestroy: DISTRUTTO");
    }

    /**
     * Se la view è runButton inizia o termina la corsa
     *
     * @param view view clickata
     */
    @Override
    public void onClick(View view) {
        if (view == runButton) {
            if (rvm.getPendingLocationIntent() == null) {
                Log.d(TAG, "onClick: NULLO PENDING LOCATION INTENT");
                Intent intent = new Intent(mContext.getApplicationContext(), LocationBroadcastReceiver.class);
                intent.setAction(LocationBroadcastReceiver.ACTION_UPDATES);
                PendingIntent pendingLocationIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), LOCATION_BROADCAST_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                rvm.setPendingLocationIntent(pendingLocationIntent);
            }
            if (!rvm.isRunning()) {
                initLocation();
                Log.d(TAG, "onClick: START RUN ");
            } else {
                clearCurrentRun();
                Log.d(TAG, "startService: STOP RUN");
            }
        }
    }

    /**
     * Starta una nuova corsa
     */
    private void startRun() {
        rvm.setRunning(true);
        rvm.setCurrentExternalKey(this, currentRunObserver);
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext,
                    getString(R.string.no_permission), Toast.LENGTH_LONG).show();
            return;
        }
        Intent taskIntent = new Intent(mContext.getApplicationContext(), TaskRemovedService.class);
        PendingIntent pendingLocationIntent = rvm.getPendingLocationIntent();
        taskIntent.putExtra("com.progetto.user.speedo.LOCINTENT", pendingLocationIntent);
        mContext.getApplicationContext().startService(taskIntent);
        // ERA QUI L'ERRORE
        FusedLocationProviderClient mFusedLocationClient = rvm.getmFusedLocationClient();
        mFusedLocationClient.requestLocationUpdates(rvm.getmLocationRequest(), pendingLocationIntent);
        runButton.setImageResource(R.drawable.ic_walk_icon);
    }
    /* Finisce la corsa corrente */

    /**
     * Termina la corsa corrente
     */
    private void clearCurrentRun() {
        rvm.setRunning(false);

        rvm.getmFusedLocationClient().removeLocationUpdates(rvm.getPendingLocationIntent());
        rvm.getExposedRunValues().removeObservers(this);
        mapOverlayManager.overlays().clear();
        controller.zoomTo(0d);
        runMap.invalidate();
        float run_distance = sp.getFloat(LocationBroadcastReceiver.KEY_NEW_DISTANCE, 0);
        float run_avgSpeed = sp.getFloat(LocationBroadcastReceiver.KEY_SPEED_AVG, 0);
        long run_end = System.currentTimeMillis();
        Intent jobIntent = new Intent(LocationUpdatesJobService.ACTION_END);
        Bundle tmpBundle = new Bundle();
        tmpBundle.putFloat(LocationBroadcastReceiver.KEY_NEW_DISTANCE, run_distance);
        tmpBundle.putFloat(LocationBroadcastReceiver.KEY_SPEED_AVG, run_avgSpeed);
        tmpBundle.putLong(LocationBroadcastReceiver.KEY_NEW_TIME, run_end);
        jobIntent.putExtra(LocationBroadcastReceiver.KEY_NEW_VALUES, tmpBundle);
        Context applicationContext = mContext.getApplicationContext();
        LocationUpdatesJobService.enqueueWork(applicationContext, LocationUpdatesJobService.class, LocationUpdatesJobService.JOB_ID, jobIntent);
        NotificationManager notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(LocationBroadcastReceiver.NOTIFICATION_ID);
        }
        runButton.setImageResource(R.drawable.ic_run_icon);
        runDistance.setText(R.string.distance_hint);
        runSpeed.setText(R.string.speed_hint);
    }

    /* Inizializza tutte le funzionalità per poter ricevere la location */
    private void initLocation() {
        createFusedLocationClient();
        createLocationRequest();
        checkPreRequisites();
        checkLocationPermission();
    }

    private void createFusedLocationClient() {
        if (rvm.getmFusedLocationClient() == null) {
            rvm.setmFusedLocationClient(LocationServices.getFusedLocationProviderClient(mContext));
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        Log.d(TAG, "onDetach: ");
    }

    /* Crea la richiesta di geo localizzazione */
    private void createLocationRequest() {
        if (rvm.getmLocationRequest() == null) {
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            // TODO Abilita il minimo
            mLocationRequest.setSmallestDisplacement(0.1f);
            rvm.setmLocationRequest(mLocationRequest);
        }
    }

    /**
     * Controlla se il GPS è abilitato,
     * se non lo è allora cerca di mostrare un alert per abilitarlo
     */
    private void checkPreRequisites() {
        Log.d(TAG, "checkPreRequisites: ");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(rvm.getmLocationRequest());
        SettingsClient client = LocationServices.getSettingsClient(mContext);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // TODO [DEBUG]
                Log.d(TAG, "onSuccess: checkPreRequisites");
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        /* Mostra una possibile soluzione all'errore */
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        /* Così chiama onActivityResult del fragment */
                        startIntentSenderForResult(resolvable.getResolution().getIntentSender(),
                                MainActivity.REQUEST_CHECK_SETTINGS_ID,
                                null, 0,
                                0, 0, null);
                        Log.d(TAG, "onFailure: RESOLVABLE 2");
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignoro l'errore
                    }
                }
            }
        });
    }

    /**
     * Controlla se ha i permessi per accedere alla locazione (FINE_LOCATION),
     * se non ha i permessi li richiede.
     */
    private void checkLocationPermission() {
        Log.d(TAG, "checkLocationPermission:");
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            /* Non controllo shouldShowRequestPermissionRationale() */
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MainActivity.ACCESS_FINE_LOCATION_PERMISSION_ID);
            Log.d(TAG, "checkLocationPermission: RICHIESTA PERMESSI 1");
        } else {
            startRun();
            Log.d(TAG, "checkLocationPermission: PERMESSI LOCATION GRANTED");
        }
    }

    /* Gestisce il risultato della richiesta dei permessi di locazione */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && requestCode == MainActivity.ACCESS_FINE_LOCATION_PERMISSION_ID &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRun();
            Log.d(TAG, "onRequestPermissionsResult: GRANTED");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.REQUEST_CHECK_SETTINGS_ID: {
                switch (resultCode) {
                    case AppCompatActivity.RESULT_OK:
                        // TODO [DEBUG]
                        Log.d(TAG, "onActivityResult: OK");
                        break;
                    case AppCompatActivity.RESULT_CANCELED:
                        Log.d(TAG, "onActivityResult: CANC");
                        Toast.makeText(mContext,
                                R.string.no_geoloc,
                                Toast.LENGTH_LONG).show();
                        break;
                }
                break;
            }
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}

