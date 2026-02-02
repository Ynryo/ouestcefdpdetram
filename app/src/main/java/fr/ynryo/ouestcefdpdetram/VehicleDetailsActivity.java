package fr.ynryo.ouestcefdpdetram;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.Call;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.VehicleData;

public class VehicleDetailsActivity {
    private final int COLOR_GREEN = Color.rgb(15, 150, 40);
    private final int COLOR_ORANGE = Color.rgb(224, 159, 7);
    private final MainActivity context;

    public VehicleDetailsActivity(MainActivity context) {
        this.context = context;
    }

    public void init(MarkerData markerData) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.vehicule_details, null);
        bottomSheetDialog.setContentView(view);

        // Header express
        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);
        if (markerData.getId().contains("SNCF")) {
            tvLigne.setText(String.valueOf(markerData.getVehicleNumber() != null ? markerData.getVehicleNumber() : "0"));
        } else {
            tvLigne.setText(String.valueOf(markerData.getLineNumber() != null ? markerData.getLineNumber() : "0"));
        }
        tvLigne.setBackgroundColor(Color.parseColor(markerData.getFillColor() != null ? markerData.getFillColor() : "#424242"));
        tvLigne.setTextColor(Color.parseColor(markerData.getColor() != null ? markerData.getColor() : "#FFFFFF"));

        view.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        view.findViewById(R.id.scrollStops).setVisibility(View.INVISIBLE);
        bottomSheetDialog.show();

        context.getFetcher().fetchVehicleStopsInfo(markerData, new FetchingManager.OnVehicleDetailsListener() {
            @Override
            public void onDetailsReceived(VehicleData details) {
                view.findViewById(R.id.loader).setVisibility(View.GONE);
                showVehicleDetails(details, view);

                if (details.getNetworkId() == 0) return;

                context.getFetcher().fetchNetworkData(details.getNetworkId(), new FetchingManager.OnNetworkDataListener() {
                    @Override
                    public void onDetailsReceived(NetworkData nData) {
                        ImageView ivLogo = view.findViewById(R.id.ivNetworkLogo);
                        ivLogo.setAdjustViewBounds(true);
                        ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);

                        Glide.with(context)
                                .as(PictureDrawable.class)
                                .load(nData.getLogoHref().toString())
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
        tvDestination.setSingleLine(true);
        tvDestination.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvDestination.setMarqueeRepeatLimit(-1);
        tvDestination.setHorizontallyScrolling(true);
        tvDestination.setSelected(true);

        if (details.getCalls() != null) {
            for (Call stop : details.getCalls()) {
                stopsContainer.addView(createRow(stop));
            }
        } else {
            TextView tvDefault = new TextView(context);
            tvDefault.setText("Aucune donnée");
            tvDefault.setTextColor(Color.BLACK);
            tvDefault.setPadding(0, 0, 0, 16);
            stopsContainer.addView(tvDefault);
        }
        view.findViewById(R.id.scrollStops).setVisibility(View.VISIBLE);
    }

    private View createRow(Call stop) {
        // row create
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        //nom de l'arrêt
        TextView tvStopName = getStopName(stop);

        //bloc de droite
        LinearLayout llRight = new LinearLayout(context);
        llRight.setOrientation(LinearLayout.HORIZONTAL);
        llRight.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END);
        llRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f));


        //icône expected time
        ImageView expectedTimeIcon = new ImageView(context);
        boolean isExpectedTime = stop.getExpectedTime() != null;
        if (isExpectedTime) {
            expectedTimeIcon.setImageResource(R.drawable.sensors_24px);
            expectedTimeIcon.setPadding(0, 0, 8, 0);
            expectedTimeIcon.setColorFilter(COLOR_GREEN);
        }

        //delay
        TextView tvDelay = getDelay(stop);

        //heure
        TextView tvStopTime = new TextView(context);
        formatStopAndSetTime(tvStopTime, stop, isExpectedTime);
        tvStopTime.setTypeface(null, Typeface.BOLD);

        llRight.addView(expectedTimeIcon);
        llRight.addView(tvStopTime);
        llRight.addView(tvDelay);

        // row build
        row.addView(tvStopName);
        row.addView(llRight);
        return row;
    }

    @NonNull
    private TextView getStopName(Call stop) {
        TextView tvStopName = new TextView(context);
        tvStopName.setTextColor(Color.BLACK);
        tvStopName.setText(stop.getStopName());
        tvStopName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(stop.getStopName());

        List<String> flags = stop.getFlags();
        int iconRes = 0;
        if (flags != null && flags.contains("NO_PICKUP")) {
            iconRes = R.drawable.logout_24px;
        } else if (flags != null && flags.contains("NO_DROP_OFF")) {
            iconRes = R.drawable.login_24px;
        }

        if (iconRes != 0) {
            builder.append("  ");
            Drawable drawable = ContextCompat.getDrawable(context, iconRes);
            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN));
                int size = (int) (tvStopName.getTextSize() * 1.2f);
                drawable.setBounds(0, 0, size, size);
                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                builder.setSpan(imageSpan, builder.length() - 1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        tvStopName.setText(builder);

        tvStopName.setSingleLine(true);
        tvStopName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvStopName.setMarqueeRepeatLimit(-1);
        tvStopName.setHorizontallyScrolling(true);
        tvStopName.setSelected(true);
        return tvStopName;
    }

    @NonNull
    private TextView getDelay(Call stop) {
        TextView tvDelay = new TextView(context);
        tvDelay.setTypeface(null, Typeface.BOLD);
        if (stop.getExpectedTime() != null && stop.getAimedTime() != null) {
            try {
                ZonedDateTime expected = ZonedDateTime.parse(stop.getExpectedTime());
                ZonedDateTime aimed = ZonedDateTime.parse(stop.getAimedTime());

                long diff = ChronoUnit.MINUTES.between(aimed, expected);

                if (diff > 0) {
                    tvDelay.setText("Retard de " + diff + " min");
                    tvDelay.setTextColor(Color.RED);
                } else if (diff < 0) {
                    tvDelay.setText("Avance de " + Math.abs(diff) + " min");
                    tvDelay.setTextColor(COLOR_ORANGE);
                }
                tvDelay.setPadding(8, 0, 0, 0);
            } catch (Exception e) {
                Log.e("VehicleDetailsActivity", "Erreur calcul retard", e);
            }
        }
        return tvDelay;
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