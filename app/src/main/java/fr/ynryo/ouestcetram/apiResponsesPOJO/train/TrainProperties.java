package fr.ynryo.ouestcetram.apiResponsesPOJO.train;

import androidx.annotation.NonNull;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TrainProperties {
    private final DateFormat formatter = new SimpleDateFormat("HH:mm");
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

    public Time getDebut() throws ParseException {
        return new Time(formatter.parse(debut).getTime());
    }

    public Time getFin() throws ParseException {
        return new Time(formatter.parse(fin).getTime());
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

    public Time getStopTime() throws ParseException {
        long timeDiff = getFin().getTime() - getDebut().getTime();
        return new Time(timeDiff);
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
