package fr.ynryo.ouestcefdpdetram;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;;

public class VehicleDetailsActivity {
    private final int COLOR_GREEN = Color.rgb(15, 150, 40);
    private MainActivity context;
    private BottomSheetDialog bottomSheetDialog;

    public VehicleDetailsActivity(MainActivity context) {
        this.context = context;
    }

    public void init(MarkerData data) {
        //afficher la fenêtre
        bottomSheetDialog = new BottomSheetDialog(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.vehicule_details, null);
        bottomSheetDialog.setContentView(view);

        //set header text et color
        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);
        tvLigne.setText(String.valueOf(data.getLineNumber()));
        String fillColorString = data.getFillColor();
        tvLigne.setBackgroundColor(Color.parseColor(fillColorString != null && !fillColorString.isEmpty() ? fillColorString : "#424242"));
        String textColorString = data.getColor();
        tvLigne.setTextColor(Color.parseColor(textColorString != null && !textColorString.isEmpty() ? textColorString : "#FFFFFF"));

        //affichage du loader
        view.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        view.findViewById(R.id.scrollStops).setVisibility(View.INVISIBLE);
        bottomSheetDialog.show();

        //get data
        FetchingManager fetcher = context.getFetcher();
        VehicleDetails vehicleDetails = fetcher.fetchVehicleStopsInfo(data, view);
//        showVehicleDetails(vehicleDetails, view);
    }

    private void showVehicleDetails(VehicleDetails details, View view) {
        LinearLayout stopsContainer = view.findViewById(R.id.stopsContainer);
        stopsContainer.removeAllViews();
        View scrollStops = view.findViewById(R.id.scrollStops);
        TextView tvDestination = view.findViewById(R.id.tvDestination);
        tvDestination.setText(details.getDestination());
        if (details.getCalls() != null) {
            for (Call stop : details.getCalls()) {
                LinearLayout.LayoutParams paramsMRight = new LinearLayout.LayoutParams(0, -1);
                // row create
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 16, 0, 16);
                row.setGravity(Gravity.CENTER_VERTICAL);

                // 1 nom de l'arrêt
                TextView tvStopName = new TextView(context);
                tvStopName.setText(stop.getStopName());
                tvStopName.setTextColor(Color.BLACK);
                tvStopName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                // 2 icône de montée/descente (calls flags)
                ImageView inOutIcon = new ImageView(context);
                if (stop.getFlags().contains("NO_PICKUP")) {
                    inOutIcon.setImageResource(R.drawable.logout_24px);
                } else if (stop.getFlags().contains("NO_DROP_OFF")) {
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
                if (stop.getExpectedTime() != null && stop.getAimedTime() != null) {
                    try {
                        ZonedDateTime expected = ZonedDateTime.parse(stop.getExpectedTime());
                        ZonedDateTime aimed = ZonedDateTime.parse(stop.getAimedTime());

                        long diff = ChronoUnit.MINUTES.between(aimed, expected);

                        if (diff > 0) {
                            // Retard : on affiche "+X min"
                            tvDelay.setText("+" + diff + " min");
                            tvDelay.setTextColor(Color.RED);
                        } else if (diff < 0) {
                            // En avance : on affiche "-X min"
                            tvDelay.setText(diff + " min");
                            tvDelay.setTextColor(Color.BLUE);
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Erreur calcul retard", e);
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
                row.addView(tvDelay, paramsMRight);
                row.addView(expectedTimeIcon);
                row.addView(tvStopTime);

                stopsContainer.addView(row);
            }
        }
        // On affiche enfin le contenu
        scrollStops.setVisibility(View.VISIBLE);
    }

    private void formatStopAndSetTime(TextView textView, fr.ynryo.ouestcefdpdetram.Call stop, boolean isExpected) {
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
