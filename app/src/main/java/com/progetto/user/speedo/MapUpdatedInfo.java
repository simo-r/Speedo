package com.progetto.user.speedo;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Contiene le informazioni da mostrare all'interno della mappa.
 * E' parcellabile perché in CustomMapDialog non viene usato alcun
 * View model e quindi il salvataggio va fatto manualmente.
 */
public class MapUpdatedInfo implements Parcelable {

    private Polyline polyline;

    private final BoundingBox boundingBox;

    private final List<Date> dates;

    private final List<Float> speeds;

    private final List<Float> distance;

    private final List<Float> speedAvg;

    private List<GeoPoint> points;


    MapUpdatedInfo() {
        boundingBox = new BoundingBox();
        polyline = new Polyline();
        dates = new ArrayList<>();
        speeds = new ArrayList<>();
        distance = new ArrayList<>();
        speedAvg = new ArrayList<>();
        points = new ArrayList<>();
    }


    public Polyline getPolyline() {
        return polyline;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void addSpeed(float speed) {
        speeds.add(speed);
    }

    public void addDate(long time) {
        dates.add(new Date(time));
    }

    public Date getDate(int i) {
        return dates.get(i);
    }

    public float getSpeed(int i) {
        return speeds.get(i);
    }

    public void addDistance(float dist) {
        distance.add(dist);
    }

    public void addSpeedAvg(float savg) {
        speedAvg.add(savg);
    }

    public float getDistance(int i) {
        return distance.get(i);
    }

    public float getSpeedAvg(int i) {
        return speedAvg.get(i);
    }

    public void setGeoPoints(List<GeoPoint> geoPoints) {
        if (geoPoints == null || geoPoints.size() == 0) {
            polyline = null;
        } else {
            polyline.setWidth(10);
            polyline.setColor(Color.GREEN);
            polyline.setPoints(geoPoints);
        }

    }


    public void setBounds(double maxLat, double maxLong, double minLat, double minLong) {
        boundingBox.set(maxLat, maxLong, minLat, minLong);
    }

    public void setPoints(List<GeoPoint> points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return "MapUpdatedInfo{" +
                "polyline=" + polyline +
                ", boundingBox=" + boundingBox +
                ", dates=" + dates +
                ", speeds=" + speeds +
                ", distance=" + distance +
                ", speedAvg=" + speedAvg +
                '}';
    }

    public Polyline getNewPolyline() {
        Polyline polyline = new Polyline();
        polyline.setWidth(10);
        polyline.setColor(Color.GREEN);
        polyline.setPoints(points);
        return polyline;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeList(dates);
        parcel.writeList(speeds);
        parcel.writeList(distance);
        parcel.writeList(speedAvg);
        parcel.writeList(points);
        parcel.writeParcelable(boundingBox, 0);
    }


    public static final Parcelable.Creator<MapUpdatedInfo> CREATOR
            = new Parcelable.Creator<MapUpdatedInfo>() {
        public MapUpdatedInfo createFromParcel(Parcel in) {
            return new MapUpdatedInfo(in);
        }

        public MapUpdatedInfo[] newArray(int size) {
            return new MapUpdatedInfo[size];
        }
    };

    private MapUpdatedInfo(Parcel in) {
        this();
        in.readList(dates, dates.getClass().getClassLoader());
        in.readList(speeds, speeds.getClass().getClassLoader());
        in.readList(distance, distance.getClass().getClassLoader());
        in.readList(speedAvg, speedAvg.getClass().getClassLoader());
        in.readList(points, points.getClass().getClassLoader());
        setGeoPoints(points);
    }

}
