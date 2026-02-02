package fr.ynryo.ouestcefdpdetram;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkersList;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworksList;
import fr.ynryo.ouestcefdpdetram.apiResponses.region.RegionsList;
import fr.ynryo.ouestcefdpdetram.apiResponses.route.RouteData;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.VehicleData;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NaolibApiService {
    @GET("vehicle-journeys/markers")
    Call<MarkersList> getVehicleMarkers(
            @Query("swLat") double swLat,
            @Query("swLon") double swLon,
            @Query("neLat") double neLat,
            @Query("neLon") double neLon
    );

    @GET("vehicle-journeys/{vehicleId}")
    Call<VehicleData> getVehicleDetails(
            @Path(value = "vehicleId", encoded = true) String vehicleId
    );

    @GET("regions")
    Call<RegionsList> getRegions();

    @GET("networks")
    Call<NetworksList> getNetworks();

    @GET("networks/{networkId}?withDetails=true")
    Call<NetworkData> getNetworkData(
            @Path(value = "networkId") int networkId
    );

    @GET("carto.php?action=train")
    Call<RouteData> getRouteLine(
            @Query("numero") int vehicleRoute
    );
}
