package fr.ynryo.ouestcefdpdetram;

import java.util.List;

public class Call {
    private String aimedTime;
    private String expectedTime;
    private String stopRef;
    private String stopName;
    private int stopOrder;
    private String callStatus;
    private List<String> flags;

    public String getAimedTime() {
        return aimedTime;
    }

    public String getExpectedTime() {
        return expectedTime;
    }

    public String getStopRef() {
        return stopRef;
    }

    public String getStopName() {
        return stopName;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public String getCallStatus() {
        return callStatus;
    }

    public List<String> getFlags() {
        return flags;
    }
}
