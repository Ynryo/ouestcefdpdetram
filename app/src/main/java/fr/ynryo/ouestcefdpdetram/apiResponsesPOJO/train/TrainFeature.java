package fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.train;

import androidx.annotation.NonNull;

public class TrainFeature {
    private String type;
    private TrainGeometry geometry;
    private TrainProperties properties;

    public String getType() {
        return type;
    }

    public TrainGeometry getRouteGeometry() {
        return geometry;
    }

    public TrainProperties getProperties() {
        return properties;
    }

    @NonNull
    @Override
    public String toString() {
        return "TrainFeature{" +
                "type='" + type + '\'' +
                ", geometry=" + geometry +
                ", properties=" + properties +
                '}';
    }
}
