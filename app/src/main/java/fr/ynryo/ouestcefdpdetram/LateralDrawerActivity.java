package fr.ynryo.ouestcefdpdetram;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.region.RegionData;
import fr.ynryo.ouestcefdpdetram.managers.Favorite;
import fr.ynryo.ouestcefdpdetram.managers.FetchingManager;
import fr.ynryo.ouestcefdpdetram.managers.SaveManager;

public class LateralDrawerActivity {
    private static final String TAG = "LateralDrawerActivity";
    private final MainActivity context;
    private final SaveManager saveManager;
    private final Map<String, Boolean> filters = new HashMap<>(); //ref réseau <> isShowed ?
    private final List<MaterialSwitch> switches = new ArrayList<>();
    private boolean isBulkUpdate = false;
    private boolean isUpdatingMasterSwitch = false;

    private View mainMenuContainer;
    private View filtersPageContainer;
    private View favoritePageContainer;
    private View creditsPageContainer;


    public LateralDrawerActivity(MainActivity context, SaveManager saveManager) {
        this.context = context;
        this.saveManager = saveManager;
        initMenu();
    }

    private void initMenu() {
        mainMenuContainer = context.findViewById(R.id.main_menu_container);
        filtersPageContainer = context.findViewById(R.id.filters_page_container);
        favoritePageContainer = context.findViewById(R.id.favorite_page_container);
        creditsPageContainer = context.findViewById(R.id.credits_page_container);

        View btnFilters = context.findViewById(R.id.btn_menu_filters);
        View btnFavorites = context.findViewById(R.id.btn_menu_favorites);
        View btnCredits = context.findViewById(R.id.btn_menu_credits);

        View btnBackFilters = context.findViewById(R.id.btn_back_to_menu_filters);
        View btnBackFavorites = context.findViewById(R.id.btn_back_to_menu_favorites);
        View btnBackCredits = context.findViewById(R.id.btn_back_to_menu_credits);

        if (btnFilters != null) btnFilters.setOnClickListener(v -> showFiltersPage());
        if (btnFavorites != null) btnFavorites.setOnClickListener(v -> showFavoritePage());
        if (btnCredits != null) btnCredits.setOnClickListener(v -> showCreditsPage());
        
        if (btnBackFilters != null) btnBackFilters.setOnClickListener(v -> showMainMenu());
        if (btnBackFavorites != null) btnBackFavorites.setOnClickListener(v -> showMainMenu());
        if (btnBackCredits != null) btnBackCredits.setOnClickListener(v -> showMainMenu());
    }

    private void showMainMenu() {
        if (mainMenuContainer != null) mainMenuContainer.setVisibility(View.VISIBLE);
        if (filtersPageContainer != null) filtersPageContainer.setVisibility(View.GONE);
        if (favoritePageContainer != null) favoritePageContainer.setVisibility(View.GONE);
        if (creditsPageContainer != null) creditsPageContainer.setVisibility(View.GONE);
    }

    private void showFiltersPage() {
        if (mainMenuContainer != null) mainMenuContainer.setVisibility(View.GONE);
        if (filtersPageContainer != null) filtersPageContainer.setVisibility(View.VISIBLE);
    }

    private void showFavoritePage() {
        if (mainMenuContainer != null) mainMenuContainer.setVisibility(View.GONE);
        if (favoritePageContainer != null) favoritePageContainer.setVisibility(View.VISIBLE);
        populateFavoriteLines(); // Call to populate favorite lines
    }

    private void showCreditsPage() {
        if (mainMenuContainer != null) mainMenuContainer.setVisibility(View.GONE);
        if (creditsPageContainer != null) creditsPageContainer.setVisibility(View.VISIBLE);
    }

    public void open() {
        DrawerLayout drawerLayout = context.findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            showMainMenu();
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void populateNetworks(List<RegionData> regions, List<NetworkData> networks) {
        if (saveManager == null) return;

        LinearLayout networksContainer = context.findViewById(R.id.networks_container);
        if (networksContainer == null) return;
        
        networksContainer.removeAllViews();
        filters.clear();
        switches.clear();

        View allShowRow = LayoutInflater.from(context).inflate(R.layout.item_network, networksContainer, false);
        TextView tvShowName = allShowRow.findViewById(R.id.network_name);
        MaterialSwitch msShowToggle = allShowRow.findViewById(R.id.network_switch);

        tvShowName.setText(R.string.show_all);
        msShowToggle.setChecked(saveManager.isAllNetworksVisible());
        msShowToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingMasterSwitch) return;
            isBulkUpdate = true;

            ArrayList<String> networksName = new ArrayList<>();
            for (MaterialSwitch ms : switches) {
                ms.setChecked(isChecked);
                String networkRef = ms.getTag().toString();
                filters.put(networkRef, isChecked);
                networksName.add(networkRef);
            }

            isBulkUpdate = false;
            saveManager.saveAllNetworksVisibility(networksName, isChecked);

            context.getFetcher().fetchMarkers(new FetchingManager.OnMarkersListener() {
                @Override
                public void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList) {
                    context.getMarkerArtist().showMarkers(markerDataStandardizedList);
                }

                @Override
                public void onErrorMarkersListener(String error) {
                    Log.e(TAG, "Erreur lors de la récupération des données markers" + error);
                }
            });
        });
        networksContainer.addView(allShowRow);


        // Grouper les réseaux par région
        Map<Integer, List<NetworkData>> networksByRegion = new HashMap<>();
        Map<Integer, RegionData> regionMap = new HashMap<>();

        // Créer une map des régions par ID
        boolean hasNational = false;
        for (RegionData r : regions) {
            if (r.getId() == 0) {
                hasNational = true;
                break;
            }
        }
        if (!hasNational) {
            regions.add(new RegionData(0, "National"));
        }
        
        for (RegionData region : regions) {
            Log.d(TAG, "Region: " + region.getName() + " with ID: " + region.getId());
            regionMap.put(region.getId(), region);
        }

        // Grouper les réseaux par région
        for (NetworkData network : networks) {
            int regionId = network.getRegionId();
            if (!networksByRegion.containsKey(regionId)) {
                String regionName = regionMap.containsKey(regionId) ? regionMap.get(regionId).getName() : "Inconnue";
                Log.d(TAG, "Ajouté à la map: " + network.getName() + " pour la région: " + regionName + " avec l'ID: " + regionId);
                networksByRegion.put(regionId, new ArrayList<>());
            }
            networksByRegion.get(regionId).add(network);
        }

        // Ajouter chaque région et ses réseaux
        for (RegionData region : regions) {
            List<NetworkData> regionNetworks = networksByRegion.get(region.getId());
            if (regionNetworks == null || regionNetworks.isEmpty()) {
                continue; // Pas de réseaux dans cette région
            }

            // Header de région (pliable)
            View regionHeader = LayoutInflater.from(context).inflate(R.layout.item_region_header, networksContainer, false);
            TextView tvRegionTitle = regionHeader.findViewById(R.id.region_title);
            ImageView ivArrow = regionHeader.findViewById(R.id.region_arrow);

            tvRegionTitle.setText(region.getName());

            // Container pour les réseaux de cette région
            LinearLayout regionNetworksContainer = new LinearLayout(context);
            regionNetworksContainer.setOrientation(LinearLayout.VERTICAL);
            regionNetworksContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            final boolean[] isExpanded = {false};
            regionNetworksContainer.setVisibility(View.GONE);
            regionHeader.setOnClickListener(v -> {
                isExpanded[0] = !isExpanded[0];
                regionNetworksContainer.setVisibility(isExpanded[0] ? View.VISIBLE : View.GONE);
                ivArrow.setRotation(isExpanded[0] ? 0 : 180);
            });

            networksContainer.addView(regionHeader);
            networksContainer.addView(regionNetworksContainer);

            // Ajouter les réseaux de cette région
            for (NetworkData network : regionNetworks) {
                String networkRef = network.getRef();
                boolean isVisible = saveManager.loadNetworkFilter(networkRef);
                filters.put(networkRef, isVisible);

                View row = LayoutInflater.from(context).inflate(R.layout.item_network, regionNetworksContainer, false);
                TextView tvName = row.findViewById(R.id.network_name);
                TextView tvCity = row.findViewById(R.id.network_city);
                MaterialSwitch visibilitySwitch = row.findViewById(R.id.network_switch);

                tvName.setText(network.getName());
                tvCity.setText(network.getAuthority());
                tvCity.setSingleLine(true);
                tvCity.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                tvCity.setMarqueeRepeatLimit(-1);
                tvCity.setHorizontallyScrolling(true);
                tvCity.setSelected(true);

                visibilitySwitch.setChecked(isVisible);
                visibilitySwitch.setTag(networkRef);
                switches.add(visibilitySwitch);

                visibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isBulkUpdate) return;

                    filters.put(networkRef, isChecked);
                    saveManager.saveNetworkFilter(networkRef, isChecked);

                    isUpdatingMasterSwitch = true;

                    if (!isChecked) {
                        msShowToggle.setChecked(false);
                    } else {
                        boolean areAllChecked = true;
                        for (MaterialSwitch s : switches) {
                            if (!s.isChecked()) {
                                areAllChecked = false;
                                break;
                            }
                        }
                        msShowToggle.setChecked(areAllChecked);
                    }
                    isUpdatingMasterSwitch = false;

                    context.getFetcher().fetchMarkers(new FetchingManager.OnMarkersListener() {
                        @Override
                        public void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList) {
                            context.getMarkerArtist().showMarkers(markerDataStandardizedList);
                        }

                        @Override
                        public void onErrorMarkersListener(String error) {
                            Log.e("LateralDrawerActivity", "Erreur markers: " + error);
                        }
                    });
                });

                regionNetworksContainer.addView(row);
            }
        }
    }

    private void populateFavoriteLines() {
        LinearLayout favoritesContainer = context.findViewById(R.id.favorites_container);
        if (favoritesContainer == null) return;

        favoritesContainer.removeAllViews();

        List<Favorite> favoriteLines = saveManager.loadFavoriteLines();

        // si y'a pas de favoris
        if (favoriteLines.isEmpty()) {
            TextView noFavoritesText = new TextView(context);
            noFavoritesText.setText(R.string.no_favorites_message);
            noFavoritesText.setTextSize(16);
            noFavoritesText.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            noFavoritesText.setPadding(16, 16, 16, 16);
            favoritesContainer.addView(noFavoritesText);
            return;
        }

        for (Favorite f : favoriteLines) {
            String lineIdStr = String.valueOf(f.getLigneId());
            
            View lineHeaderView = LayoutInflater.from(context).inflate(R.layout.item_favorite_line_header, favoritesContainer, false);
            TextView tvLineNumber = lineHeaderView.findViewById(R.id.tv_line_number);
            TextView tvDestinationHeader = lineHeaderView.findViewById(R.id.tv_destination);

            int fillColor = Color.parseColor(f.getFillColor() != null ? f.getFillColor() : "#424242");
            int textColor = Color.parseColor(f.getTextColor() != null ? f.getTextColor() : "#FFFFFF");

            tvLineNumber.setText(f.getLineText());
            tvLineNumber.setBackgroundColor(fillColor);
            tvLineNumber.setTextColor(textColor);
            tvDestinationHeader.setText(f.getDestination());

            favoritesContainer.addView(lineHeaderView);

            LinearLayout lineVehiclesContainer = new LinearLayout(context);
            lineVehiclesContainer.setOrientation(LinearLayout.VERTICAL);
            lineVehiclesContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            favoritesContainer.addView(lineVehiclesContainer);

            // fetch markers for favorite line
            context.getFetcher().fetchMarkers(lineIdStr, new FetchingManager.OnMarkersListener() {
                @Override
                public void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList) {
                    // filtrer uniquement les véhicules de la ligne
                    for (MarkerDataStandardized vehicle : markerDataStandardizedList) {
                        context.getFetcher().fetchVehicleStopsInfo(vehicle, new FetchingManager.OnVehicleDetailsListener() {
                            @Override
                            public void onResponseVehicleDetailsListener(MarkerDataStandardized markerDetails) {
                                if (f.getDestination().equals(markerDetails.getDestination())) {
                                    View vehicleView = LayoutInflater.from(context).inflate(R.layout.item_favorite_vehicle, lineVehiclesContainer, false);
                                    ImageView ivMarker = vehicleView.findViewById(R.id.iv_marker);
                                    TextView tvNextStop = vehicleView.findViewById(R.id.tv_next_stop);
                                    TextView tvTime = vehicleView.findViewById(R.id.tv_time);

                                    ivMarker.setImageBitmap(context.getMarkerArtist().createCustomMarker(markerDetails, 0, false));
                                    tvNextStop.setText(markerDetails.getNextStop() != null ? markerDetails.getNextStop().getStopName() : context.getString(R.string.no_data));
                                    tvTime.setText(markerDetails.getNextStop() != null && markerDetails.getNextStop().getDepartureTime() != null ? markerDetails.getNextStop().getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm")) : context.getString(R.string.no_data));
                                    lineVehiclesContainer.addView(vehicleView);
                                }
                            }

                            @Override
                            public void onErrorVehicleDetailsListener(String error) {
                                Log.e(TAG, "Error fetching details for favorite " + lineIdStr + ": " + error);
                            }
                        });
                    }
                }

                @Override
                public void onErrorMarkersListener(String error) {
                    Log.e(TAG, "Error fetching markers for favorite " + lineIdStr + ": " + error);
                }
            });
        }
    }

    public boolean isNetworkVisible(String networkRef) {
        if (filters.isEmpty()) return true;
        Boolean networkVisibility = filters.get(networkRef);
        return networkVisibility != null && networkVisibility;
    }
}