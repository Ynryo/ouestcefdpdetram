package fr.ynryo.ouestcefdpdetram.apiResponses.vehicle;

import androidx.annotation.NonNull;

import java.util.List;

public class Call {
    private String aimedTime;
    private String expectedTime;
    private String stopRef;
    private String stopName;
    private int stopOrder;
    private String platformName;
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

    public String getPlatformName() {
        return platformName;
    }

    public String getCallStatus() {
        return callStatus;
    }

    public List<String> getFlags() {
        return flags;
    }

    @NonNull
    @Override
    public String toString() {
        return "Call{" +
                "aimedTime='" + aimedTime + '\'' +
                ", expectedTime='" + expectedTime + '\'' +
                ", stopRef='" + stopRef + '\'' +
                ", stopName='" + stopName + '\'' +
                ", stopOrder=" + stopOrder +
                ", callStatus='" + callStatus + '\'' +
                ", flags=" + flags +
                '}';
    }
}
