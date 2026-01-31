package fr.ynryo.ouestcefdpdetram;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.Call;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.VehicleData;
import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;

public class VehicleDetailsActivity {
    private final int COLOR_GREEN = Color.rgb(15, 150, 40);
    private MainActivity context;
    private BottomSheetDialog bottomSheetDialog;

    public VehicleDetailsActivity(MainActivity context) {
        this.context = context;
    }

    public void init(MarkerData data) {
        bottomSheetDialog = new BottomSheetDialog(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.vehicule_details, null);
        bottomSheetDialog.setContentView(view);

        // Header express
        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);
        tvLigne.setText(data.getLineNumber());
        tvLigne.setBackgroundColor(Color.parseColor(data.getFillColor() != null ? data.getFillColor() : "#424242"));
        tvLigne.setTextColor(Color.parseColor(data.getColor() != null ? data.getColor() : "#FFFFFF"));

        view.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        view.findViewById(R.id.scrollStops).setVisibility(View.INVISIBLE);
        bottomSheetDialog.show();

        context.getFetcher().fetchVehicleStopsInfo(data, new FetchingManager.OnVehicleDetailsListener() {
            @Override
            public void onDetailsReceived(VehicleData details) {
                view.findViewById(R.id.loader).setVisibility(View.GONE);
                showVehicleDetails(details, view);

                if (details.getNetworkId() == 0) return;

                context.getFetcher().fetchNetworkData(details.getNetworkId(), new FetchingManager.OnNetworkDataListener() {
                    @Override
                    public void onDetailsReceived(NetworkData data) {
                        ImageView ivLogo = view.findViewById(R.id.ivNetworkLogo);
                        ivLogo.setColorFilter(Color.BLACK);

                        Glide.with(context)
                                .as(PictureDrawable.class)
                                .load(data.getLogoHref().toString())
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .into(ivLogo);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w("VehicleDetailsActivity", "Erreur lors de la récuperation du logo");
                    }
                });
            }

            @Override
            public void onError(String error) {
                view.findViewById(R.id.loader).setVisibility(View.GONE);
                TextView tvDest = view.findViewById(R.id.tvDestination);
                tvDest.setText("Erreur de réseau");
            }
        });
    }

    private void showVehicleDetails(VehicleData details, View view) {
        LinearLayout stopsContainer = view.findViewById(R.id.stopsContainer);
        stopsContainer.removeAllViews();

        TextView tvDestination = view.findViewById(R.id.tvDestination);
        tvDestination.setText(details.getDestination());

        if (details.getCalls() != null) {
            for (Call stop : details.getCalls()) {
                stopsContainer.addView(createRow(stop));
            }
        }
        view.findViewById(R.id.scrollStops).setVisibility(View.VISIBLE);
    }

    private View createRow(Call stop) {
        LinearLayout.LayoutParams paramsMRight = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        // row create
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // 1 nom de l'arrêt
        TextView tvStopName = new TextView(context);
        tvStopName.setText(stop.getStopName());
        tvStopName.setTextColor(Color.BLACK);
        tvStopName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // 2 icône de montée/descente (calls flags)
        ImageView inOutIcon = new ImageView(context);
        List<String> flags = stop.getFlags();
        if (flags != null && flags.contains("NO_PICKUP")) {
            inOutIcon.setImageResource(R.drawable.logout_24px);
        } else if (flags != null && flags.contains("NO_DROP_OFF")) {
            inOutIcon.setImageResource(R.drawable.login_24px);
        }
        inOutIcon.setPadding(8, 0, 0, 0);
        inOutIcon.setColorFilter(Color.BLACK);

        // 3 espace vide flexible (Spacer)
        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));

        // 4 icône expected time
        ImageView expectedTimeIcon = new ImageView(context);
        boolean isExpectedTime = stop.getExpectedTime() != null;
        if (isExpectedTime) {
            expectedTimeIcon.setImageResource(R.drawable.sensors_24px);
            expectedTimeIcon.setPadding(0, 0, 8, 0);
            expectedTimeIcon.setColorFilter(COLOR_GREEN);
        }

        TextView tvDelay = new TextView(context);
        tvDelay.setTypeface(null, Typeface.BOLD);
        if (stop.getExpectedTime() != null && stop.getAimedTime() != null) {
            try {
                ZonedDateTime expected = ZonedDateTime.parse(stop.getExpectedTime());
                ZonedDateTime aimed = ZonedDateTime.parse(stop.getAimedTime());

                long diff = ChronoUnit.MINUTES.between(aimed, expected);
                Log.w("delay", String.valueOf(diff));

                if (diff > 0) {
                    tvDelay.setText("Retard de " + diff + " min");
                    tvDelay.setTextColor(Color.RED);
                } else if (diff < 0) {
                    tvDelay.setText(diff + " min");
                    tvDelay.setTextColor(Color.BLUE);
                }
                tvDelay.setPadding(8, 0, 0, 0);
            } catch (Exception e) {
                Log.e("VehicleDetailsActivity", "Erreur calcul retard", e);
            }
        }


        // 5 heure
        TextView tvStopTime = new TextView(context);
        formatStopAndSetTime(tvStopTime, stop, isExpectedTime);
        tvStopTime.setTypeface(null, Typeface.BOLD);

        // row build
        row.addView(tvStopName);
        row.addView(inOutIcon);
        row.addView(spacer);
        row.addView(expectedTimeIcon);
        row.addView(tvStopTime);
        row.addView(tvDelay, paramsMRight);
        return row;
    }

    private void formatStopAndSetTime(TextView textView, Call stop, boolean isExpected) {
        try {
            String rawTime = isExpected ? stop.getExpectedTime() : stop.getAimedTime();
            if (rawTime != null) {
                ZonedDateTime zdt = ZonedDateTime.parse(rawTime);
                String formatted = zdt.format(DateTimeFormatter.ofPattern("HH:mm"));
                textView.setText(formatted);
                textView.setTextColor(isExpected ? COLOR_GREEN : Color.DKGRAY);
            } else {
                textView.setText("??:??");
                textView.setTextColor(Color.RED);
            }
        } catch (Exception e) {
            textView.setText("??:??");
        }
    }
}