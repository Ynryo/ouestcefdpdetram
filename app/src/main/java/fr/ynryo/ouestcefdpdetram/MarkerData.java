package fr.ynryo.ouestcefdpdetram;

import com.google.gson.annotations.SerializedName;

public class MarkerData {
    @SerializedName("lat")
    private double lat;

    @SerializedName("lon")
    private double lon;

    @SerializedName("title")
    private String title;

    // Getters
    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getTitle() {
        return title;
    }
}