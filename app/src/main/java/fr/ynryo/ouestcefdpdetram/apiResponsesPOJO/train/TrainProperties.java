package fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.train;

import androidx.annotation.NonNull;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TrainProperties {
    private DateFormat formatter = new SimpleDateFormat("HH:mm");
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

    public int getArret() {
        return arret;
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
                '}';
    }
}
