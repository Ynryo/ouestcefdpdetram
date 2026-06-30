package fr.ynryo.ouestcefdpdetram.genericMarkerDatas;

import java.util.ArrayList;
import java.util.List;

/**
 * Reçoit une liste de trains fraichement fetched et return une liste de trains nettoyée contenant des UMs (Unités Multiples)
 */
public class TrainUmDetector {
    private static final double UM_THRESHOLD_DEG = 0.0002;

    public TrainUmDetector() {
    }

    /**
     * Détecte les trains circulant en Unité Multiple (UM).
     *
     * @param markers La liste des trains récupérés.
     * @return umPairs Une liste de paires de trains couplés.
     */
    public static List<MarkerDataStandardized> filterUm(List<MarkerDataStandardized> markers) {
        List<MarkerDataStandardized> result = new ArrayList<>(markers);

        for (int i = 0; i < result.size(); i++) {
            MarkerDataStandardized a = result.get(i);
            if (!a.isTrain() || a.getBearing() == 0) continue; //bearing == 0 == gare

            for (int j = i + 1; j < result.size(); j++) {
                MarkerDataStandardized b = result.get(j);
                if (!b.isTrain() || b.getBearing() == 0) continue;

                if (areColocated(a, b)) {
                    result.set(i, buildUmMarker(a, b)); // remplace a par l'UM
                    result.remove(j); // supprime b
                    break; // a ne peut avoir qu'un seul partenaire
                }
            }
        }

        return result;
    }

    private static boolean areColocated(MarkerDataStandardized a, MarkerDataStandardized b) {
        return Math.abs(a.getLatitude() - b.getLatitude()) < UM_THRESHOLD_DEG && Math.abs(a.getLongitude() - b.getLongitude()) < UM_THRESHOLD_DEG;
    }

    private static MarkerDataStandardized buildUmMarker(MarkerDataStandardized trainA, MarkerDataStandardized trainB) {
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
        um.setNetworkRef(trainA.getNetworkRef());

        return um;
    }
}
