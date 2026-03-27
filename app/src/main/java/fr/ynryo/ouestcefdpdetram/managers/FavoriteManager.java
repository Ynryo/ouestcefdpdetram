package fr.ynryo.ouestcefdpdetram.managers;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.MainActivity;
import fr.ynryo.ouestcefdpdetram.R;

/**
 * Classe gérant les lignes favorites
 */
public class FavoriteManager {
    private final static String TAG = "FavoriteManager";
    private final List<Favorite> favoriteLines = new ArrayList<>();
    private final MainActivity context;
    private final SaveManager saveManager;
    private MarkerDataStandardized showedVehicleId;
    private FloatingActionButton favoriteButton;

    public FavoriteManager(MainActivity context, SaveManager saveManager) {
        this.context = context;
        this.saveManager = saveManager;
        this.favoriteLines.clear();
        this.favoriteLines.addAll(saveManager != null ? saveManager.loadFavoriteLines() : new ArrayList<>());
    }

    public void toggleFavorite(MarkerDataStandardized mData) {
        if (mData == null) return;
        if (isFavorite(mData)) {
            removeFavorite(mData);
        } else {
            addFavorite(mData);
        }
    }

    public void addFavorite(MarkerDataStandardized mData) {
        if (isFavorite(mData)) return;
        this.showedVehicleId = mData;
        favoriteLines.add(new Favorite(mData.getLineId(), mData.getLineNumber(), mData.getDestination(), mData.getFillColor(), mData.getTextColor()));
        favoriteButton.setImageResource(R.drawable.icon_favorite_fill);
        saveManager.saveFavoriteLines(favoriteLines);
    }

    public void removeFavorite(MarkerDataStandardized mData) {
        if (!isFavorite(mData)) return;
        this.showedVehicleId = null;
        favoriteLines.removeIf(f -> f.getLigneId() == mData.getLineId() && f.getDestination().equals(mData.getDestination()));
        favoriteButton.setImageResource(R.drawable.icon_favorite);
        saveManager.saveFavoriteLines(favoriteLines);
    }

    public boolean isFavorite(MarkerDataStandardized mData) {
        if (mData == null) return false;
        for (Favorite f : favoriteLines) {
            if (f.getLigneId() == mData.getLineId() && f.getDestination().equals(mData.getDestination())) {
                return true;
            }
        }
        return false;
    }

    public List<Favorite> getFavoriteLines() {
        return favoriteLines;
    }

    public void setFavoriteButton(FloatingActionButton favoriteButton, MarkerDataStandardized mData) {
        this.favoriteButton = favoriteButton;
        if (this.favoriteButton == null) return;
        if (isFavorite(mData)) this.favoriteButton.setImageResource(R.drawable.icon_favorite_fill);
        this.favoriteButton.setOnClickListener(view -> toggleFavorite(mData));
    }
}
