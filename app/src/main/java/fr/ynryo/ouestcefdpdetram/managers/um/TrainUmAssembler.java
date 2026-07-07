package fr.ynryo.ouestcefdpdetram.managers.um;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.ynryo.ouestcefdpdetram.genericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.genericMarkerDatas.MarkerDataStop;
import fr.ynryo.ouestcefdpdetram.genericMarkerDatas.MarkerType;

public class TrainUmAssembler {
    private final static String TAG = "TrainUmAssembler";

    /**
     * Builds a unified marker by combining data from two MarkerDataStandardized instances.
     * The unified marker includes a composite ID, average location, and combined metadata.
     *
     * @param trainA the first MarkerDataStandardized instance to be merged
     * @param trainB the second MarkerDataStandardized instance to be merged
     * @return a MarkerDataStandardized instance representing the unified marker created from the input instances
     */
    public static MarkerDataStandardized buildUmMarker(MarkerDataStandardized trainA, MarkerDataStandardized trainB) {
        MarkerDataStandardized um = new MarkerDataStandardized();
        String idA = trainA.getId();
        String idB = trainB.getId();
        um.setId(idA.compareTo(idB) <= 0 ? idA + "+" + idB : idB + "+" + idA);

        um.setMarkerType(MarkerType.TRAIN);
        um.setUmPair(trainA, trainB);

        um.setLatitude((trainA.getLatitude() + trainB.getLatitude()) / 2.0);
        um.setLongitude((trainA.getLongitude() + trainB.getLongitude()) / 2.0);
        um.setBearing((trainA.getBearing() + trainB.getBearing()) / 2.0f);
        um.setFillColor(trainA.getFillColor());
        um.setTextColor(trainA.getTextColor());
        um.setLineNumber(trainA.getLineNumber() + " et " + trainB.getLineNumber());
        um.setNetworkId(trainA.getNetworkId());
        um.setNetworkRef(trainA.getNetworkRef());

        return um;
    }


    /**
     * Assembles a list of timeline rows representing stops for a unified marker (UM),
     * combining and aligning stops from two associated MarkerDataStandardized instances.
     *
     * @param um the unified marker containing two associated MarkerDataStandardized instances, which
     *           represent two trains combined as a UM. The method uses these instances to retrieve and
     *           align their respective stop data.
     * @return a list of TrainUmTimelineRow objects, where each row represents either a common stop,
     *         a split stop (distinct stops from the two trains), or an aligned stop based on calculated
     *         chronological order.
     */
    public static List<TrainUmTimelineRow> assembleUmStops(MarkerDataStandardized um) {
        if (!um.isUm()) return new ArrayList<>();

        MarkerDataStandardized trainA = um.getUmA();
        MarkerDataStandardized trainB = um.getUmB();
        List<MarkerDataStop> stopsA = trainA.getStops() != null ? trainA.getStops() : new ArrayList<>();
        List<MarkerDataStop> stopsB = trainB.getStops() != null ? trainB.getStops() : new ArrayList<>();

        // 1. Trouver les arrêts communs pour délimiter les phases
        List<String> commonRefs = new ArrayList<>();
        Set<String> bRefs = new HashSet<>();
        for (MarkerDataStop b : stopsB) bRefs.add(b.getStopRef());
        for (MarkerDataStop a : stopsA) {
            if (bRefs.contains(a.getStopRef())) commonRefs.add(a.getStopRef());
        }

        List<TrainUmTimelineRow> displayRows = new ArrayList<>();

        if (commonRefs.isEmpty()) {
            // Cas extrême : ils ne se croisent jamais, on met tout côte à côte
            zipStopsSideBySide(stopsA, stopsB, 0, stopsA.size(), 0, stopsB.size(), displayRows);
            return displayRows;
        }

        String firstCommon = commonRefs.get(0);
        String lastCommon = commonRefs.get(commonRefs.size() - 1);

        // Index de parcours
        int indexA = 0, indexB = 0;

        // ==========================================
        // PHASE 1 : AVANT LE MERGE (Côte à côte)
        // ==========================================
        int endPhase1A = findStopIndex(stopsA, firstCommon);
        int endPhase1B = findStopIndex(stopsB, firstCommon);

        if (endPhase1A > 0 || endPhase1B > 0) {
            zipStopsSideBySide(stopsA, stopsB, indexA, endPhase1A, indexB, endPhase1B, displayRows);
            // Insertion de l'icône de MERGE
            displayRows.add(TrainUmTimelineRow.createMergeGraphic());
        }

        indexA = endPhase1A;
        indexB = endPhase1B;

        // ==========================================
        // PHASE 2 : TRAJET COMMUN (Fusionné)
        // ==========================================
        int endPhase2A = findStopIndex(stopsA, lastCommon) + 1;
        int endPhase2B = findStopIndex(stopsB, lastCommon) + 1;

        while (indexA < endPhase2A && indexB < endPhase2B) {
            // On prend l'arrêt de A, et on l'assigne à l'UM
            MarkerDataStop commonStop = new MarkerDataStop(stopsA.get(indexA));
            commonStop.setVehicle(um);
            displayRows.add(TrainUmTimelineRow.createCommonStop(commonStop));
            indexA++;
            indexB++;
        }

        // ==========================================
        // PHASE 3 : APRÈS LE SPLIT (Côte à côte)
        // ==========================================
        if (indexA < stopsA.size() || indexB < stopsB.size()) {
            // Insertion de l'icône de SPLIT
            displayRows.add(TrainUmTimelineRow.createSplitGraphic());
            zipStopsSideBySide(stopsA, stopsB, indexA, stopsA.size(), indexB, stopsB.size(), displayRows);
        }

        return displayRows;
    }

    // Fonction utilitaire pour trouver l'index d'un arrêt
    private static int findStopIndex(List<MarkerDataStop> stops, String stopRef) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getStopRef().equals(stopRef)) return i;
        }
        return stops.size();
    }

    // Fonction utilitaire pour aligner côte à côte les arrêts exclusifs
    private static void zipStopsSideBySide(List<MarkerDataStop> stopsA, List<MarkerDataStop> stopsB,
                                           int startA, int endA, int startB, int endB,
                                           List<TrainUmTimelineRow> displayRows) {
        int i = startA;
        int j = startB;
        while (i < endA || j < endB) {
            MarkerDataStop stopA = (i < endA) ? stopsA.get(i) : null;
            MarkerDataStop stopB = (j < endB) ? stopsB.get(j) : null;
            displayRows.add(TrainUmTimelineRow.createSideBySideStop(stopA, stopB));
            if (i < endA) i++;
            if (j < endB) j++;
        }
    }
    public static String getDestination(MarkerDataStandardized um) {
        if (!um.isUm()) return um.getDestination();

        String destA = um.getUmA().getDestination();
        String destB = um.getUmB().getDestination();

        if (destA != null && destA.equals(destB)) {
            return destA;
        } else {
            return destA + " / " + destB;
        }
    }
}
