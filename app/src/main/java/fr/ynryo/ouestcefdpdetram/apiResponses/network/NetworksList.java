package fr.ynryo.ouestcefdpdetram.apiResponses.network;

import androidx.annotation.NonNull;

import java.util.List;

public class NetworksList {
    private List<NetworkData> networks;

    public List<NetworkData> getNetworks() {
        return networks;
    }

    @NonNull
    @Override
    public String toString() {
        return "NetworksList{" +
                "networks=" + networks +
                '}';
    }
}
