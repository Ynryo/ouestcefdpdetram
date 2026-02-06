package fr.ynryo.ouestcefdpdetram;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SaveManager {
    private static final String PREFS_NAME = "ouestcefdpdetramPrefs";
    private static final String KEY_PREFIX_NETWORK = "network_";
    private final SharedPreferences prefs;

    public SaveManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Sauvegarde l'état (visible/caché) d'un filtre réseau spécifique.
     * @param networkRef La référence unique du réseau (String).
     * @param isVisible true si le réseau doit être visible, false sinon.
     */
    public void saveNetworkFilter(String networkRef, boolean isVisible) {
        String key = KEY_PREFIX_NETWORK + networkRef;
        prefs.edit().putBoolean(key, isVisible).apply();
        Log.d("SaveManager", "Saved filter " + networkRef + ": " + isVisible);
    }

    /**
     * Charge l'état sauvegardé d'un filtre réseau.
     * @param networkRef La référence unique du réseau (String).
     * @return L'état sauvegardé (true si visible), ou true par défaut s'il n'y a pas de sauvegarde.
     */
    public boolean loadNetworkFilter(String networkRef) {
        String key = KEY_PREFIX_NETWORK + networkRef;
        // La valeur par défaut est TRUE, ce qui signifie que par défaut, tous les réseaux sont affichés
        // si aucune préférence n'est trouvée pour cet ID.
        return prefs.getBoolean(key, true);
    }


//    private void saveSliderPreferences() {
//        prefs.edit()
//                .putInt("avmMin", Math.round(avmSlider.getSliderFromValue()))
//                .putInt("avmMax", Math.round(avmSlider.getSliderToValue()))
//                .putInt("pretMin", Math.round(pretSlider.getSliderFromValue()))
//                .putInt("pretMax", Math.round(pretSlider.getSliderToValue()))
//                .putInt("partezMin", Math.round(partezSlider.getSliderFromValue()))
//                .putInt("partezMax", Math.round(partezSlider.getSliderToValue()))
//                .apply();
//    }
//
//
//    private void loadSliderPreferences() {
//        avmSlider.onUpdateValues(
//                prefs.getInt("avmMin", 20),
//                prefs.getInt("avmMax", 30)
//        );
//    }
}