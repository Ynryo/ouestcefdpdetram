package fr.ynryo.ouestcetram.apiResponsesPOJO.train;

import androidx.annotation.NonNull;

public class TrainGeometry {
    private String type;
    private Object coordinates;

    public String getType() {
        return type;
    }

    public Object getCoordinates() {
        return coordinates;
    }

    @NonNull
    @Override
    public String toString() {
        return "TrainGeometry{" +
                "type='" + type + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }
}
