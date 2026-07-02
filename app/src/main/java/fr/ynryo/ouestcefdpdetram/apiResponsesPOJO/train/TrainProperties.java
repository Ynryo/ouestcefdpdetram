package fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.train;

import androidx.annotation.NonNull;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class TrainProperties {
    private String uic;
    private String debut;
    private String fin;
    private int etape;
    private int arret;
    private String code;
    private String ch;
    private String localite;
    private String pk;
    private String ligne;
    private String distance;
    private String offset;
    private int min = 0;
    private String origine;

    public String getUic() {
        return uic;
    }

    public LocalTime getDebut() {
        return LocalTime.parse(debut);
    }

    public LocalTime getFin() {
        return LocalTime.parse(fin);
    }

    public int getEtape() {
        return etape;
    }

    public boolean isStop() {
        return arret == 1;
    }

    public String getCode() {
        return code;
    }

    public String getCh() {
        return ch;
    }

    public String getLocalite() {
        return localite;
    }

    public String getPk() {
        return pk;
    }

    public String getLigne() {
        return ligne;
    }

    public String getDistance() {
        return distance;
    }

    public String getOffset() {
        return offset;
    }

    public Long getStopTime() {
        LocalTime debut = getDebut();
        LocalTime fin = getFin();

        if (debut != null || fin != null) return null;

        return ChronoUnit.MINUTES.between(getFin(), getDebut());
    }

    public int getDelay() {
        return min;
    }

    public String getOrigine() {
        return origine;
    }

    public boolean isRoute() {
        return origine != null;
    }

    @NonNull
    @Override
    public String toString() {
        return "TrainProperties{" +
                "uic='" + uic + '\'' +
                ", debut='" + debut + '\'' +
                ", fin='" + fin + '\'' +
                ", etape=" + etape +
                ", arret=" + arret +
                ", code='" + code + '\'' +
                ", ch='" + ch + '\'' +
                ", localite='" + localite + '\'' +
                ", pk='" + pk + '\'' +
                ", ligne='" + ligne + '\'' +
                ", distance='" + distance + '\'' +
                ", offset='" + offset + '\'' +
                ", origine='" + origine + '\'' +
                '}';
    }
}
