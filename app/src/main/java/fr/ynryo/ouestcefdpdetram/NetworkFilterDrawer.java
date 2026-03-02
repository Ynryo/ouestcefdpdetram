package fr.ynryo.ouestcefdpdetram;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.region.RegionData;

public class NetworkFilterDrawer {
    public static final String TAG = "NetworkFilterDrawer";
    private final MainActivity context;
    private final SaveManager saveManager;
    private final Map<String, Boolean> filters = new HashMap<>(); //ref reseau <> isShowed ?
    private final List<MaterialSwitch> switches = new ArrayList<>();
    private boolean isBulkUpdate = false;
    private boolean isUpdatingMasterSwitch = false;

    public NetworkFilterDrawer(MainActivity context) {
        this.context = context;
        this.saveManager = new SaveManager(context);
    }

    public void open() {
        if (context == null) return;
        DrawerLayout drawerLayout = context.findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void populateNetworks(List<RegionData> regions, List<NetworkData> networks) {
        if (context == null) return;
        
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
                public void onMarkersReceived(List<MarkerData> markers) {
                    context.getMarkerArtist().showMarkers(markers);
                }

                @Override
                public void onError(String error) {
                    Log.e("MainActivity", "Erreur lors de la récupération des données markers" + error);
                }
            });
        });
        networksContainer.addView(allShowRow);


        // Grouper les réseaux par région
        Map<Integer, List<NetworkData>> networksByRegion = new HashMap<>();
        Map<Integer, RegionData> regionMap = new HashMap<>();

        // Créer une map des régions par ID
        regions.add(new RegionData(0, "National"));
        for (RegionData region : regions) {
            Log.d(TAG, "Region: " + region.getName() + " with ID: " + region.getId());
            regionMap.put(region.getId(), region);
        }

        // Grouper les réseaux par région
        for (NetworkData network : networks) {
            int regionId = network.getRegionId();
//            if (regionId == 0) continue;
            if (!networksByRegion.containsKey(regionId)) {
                Log.d("NetworkFilterDrawer", "Ajouté à la map: " + network.getName() + " pour la région: " + regionMap.get(regionId).getName() + " avec l'ID: " + regionId);
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
                        public void onMarkersReceived(List<MarkerData> markers) {
                            context.getMarkerArtist().showMarkers(markers);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("NetworkFilterDrawer", "Erreur markers: " + error);
                        }
                    });
                });

                regionNetworksContainer.addView(row);
            }
        }
    }

    public boolean isNetworkVisible(String networkRef) {
        if (filters.isEmpty()) return true;
        Boolean networkVisibility = filters.get(networkRef);
        return networkVisibility != null && networkVisibility;
    }
}