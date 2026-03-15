package fr.ynryo.ouestcefdpdetram;

import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.markers.MarkersList;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.region.RegionData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.train.TrainData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.vehicle.VehicleData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.version.VersionResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
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

    @GET("carto.php?action=train")
    Call<TrainData> getVehicleDetails(
            @Query("numero") int vehicleNumber
    );

    @GET("regions")
    Call<List<RegionData>> getRegions();

    @GET("networks")
    Call<List<NetworkData>> getNetworks();

    @GET("networks/{networkId}?withDetails=true")
    Call<NetworkData> getNetworkData(
            @Path(value = "networkId") int networkId
    );

    @GET("carto.php?action=train")
    Call<VehicleData> getRouteLine(
            @Query("numero") int vehicleRoute
    );

    @GET("version/latest")
    Call<VersionResponse> getLatestVersion();
}
