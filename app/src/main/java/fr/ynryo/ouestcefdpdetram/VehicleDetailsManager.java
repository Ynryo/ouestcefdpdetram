package fr.ynryo.ouestcefdpdetram;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PictureDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.VehicleData;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.VehicleStop;

public class VehicleDetailsManager {
    private static final int COLOR_GREEN = Color.rgb(15, 150, 40);
    private static final int COLOR_ORANGE = Color.rgb(224, 159, 7);
    private static final int COLOR_DARK_ORANGE = Color.rgb(224, 112, 7);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final MainActivity context;
    private BottomSheetDialog bottomSheetDialog;
    private String vehicleId;

    public VehicleDetailsManager(MainActivity context) {
        WeakReference<MainActivity> contextRef = new WeakReference<>(context);
        this.context = contextRef.get();
    }

    public void open(MarkerData markerData) {
        if (this.context == null) return;
        
        close();

        bottomSheetDialog = new BottomSheetDialog(context);
        vehicleId = markerData.getId();
        View view = LayoutInflater.from(context).inflate(R.layout.vehicule_details, null);
        bottomSheetDialog.setContentView(view);
        //temp le tps de trouver une solus
        bottomSheetDialog.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                // On récupère le conteneur interne du BottomSheet
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                int peekHeight = (int) (screenHeight * 0.5);
                behavior.setPeekHeight(peekHeight);

                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);
        if (markerData.getId().startsWith("SNCF")) {
            tvLigne.setText(markerData.getVehicleNumber() != null ? markerData.getVehicleNumber() : "ND");
        } else {
            tvLigne.setText(markerData.getLineNumber() != null ? markerData.getLineNumber() : "ND");
        }
        int fillColor = Color.parseColor(markerData.getFillColor() != null ? markerData.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerData.getColor() != null ? markerData.getColor() : "#FFFFFF");
        tvLigne.setBackgroundColor(fillColor);
        tvLigne.setTextColor(textColor);

        ProgressBar loader = view.findViewById(R.id.loader);
        loader.setVisibility(View.VISIBLE);
        loader.setIndeterminateTintList(ColorStateList.valueOf(fillColor));

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
                        URI imgURI = nData.getLogoHref();
                        if (imgURI == null) return;
                        ImageView ivLogo = view.findViewById(R.id.ivNetworkLogo);
                        ivLogo.setBackgroundResource(R.color.surface_light);
                        ivLogo.setAdjustViewBounds(true);
                        ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);

                        Glide.with(context)
                                .as(PictureDrawable.class)
                                .load(imgURI.toString())
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .override(100, 100)
                                .into(ivLogo);
                    }

                    @Override
                    public void onError(String error) {
                        Log.w("VehicleDetailsManager", "Erreur lors de la récuperation du logo");
                    }
                });
            }

            @Override
            public void onError(String error) {
                view.findViewById(R.id.loader).setVisibility(View.GONE);
                TextView tvDest = view.findViewById(R.id.tvDestination);
                tvDest.setText(R.string.network_error);
            }
        });
    }

    public void close() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
            bottomSheetDialog = null;
        }
    }

    private void showVehicleDetails(VehicleData details, View view) {
        context.getFollowManager().setFollowButton(view.findViewById(R.id.followButton), details.getId());
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
            for (VehicleStop stop : details.getCalls()) {
                stopsContainer.addView(createRow(stop));
            }
        } else {
            TextView tvDefault = new TextView(context);
            tvDefault.setText(R.string.no_data);
            tvDefault.setTextColor(MaterialColors.getColor(tvDefault, com.google.android.material.R.attr.colorOnSurface));
            tvDefault.setPadding(0, 0, 0, 16);
            stopsContainer.addView(tvDefault);
        }
        view.findViewById(R.id.scrollStops).setVisibility(View.VISIBLE);
    }

    private LinearLayout createRow(VehicleStop stop) {
        //stop row
        LinearLayout stopRow = new LinearLayout(context);
        stopRow.setOrientation(LinearLayout.HORIZONTAL);
        stopRow.setPadding(0, 16, 0, 16);
        stopRow.setGravity(Gravity.CENTER_VERTICAL);

        //left part
        LinearLayout llLeft = new LinearLayout(context);
        llLeft.setOrientation(LinearLayout.HORIZONTAL);
        llLeft.setGravity(Gravity.CENTER_VERTICAL);
        llLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvPlatform = getPlatform(stop);
        llLeft.addView(tvPlatform);
        if (!tvPlatform.getText().toString().isEmpty()) {
            llLeft.addView(getSpacer());
        }
        llLeft.addView(getStopName(stop));

        //right part
        LinearLayout llRight = new LinearLayout(context);
        llRight.setOrientation(LinearLayout.HORIZONTAL);
        llRight.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        llRight.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0f));

        llRight.addView(getTime(stop));
        llRight.addView(getDelay(stop));

        //assembling
        stopRow.addView(llLeft);
        stopRow.addView(llRight);
        return stopRow;
    }

    @NonNull
    private TextView getPlatform(VehicleStop stop) {
        TextView tvPlatformName = new TextView(context);
        String platformName = stop.getPlatformName();
        if (platformName == null) {
            return tvPlatformName;
        }
        tvPlatformName.setText(platformName);
        tvPlatformName.setTextColor(Color.BLACK);
        tvPlatformName.setPadding(0, 0, 8, 0);

        GradientDrawable gdPlatformName = new GradientDrawable();
        gdPlatformName.setShape(GradientDrawable.RECTANGLE);
        gdPlatformName.setPadding(8, 4, 8, 4);
        gdPlatformName.setStroke(2, Color.BLACK);
        gdPlatformName.setCornerRadius(10);
        tvPlatformName.setBackground(gdPlatformName);

        return tvPlatformName;
    }

    @NonNull
    private TextView getStopName(VehicleStop stop) {
        TextView tvStopName = new TextView(context);
        tvStopName.setTextColor(MaterialColors.getColor(tvStopName, com.google.android.material.R.attr.colorOnSurface));
        tvStopName.setText(stop.getStopName());

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
                drawable.setColorFilter(new PorterDuffColorFilter(
                        MaterialColors.getColor(tvStopName, com.google.android.material.R.attr.colorOnSurface),
                        PorterDuff.Mode.SRC_IN)
                );
                int size = (int) (tvStopName.getTextSize() * 1.2f);
                drawable.setBounds(0, 0, size, size);
                ImageSpan imageSpan = new ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM);
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
    private TextView getDelay(VehicleStop stop) {
        TextView tvDelay = new TextView(context);
        if (stop.getExpectedTime() != null && stop.getAimedTime() != null) {
            try {
                ZonedDateTime expected = ZonedDateTime.parse(stop.getExpectedTime());
                ZonedDateTime aimed = ZonedDateTime.parse(stop.getAimedTime());

                long diff = ChronoUnit.MINUTES.between(aimed, expected);

                if (diff == 0) {
                    return tvDelay;
                }

                if (diff > 0) {
                    String delayText = "Retard de " + diff + " min";
                    tvDelay.setText(delayText);
                    if (diff <= 5) {
                        tvDelay.setTextColor(COLOR_DARK_ORANGE);
                    } else {
                        tvDelay.setTextColor(Color.RED);
                    }
                } else {
                    String delayText = "Avance de " + Math.abs(diff) + " min";
                    tvDelay.setText(delayText);
                    tvDelay.setTextColor(COLOR_ORANGE);
                }
                tvDelay.setPadding(8, 0, 0, 0);
            } catch (Exception e) {
                Log.e("VehicleDetailsManager", "Erreur calcul retard", e);
            }
        }
        return tvDelay;
    }

    @NonNull
    private LinearLayout getTime(VehicleStop stop) {
        LinearLayout llTime = new LinearLayout(context);
        llTime.setOrientation(LinearLayout.HORIZONTAL);
        llTime.setGravity(Gravity.CENTER_VERTICAL);

        boolean isExpectedTime = stop.getExpectedTime() != null;
        ImageView ivExpectedTimeIcon = new ImageView(context);
        if (isExpectedTime) {
            ivExpectedTimeIcon.setImageResource(R.drawable.sensors_24px);
            ivExpectedTimeIcon.setPadding(0, 0, 8, 0);
            ivExpectedTimeIcon.setColorFilter(COLOR_GREEN);
        }

        llTime.addView(ivExpectedTimeIcon);
        llTime.addView(formatStopTime(stop, isExpectedTime));
        return llTime;
    }

    private TextView formatStopTime(VehicleStop stop, boolean isExpected) {
        TextView tvTime = new TextView(context);
        tvTime.setTypeface(null, Typeface.BOLD);
        try {
            String rawTime = isExpected ? stop.getExpectedTime() : stop.getAimedTime();
            if (rawTime == null) {
                tvTime.setText("??:??");
                tvTime.setTextColor(Color.RED);
                return tvTime;
            }

            ZonedDateTime zdt = ZonedDateTime.parse(rawTime);
            String formatted = zdt.format(TIME_FORMATTER);
            tvTime.setText(formatted);
            tvTime.setTextColor(isExpected ? COLOR_GREEN : MaterialColors.getColor(tvTime, com.google.android.material.R.attr.colorOnSurface));
        } catch (Exception e) {
            tvTime.setText("??:??");
        }
        return tvTime;
    }


    @NonNull
    private View getSpacer() {
        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(16, 0));
        return spacer;
    }

    public String getCurrentVehicleId() {
        return vehicleId;
    }
}
