package fr.ynryo.ouestcefdpdetram;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.region.RegionData;

public class NetworkFilterDrawer {
    private final MainActivity context;
    private final SaveManager saveManager;
    private final Map<String, Boolean> filters = new HashMap<>(); //ref reseau <> isShowed ?
    private List<MaterialSwitch> switches = new ArrayList<>();
    private boolean isBulkUpdate = false;
    private boolean isUpdatingMasterSwitch = false;

    public NetworkFilterDrawer(MainActivity context) {
        WeakReference<MainActivity> contextRef = new WeakReference<>(context);
        this.context = contextRef.get();
        this.saveManager = new SaveManager(context);
    }

    public void open() {
        DrawerLayout drawerLayout = context.findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void populateNetworks(List<RegionData> regions, List<NetworkData> networks) {
        LinearLayout networksContainer = context.findViewById(R.id.networks_container);
        networksContainer.removeAllViews();
        filters.clear();
        switches.clear();

        View allShowRow = LayoutInflater.from(context).inflate(R.layout.item_network, networksContainer, false);
        TextView tvShowName = allShowRow.findViewById(R.id.network_name);
        MaterialSwitch msShowToggle = allShowRow.findViewById(R.id.network_switch);

        tvShowName.setText("Tout afficher");
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
                    context.showMarkers(markers);
                }

                @Override
                public void onError(String error) {
                    Log.e("MainActivity", "Erreur lors de la récupération des données markers" + error);
                }
            });
        });
        networksContainer.addView(allShowRow);


        for (NetworkData network : networks) {
            String networkRef = network.getRef();
            boolean isVisible = saveManager.loadNetworkFilter(networkRef);
            filters.put(networkRef, isVisible);

            View row = LayoutInflater.from(context).inflate(R.layout.item_network, networksContainer, false);
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
                        context.showMarkers(markers);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("MainActivity", "Erreur lors de la récupération des données markers" + error);
                    }
                });
            });

            networksContainer.addView(row);
        }
    }

    public boolean isNetworkVisible(String networkRef) {
        if (filters.isEmpty()) return true;
        Boolean networkVisibility = filters.get(networkRef);
        return networkVisibility != null && networkVisibility;
    }
}