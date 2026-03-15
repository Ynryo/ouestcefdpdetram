package fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.train;

import androidx.annotation.NonNull;

import java.util.List;

public class TrainData {
    private String type;
    private List<TrainFeature> features;

    public String getType() {
        return type;
    }

    public List<TrainFeature> getRouteFeatures() {
        return features;
    }

    @NonNull
    @Override
    public String toString() {
        return "TrainData{" +
                "type='" + type + '\'' +
                ", features=" + features +
                '}';
    }
}
