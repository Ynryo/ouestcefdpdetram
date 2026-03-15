package fr.ynryo.ouestcefdpdetram.GenericMarkerDatas;

public enum MarkerType {
    CT_TRAIN("Train"),
    BT_VEHICLE("Bus/Tram");

    private final String displayName;

    MarkerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Détermine le type en fonction de l'ID du marqueur.
     * Si l'ID contient "SNCF", c'est un train.
     *
     * @param markerId L'ID du marqueur
     * @return CT_TRAIN si l'ID contient "SNCF", sinon BT_VEHICLE
     */
    public static MarkerType fromMarkerId(String markerId) {
        if (markerId != null && markerId.contains("SNCF")) {
            return CT_TRAIN;
        }
        return BT_VEHICLE;
    }
}