package fr.ynryo.ouestcefdpdetram;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Map;

public class SaveManager {
    private static final String PREFS_NAME = "ouestcefdpdetramPrefs";
    private static final String KEY_PREFIX_NETWORK = "network_";
    private final SharedPreferences prefs;

    public SaveManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveNetworkFilter(String networkRef, boolean isVisible) {
        String key = KEY_PREFIX_NETWORK + networkRef;
        prefs.edit().putBoolean(key, isVisible).apply();
    }

    public boolean loadNetworkFilter(String networkRef) {
        String key = KEY_PREFIX_NETWORK + networkRef;
        return prefs.getBoolean(key, true);
    }

    public void saveAllNetworksVisibility(ArrayList<String> networkRefs, boolean isVisible) {
        for (String networkRef : networkRefs) {
            String key = KEY_PREFIX_NETWORK + networkRef;
            prefs.edit().putBoolean(key, isVisible).apply();
        }
    }

    public boolean isAllNetworksVisible() {
        if (prefs.getAll().isEmpty()) return true;
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith(KEY_PREFIX_NETWORK) && entry.getValue().equals(false)) {
                return false;
            }
        }
        return true;
    }
}