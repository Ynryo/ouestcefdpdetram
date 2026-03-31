package fr.ynryo.ouestcetram.apiResponsesPOJO.markers;

import androidx.annotation.NonNull;

public class MarkerPosition {
    private double latitude;
    private double longitude;
    private float bearing;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getBearing() {
        return bearing;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerPosition{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", bearing=" + bearing +
                '}';
    }
}
