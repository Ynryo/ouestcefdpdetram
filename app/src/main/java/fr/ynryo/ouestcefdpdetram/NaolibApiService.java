package fr.ynryo.ouestcefdpdetram;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NaolibApiService {
    @GET("markers")
    Call<VehicleJourneyResponse> getVehicleMarkers(
            @Query("swLat") double swLat,
            @Query("swLon") double swLon,
            @Query("neLat") double neLat,
            @Query("neLon") double neLon
    );

    @GET("{vehicleId}")
    Call<VehicleDetails> getVehicleDetails(
            @Path(value = "vehicleId", encoded = true) String vehicleId
    );

    @GET("{networkId}")
    Call<NetworkJourneyResponse> getNetworkInfomations(
            @Path(value = "networkId", encoded = true) int networkId
    );
}
