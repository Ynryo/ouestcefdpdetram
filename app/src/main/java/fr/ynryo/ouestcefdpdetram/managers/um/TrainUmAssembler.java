package fr.ynryo.ouestcefdpdetram.managers.um;

import java.util.ArrayList;
import java.util.List;

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
        if (!um.isUm()) return new ArrayList<>(); //si c'est pas um

        um.setDestination(getDestination(um));
        //TODO: afficher logo

        //get les US dans l'UM
        MarkerDataStandardized trainA = um.getUmA();
        MarkerDataStandardized trainB = um.getUmB();

        //get les stops des US
        List<MarkerDataStop> stopsA = trainA.getStops();
        List<MarkerDataStop> stopsB = trainB.getStops();

        List<TrainUmTimelineRow> timelineRows = new ArrayList<>();

        int i = 0, j = 0;

        while (i < stopsA.size() || j < stopsB.size()) {
            // Si le train B est terminé, on vide le train A
            if (i < stopsA.size() && j >= stopsB.size()) {
                stopsA.get(i).setVehicle(um);
                timelineRows.add(new TrainUmTimelineRow(stopsA.get(i), null));
                i++;
                continue;
            }
            // Si le train A est terminé, on vide le train B
            if (j < stopsB.size() && i >= stopsA.size()) {
                stopsB.get(j).setVehicle(um);
                timelineRows.add(new TrainUmTimelineRow(null, stopsB.get(j)));
                j++;
                continue;
            }

            MarkerDataStop stopA = stopsA.get(i);
            MarkerDataStop stopB = stopsB.get(j);

            // CAS 1 : C'est le même arrêt physique (UM parfait)
            if (stopA.getStopRef().equals(stopB.getStopRef())) {
                stopA.setVehicle(um); // Liaison globale à l'UM
                timelineRows.add(new TrainUmTimelineRow(stopA));
                i++;
                j++;
            }
            // CAS 2 : Arrêts différents (Divergence / Séparation)
            else {
                // On compare les horaires pour les aligner au mieux de haut en bas
                java.time.LocalTime timeA = stopA.getArrivalTime() != null ? stopA.getArrivalTime() : stopA.getDepartureTime();
                java.time.LocalTime timeB = stopB.getArrivalTime() != null ? stopB.getArrivalTime() : stopB.getDepartureTime();

                if (timeA != null && timeB != null) {
                    if (timeA.isBefore(timeB)) {
                        // L'arrêt A arrive en premier chronologiquement
                        stopA.setVehicle(um);
                        timelineRows.add(new TrainUmTimelineRow(stopA, null));
                        i++;
                    } else if (timeB.isBefore(timeA)) {
                        // L'arrêt B arrive en premier
                        stopB.setVehicle(um);
                        timelineRows.add(new TrainUmTimelineRow(null, stopB));
                        j++;
                    } else {
                        // Même heure mais arrêts différents (Branches parallèles au même moment)
                        stopA.setVehicle(um);
                        stopB.setVehicle(um);
                        timelineRows.add(new TrainUmTimelineRow(stopA, stopB));
                        i++;
                        j++;
                    }
                } else {
                    // Fallback si pas d'horaire : on les met côte à côte par défaut
                    stopA.setVehicle(um);
                    stopB.setVehicle(um);
                    timelineRows.add(new TrainUmTimelineRow(stopA, stopB));
                    i++;
                    j++;
                }
            }
        }

        return timelineRows;
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
