package com.progetto.user.speedo;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

/**
 * Mostra la mappa di una corsa all'interno d un
 * dialog fragment che è creato a partire da un
 * AlertDialog.
 */
public class CustomMapDialog extends DialogFragment {

    private static final String TAG = "CUSTOM-MAP-DIALOG";
    private static final String KEY_MAP_UPDATED_INFO = "com.progetto.user.speedo.MAP_UPDATED_INFO";
    private MapUpdatedInfo item;
    private MapView map;

    public static CustomMapDialog newInstance() {
        return new CustomMapDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            /* Restore delle precedenti info se lo smartphone è stato ruotato */
            MapUpdatedInfo restoredItem = savedInstanceState.getParcelable(KEY_MAP_UPDATED_INFO);
            if (restoredItem != null) {
                Log.d(TAG, "onCreateDialog: RESTORED");
                item = restoredItem;
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        if (activity == null) return super.onCreateDialog(savedInstanceState);
        Configuration.getInstance().load(activity.getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()));
        LayoutInflater inflater = activity.getLayoutInflater();
        View v = inflater.inflate(R.layout.map_alert_content, null);
        map = v.findViewById(R.id.mapStatsView);
        map.onResume();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);
        map.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        if (item == null) return builder.create();
        Polyline currentPolyLine = item.getNewPolyline();
        BoundingBox currentBB = item.getBoundingBox();
        if (currentBB == null || currentPolyLine == null) return builder.create();
        CustomMapDialog.updateMap(item, currentPolyLine, currentBB, activity, map);
        builder.setIcon(R.drawable.ic_map_icon);
        builder.setTitle(DateFormat.format(RunFragment.DATE_FORMATTER, item.getDate(0)));
        builder.setView(v);
        return builder.create();
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        map.onPause();
        map.onDetach();
        Log.d(TAG, "onDismiss: ");
    }

    public void setData(MapUpdatedInfo item) {
        this.item = item;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_MAP_UPDATED_INFO, item);
        Log.d(TAG, "onSaveInstanceState: ");
    }

    /**
     * Aggiorna la mappa passata come parametro con le nuove
     * informazioni.
     *
     * @param item            oggetto con le nuove informazioni
     * @param currentPolyLine polyline da aggiungere
     * @param currentBB       bounding box corrente
     * @param context         context del chiamante
     * @param map             mappa del chiamanate
     */
    public static void updateMap(MapUpdatedInfo item, Polyline currentPolyLine, BoundingBox currentBB, Context context, MapView map) {
        OverlayManager mapOverlayManager = map.getOverlayManager();
        IMapController controller = map.getController();
        double latSpan = currentBB.getLatitudeSpan();
        double lngSpan = currentBB.getLongitudeSpan();
        Log.d(TAG, "onPostExecute: LAT SPAN " + currentBB.getLatitudeSpan() + " , LNG SPAN " + currentBB.getLongitudeSpan());
        ArrayList<GeoPoint> points = currentPolyLine.getPoints();
        Drawable map_icon = context.getResources().getDrawable(R.drawable.ic_marker);
        for (int i = 0; i < points.size(); i++) {
            GeoPoint g = points.get(i);
            Marker measureMark = new Marker(map);
            measureMark.setIcon(map_icon);
            measureMark.setFlat(true);
            measureMark.setPosition(g);
            measureMark.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            measureMark.setTitle(String.valueOf(
                    DateFormat.format(RunFragment.DATE_FORMATTER, item.getDate(i))));
            measureMark.setSnippet(context.getString(R.string.run_notification, item.getDistance(i)));
            measureMark.setSubDescription(context.getString(R.string.map_info_speed, item.getSpeed(i), item.getSpeedAvg(i)));
            mapOverlayManager.add(measureMark);
        }
        mapOverlayManager.add(currentPolyLine);
        Log.d(TAG, "updateMap: SIZE " + points.size());
        if (points.size() == 1) {
            Log.d(TAG, "onChanged: AAAA MAX ZOOM");
            controller.zoomTo(RunFragment.MAX_ZOOM);
            controller.animateTo(points.get(0));
        } else {
            controller.zoomToSpan(latSpan, lngSpan);
            map.zoomToBoundingBox(currentBB, true, 5);
            controller.setCenter(currentBB.getCenterWithDateLine());
        }
    }

}
