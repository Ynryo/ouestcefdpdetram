package fr.ynryo.ouestcefdpdetram;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PictureDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStop;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.network.NetworkData;

public class MarkerStopsDetailActivity {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String TAG = "MarkerStopsDetailActivity";
    private static final int COLOR_GREEN = Color.rgb(15, 150, 40);
    private final MainActivity context;
    private BottomSheetDialog bottomSheetDialog;
    private String vehicleId;

    public MarkerStopsDetailActivity(MainActivity context) {
        WeakReference<MainActivity> contextRef = new WeakReference<>(context);
        this.context = contextRef.get();
    }

    public void open(MarkerDataStandardized markerDataStandardized) {
        if (this.context == null) return;
        close();

        bottomSheetDialog = new BottomSheetDialog(context);
        vehicleId = markerDataStandardized.getId();

        View view = LayoutInflater.from(context).inflate(R.layout.vehicule_details, null);
        bottomSheetDialog.setContentView(view);
        setupBottomSheetAppearance(view);
        setupLineHeader(view, markerDataStandardized);
        setupLoader(view, markerDataStandardized);

        bottomSheetDialog.show();
        fetchVehicleData(markerDataStandardized, view);
    }

    public void close() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
            bottomSheetDialog = null;
        }
    }

    // ==================== SETUP ====================
    private void setupBottomSheetAppearance(View view) {
        bottomSheetDialog.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                int peekHeight = calculatePeekHeight();
                behavior.setPeekHeight(peekHeight);
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    private int calculatePeekHeight() {
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        return (int) (screenHeight * 0.5);
    }

    private void setupLineHeader(View view, MarkerDataStandardized markerDataStandardized) {
        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);

        String lineNumber = markerDataStandardized.getLineId();
        tvLigne.setText(lineNumber);

        int fillColor = Color.parseColor(markerDataStandardized.getFillColor() != null ? markerDataStandardized.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerDataStandardized.getTextColor() != null ? markerDataStandardized.getTextColor() : "#FFFFFF");

        tvLigne.setBackgroundColor(fillColor);
        tvLigne.setTextColor(textColor);
    }

    private void setupLoader(View view, MarkerDataStandardized markerDataStandardized) {
        ProgressBar loader = view.findViewById(R.id.loader);
        int fillColor = Color.parseColor(markerDataStandardized.getFillColor() != null ? markerDataStandardized.getFillColor() : "#424242");

        loader.setVisibility(View.VISIBLE);
        loader.setIndeterminateTintList(ColorStateList.valueOf(fillColor));

        view.findViewById(R.id.llStopsContent).setVisibility(View.INVISIBLE);
    }

    // ==================== DATA FETCHING ====================
    private void fetchVehicleData(MarkerDataStandardized markerDataStandardized, View view) {
        context.getFetcher().fetchVehicleStopsInfo(markerDataStandardized, new FetchingManager.OnVehicleDetailsListener() {
            @Override
            public void onResponseVehicleDetailsListener(MarkerDataStandardized markerDataStandardized) {
                hideLoader(view);

                if (context.getMarkerArtist() != null) {
                    context.getMarkerArtist().getRouteArtist().drawVehicleRoute(markerDataStandardized);
                }

                showVehicleDetails(markerDataStandardized, view);
                fetchNetworkLogo(markerDataStandardized, view);
            }

            @Override
            public void onErrorVehicleDetailsListener(String error) {
                hideLoader(view);
                showError(view);
            }
        });
    }

    private void fetchNetworkLogo(MarkerDataStandardized markerDataStandardized, View view) {
        if (markerDataStandardized.getNetworkId() == 0) return;

        context.getFetcher().fetchNetworkData(markerDataStandardized.getNetworkId(), new FetchingManager.OnNetworkDataListener() {
            @Override
            public void onResponseNetworkDataListener(NetworkData nData) {
                URI imgURI = nData.getLogoHref();
                if (imgURI != null) {
                    loadNetworkLogo(view, imgURI);
                }
            }

            @Override
            public void onErrorNetworkDataListener(String error) {
                Log.w(TAG, "Erreur lors de la récuperation du logo");
            }
        });
    }

    private void loadNetworkLogo(View view, URI imgURI) {
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

    private void hideLoader(View view) {
        view.findViewById(R.id.loader).setVisibility(View.GONE);
    }

    private void showError(View view) {
        TextView tvDest = view.findViewById(R.id.tvDestination);
        tvDest.setText(R.string.network_error);
    }

    // ==================== DISPLAY ====================
    private void showVehicleDetails(MarkerDataStandardized markerDataStandardized, View view) {
        context.getFollowManager().setFollowButton(view.findViewById(R.id.followButton), markerDataStandardized.getId());

        setupDestinationText(view, markerDataStandardized);
        setupStopsList(view, markerDataStandardized);
    }

    private void setupDestinationText(View view, MarkerDataStandardized markerDataStandardized) {
        TextView tvDestination = view.findViewById(R.id.tvDestination);
        tvDestination.setText(markerDataStandardized.getDestination());
        tvDestination.setSingleLine(true);
        tvDestination.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvDestination.setMarqueeRepeatLimit(-1);
        tvDestination.setHorizontallyScrolling(true);
        tvDestination.setSelected(true);
    }

    private void setupStopsList(View view, MarkerDataStandardized markerDataStandardized) {
        RecyclerView rvStops = view.findViewById(R.id.rvStops);
        rvStops.setLayoutManager(new LinearLayoutManager(context));

        List<MarkerDataStop> stops = markerDataStandardized.getStops() != null ? markerDataStandardized.getStops() : new ArrayList<>();
        rvStops.setAdapter(new StopsAdapter(stops));

        view.findViewById(R.id.llStopsContent).setVisibility(View.VISIBLE);
    }

    public String getCurrentVehicleId() {
        return vehicleId;
    }

    // ==================== ADAPTER ====================
    private class StopsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_STOP = 0;
        private static final int TYPE_EMPTY = 1;

        private final List<MarkerDataStop> stops;

        StopsAdapter(List<MarkerDataStop> stops) {
            this.stops = stops;
        }

        @Override
        public int getItemViewType(int position) {
            return stops.isEmpty() ? TYPE_EMPTY : TYPE_STOP;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_EMPTY) {
                return createEmptyViewHolder(parent);
            }
            return createStopViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_EMPTY) {
                bindEmptyViewHolder(holder);
                return;
            }

            MarkerDataStop stop = stops.get(position);
            bindStopViewHolder((StopViewHolder) holder, stop);
        }

        @Override
        public int getItemCount() {
            return stops.isEmpty() ? 1 : stops.size();
        }

        // ========== NO DATA ==========
        private RecyclerView.ViewHolder createEmptyViewHolder(ViewGroup parent) {
            TextView tvEmpty = new TextView(parent.getContext());
            tvEmpty.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            tvEmpty.setPadding(0, 32, 0, 32);
            tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            return new RecyclerView.ViewHolder(tvEmpty) {};
        }

        private void bindEmptyViewHolder(RecyclerView.ViewHolder holder) {
            TextView tvEmpty = (TextView) holder.itemView;
            tvEmpty.setText(R.string.no_data);
            tvEmpty.setTextColor(MaterialColors.getColor(
                    holder.itemView,
                    com.google.android.material.R.attr.colorOnSurface
            ));
        }

        // ========== STOP ITEM ==========
        private RecyclerView.ViewHolder createStopViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stop, parent, false);
            return new StopViewHolder(view);
        } //inflate item stop

        private void bindStopViewHolder(StopViewHolder vh, MarkerDataStop stop) { //distribute data
            bindPlatform(vh, stop);
            bindStopName(vh, stop);
            bindArrivalTime(vh, stop);
            bindAtStopTime(vh, stop);
            bindDepartureTime(vh, stop);
            bindDelay(vh, stop);
        }

        private void bindPlatform(StopViewHolder vh, MarkerDataStop stop) {
            String platformName = stop.getPlatformName();
            if (platformName != null && !platformName.isEmpty()) {
                vh.tvPlatform.setText(platformName);
                vh.tvPlatform.setVisibility(View.VISIBLE);
                vh.tvPlatform.setTextColor(Color.WHITE);
                vh.spacerPlatform.setVisibility(View.VISIBLE);

                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setStroke(2, Color.WHITE);
                gd.setCornerRadius(10);
                vh.tvPlatform.setBackground(gd);
            } else {
                vh.tvPlatform.setVisibility(View.GONE);
                vh.spacerPlatform.setVisibility(View.GONE);
            }
        }

        private void bindStopName(StopViewHolder vh, MarkerDataStop stop) {
            SpannableStringBuilder builder = new SpannableStringBuilder(stop.getStopName());

            int iconRes = getStopIconResource(stop);
            if (iconRes != 0) {
                appendStopIcon(vh, builder, iconRes);
            }

            vh.tvStopName.setText(builder);
            vh.tvStopName.setSelected(true);
        }

        private int getStopIconResource(MarkerDataStop stop) {
            if (stop.cantPickup()) return R.drawable.logout_24px;
            if (stop.cantDropoff()) return R.drawable.login_24px;
            return 0;
        }

        private void appendStopIcon(StopViewHolder vh, SpannableStringBuilder builder, int iconRes) {
            builder.append("  ");
            Drawable d = ContextCompat.getDrawable(context, iconRes);
            if (d != null) {
                d.mutate();
                d.setColorFilter(new PorterDuffColorFilter(
                        MaterialColors.getColor(vh.tvStopName, com.google.android.material.R.attr.colorOnSurface),
                        PorterDuff.Mode.SRC_IN
                ));
                int size = (int) (vh.tvStopName.getTextSize() * 1.2f);
                d.setBounds(0, 0, size, size);
                builder.setSpan(
                        new ImageSpan(d, ImageSpan.ALIGN_BOTTOM),
                        builder.length() - 1,
                        builder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        private void bindArrivalTime(StopViewHolder vh, MarkerDataStop stop) {
            MarkerDataStandardized vehicle = stop.getVehicle();
            if (!vehicle.isTrain() || stop.isDepartureStop()) {
                vh.tvArrivingTime.setVisibility(View.GONE);
                return;
            }

            LocalTime arrivalTime = stop.getArrivalTime();
            if (arrivalTime != null) {
                vh.tvArrivingTime.setVisibility(View.VISIBLE);
                vh.tvArrivingTime.setText(arrivalTime.format(TIME_FORMATTER));
                vh.tvArrivingTime.setTextColor(stop.isOnLive() ? COLOR_GREEN : getDefaultTextColor(vh));
                bindOnLive(vh.ivArrivingTimeIcon, stop);
            } else {
                vh.tvArrivingTime.setVisibility(View.GONE);
            }
        }

        private void bindAtStopTime(StopViewHolder vh, MarkerDataStop stop) {
            MarkerDataStandardized vehicle = stop.getVehicle();
            if (!vehicle.isTrain() || stop.isDestinationStop() || stop.isDepartureStop()) {
                vh.tvAtStopTime.setVisibility(View.GONE);
                return;
            }
            Long atStopMinutes = stop.getAtStopTime();
            if (atStopMinutes != null && atStopMinutes >= 0) {
                vh.tvAtStopTime.setVisibility(View.VISIBLE);
                vh.tvAtStopTime.setText(atStopMinutes + "min d'arrêt");
                vh.tvAtStopTime.setTextColor(Color.GRAY);
            } else {
                vh.tvAtStopTime.setText("—");
            }
        }

        private void bindDepartureTime(StopViewHolder vh, MarkerDataStop stop) {
            if (stop.isDestinationStop()) {
                vh.tvDepartureTime.setVisibility(View.GONE);
                return;
            }

            LocalTime departureTime = stop.getDepartureTime();
            if (departureTime != null) {
                vh.tvDepartureTime.setVisibility(View.VISIBLE);
                vh.tvDepartureTime.setText(departureTime.format(TIME_FORMATTER));
                vh.tvDepartureTime.setTextColor(stop.isOnLive() ? COLOR_GREEN : getDefaultTextColor(vh));
                bindOnLive(vh.ivDepartureTimeIcon, stop);
            } else {
                vh.tvDepartureTime.setText("??:??");
            }
        }

        private void bindOnLive(ImageView ivTimeIcon, MarkerDataStop stop) {
            if (stop.isOnLive()) {
                ivTimeIcon.setImageResource(R.drawable.sensors_24px);
                ivTimeIcon.setColorFilter(COLOR_GREEN);
                ivTimeIcon.setVisibility(View.VISIBLE);
//                vh.tvAtStopTime.setLayoutParams(new LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                ));
            } else {
                ivTimeIcon.setVisibility(View.GONE);
            }
        }

        private void bindDelay(StopViewHolder vh, MarkerDataStop stop) {
            vh.tvDelay.setVisibility(View.GONE);

            if (stop.getDelay() == null || stop.getDelay() == 0) return;

            vh.tvDelay.setVisibility(View.VISIBLE);
            vh.tvDelay.setText(stop.getDelayText());
            vh.tvDelay.setTextColor(stop.getDelayColor());
        }

        private int getDefaultTextColor(StopViewHolder vh) {
            return MaterialColors.getColor(vh.tvDepartureTime, com.google.android.material.R.attr.colorOnSurface);
        }
    }

    private static class StopViewHolder extends RecyclerView.ViewHolder {
        final TextView tvPlatform, tvStopName, tvDepartureTime, tvAtStopTime, tvArrivingTime, tvDelay;
        final View spacerPlatform;
        final ImageView ivArrivingTimeIcon, ivDepartureTimeIcon;

        StopViewHolder(View itemView) {
            super(itemView);
            tvPlatform = itemView.findViewById(R.id.tvPlatform);
            tvStopName = itemView.findViewById(R.id.tvStopName);
            tvDepartureTime = itemView.findViewById(R.id.tvDepartureTime);
            tvAtStopTime = itemView.findViewById(R.id.tvAtStopTime);
            tvArrivingTime = itemView.findViewById(R.id.tvArrivingTime);
            tvDelay = itemView.findViewById(R.id.tvDelay);
            spacerPlatform = itemView.findViewById(R.id.spacerPlatform);
            ivArrivingTimeIcon = itemView.findViewById(R.id.ivArrivingTimeIcon);
            ivDepartureTimeIcon = itemView.findViewById(R.id.ivDepartureTimeIcon);
        }
    }
}