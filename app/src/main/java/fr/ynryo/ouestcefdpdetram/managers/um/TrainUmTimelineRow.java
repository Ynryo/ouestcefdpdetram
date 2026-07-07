package fr.ynryo.ouestcefdpdetram.managers.um;

import fr.ynryo.ouestcefdpdetram.genericMarkerDatas.MarkerDataStop;

public class TrainUmTimelineRow {
    public TimelineRowType type;
    public MarkerDataStop stopA; // Utilisé comme arrêt commun si type == COMMON
    public MarkerDataStop stopB;

    private TrainUmTimelineRow(TimelineRowType type, MarkerDataStop stopA, MarkerDataStop stopB) {
        this.type = type;
        this.stopA = stopA;
        this.stopB = stopB;
    }

    public static TrainUmTimelineRow createCommonStop(MarkerDataStop stop) {
        return new TrainUmTimelineRow(TimelineRowType.COMMON, stop, null);
    }

    public static TrainUmTimelineRow createSideBySideStop(MarkerDataStop stopA, MarkerDataStop stopB) {
        return new TrainUmTimelineRow(TimelineRowType.SIDE_BY_SIDE, stopA, stopB);
    }

    public static TrainUmTimelineRow createMergeGraphic() {
        return new TrainUmTimelineRow(TimelineRowType.MERGE_GRAPHIC, null, null);
    }

    public static TrainUmTimelineRow createSplitGraphic() {
        return new TrainUmTimelineRow(TimelineRowType.SPLIT_GRAPHIC, null, null);
    }
}