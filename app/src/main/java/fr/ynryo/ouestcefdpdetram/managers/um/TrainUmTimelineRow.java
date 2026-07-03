package fr.ynryo.ouestcefdpdetram.managers.um;

import fr.ynryo.ouestcefdpdetram.genericMarkerDatas.MarkerDataStop;

public class TrainUmTimelineRow {
    private MarkerDataStop stopA;
    private MarkerDataStop stopB;
    private MarkerDataStop commonStop;
    private boolean isSplit = false;

    // Constructeur pour un arrêt commun (UM)
    public TrainUmTimelineRow(MarkerDataStop commonStop) {
        this.commonStop = commonStop;
        this.isSplit = false;
    }

    // Constructeur pour deux arrêts distincts côte à côte
    public TrainUmTimelineRow(MarkerDataStop stopA, MarkerDataStop stopB) {
        this.stopA = stopA;
        this.stopB = stopB;
        this.isSplit = true;
    }

    public MarkerDataStop getStopA() {
        return stopA;
    }

    public MarkerDataStop getStopB() {
        return stopB;
    }

    public MarkerDataStop getCommonStop() {
        return commonStop;
    }

    public boolean isSplit() {
        return isSplit;
    }
}