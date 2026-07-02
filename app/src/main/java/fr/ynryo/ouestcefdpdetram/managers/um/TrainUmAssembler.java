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
        um.setNetworkRef(trainA.getNetworkRef());

        return um;
    }


    /**
     * Assemble les arrêts des 2 trains. Les arrêts communs sont fusionnés pour s'afficher qu'une seule fois
     * et les arrêts non-communs ne sont pas fusionnés (mais triés) pour s'afficher pour chacun des trains
     * (une colonne pour chaque train).
     */
    public static void assembleUmStops(MarkerDataStandardized um) {
        if (!um.isUm()) {
            return;
        }
        MarkerDataStandardized trainA = um.getUmA();
        MarkerDataStandardized trainB = um.getUmB();

        if (trainA.getStops() == null || trainA.getStops().isEmpty() ||
                trainB.getStops() == null || trainB.getStops().isEmpty()) {
            return;
        }

        List<MarkerDataStop> stopsA = trainA.getStops();
        List<MarkerDataStop> stopsB = trainB.getStops();

        // 1. Identifier les références des arrêts communs (ils serviront de points de repère pour le tri)
        List<String> commonRefs = new ArrayList<>();
        Set<String> bRefs = new HashSet<>();
        for (MarkerDataStop stopB : stopsB) {
            bRefs.add(stopB.getStopRef());
        }
        for (MarkerDataStop stopA : stopsA) {
            if (bRefs.contains(stopA.getStopRef())) {
                commonRefs.add(stopA.getStopRef()); // Garde l'ordre chronologique naturel du trajet
            }
        }

        List<MarkerDataStop> mergedStops = new ArrayList<>();
        int i = 0;
        int j = 0;

        // 2. Construire la nouvelle timeline en s'arrêtant à chaque point de rencontre
        for (String commonRef : commonRefs) {

            // Ajouter les arrêts de la branche exclusive du train A
            while (i < stopsA.size() && !stopsA.get(i).getStopRef().equals(commonRef)) {
                MarkerDataStop exclusiveA = new MarkerDataStop(stopsA.get(i));
                exclusiveA.setVehicle(trainA); // Appartient uniquement au train A
                mergedStops.add(exclusiveA);
                i++;
            }

            // Ajouter les arrêts de la branche exclusive du train B
            while (j < stopsB.size() && !stopsB.get(j).getStopRef().equals(commonRef)) {
                MarkerDataStop exclusiveB = new MarkerDataStop(stopsB.get(j));
                exclusiveB.setVehicle(trainB); // Appartient uniquement au train B
                mergedStops.add(exclusiveB);
                j++;
            }

            // Ajouter l'arrêt commun (fusionné)
            if (i < stopsA.size() && j < stopsB.size()) {
                MarkerDataStop mergedStop = new MarkerDataStop(stopsA.get(i));
                mergedStop.setVehicle(um); // Appartient à l'UM entier (les deux colonnes)
                mergedStops.add(mergedStop);
                i++;
                j++;
            }
        }

        // 3. Ajouter les éventuels terminus divergents (après le tout dernier arrêt commun)
        while (i < stopsA.size()) {
            MarkerDataStop exclusiveA = new MarkerDataStop(stopsA.get(i));
            exclusiveA.setVehicle(trainA);
            mergedStops.add(exclusiveA);
            i++;
        }
        while (j < stopsB.size()) {
            MarkerDataStop exclusiveB = new MarkerDataStop(stopsB.get(j));
            exclusiveB.setVehicle(trainB);
            mergedStops.add(exclusiveB);
            j++;
        }

        um.setStops(mergedStops);
    }
}
