package fr.ynryo.ouestcefdpdetram;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NaolibApiService {
    @GET("markers")
    Call<List<MarkerData>> getVehicleMarkers(
            @Query("swLat") double swLat,
            @Query("swLon") double swLon,
            @Query("neLat") double neLat,
            @Query("neLon") double neLon
    );
}