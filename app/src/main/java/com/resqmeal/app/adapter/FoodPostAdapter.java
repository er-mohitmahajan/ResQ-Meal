package com.resqmeal.app.adapter;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.resqmeal.app.R;
import com.resqmeal.app.databinding.ViewFoodCardBinding;
import com.resqmeal.app.model.FoodPost;
import com.resqmeal.app.utils.Constants;
import com.resqmeal.app.utils.GeoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FoodPostAdapter extends RecyclerView.Adapter<FoodPostAdapter.ViewHolder> {
    private static final Object PAYLOAD_EXPIRY = new Object();
    private static final long EXPIRY_REFRESH_INTERVAL_MS = 60_000L;

    public interface OnPostClickListener {
        void onPostClick(FoodPost post);
    }

    private List<FoodPost> posts = new ArrayList<>();
    private final OnPostClickListener listener;
    private final Handler expiryHandler = new Handler(Looper.getMainLooper());
    private Double distanceOriginLat;
    private Double distanceOriginLng;
    private boolean expiryUpdatesStarted;

    private final Runnable expiryUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!expiryUpdatesStarted) {
                return;
            }
            if (getItemCount() > 0) {
                notifyItemRangeChanged(0, getItemCount(), PAYLOAD_EXPIRY);
            }
            expiryHandler.postDelayed(this, EXPIRY_REFRESH_INTERVAL_MS);
        }
    };

    public FoodPostAdapter(OnPostClickListener listener) {
        this.listener = listener;
    }

    public void setPosts(List<FoodPost> posts) {
        this.posts = posts == null ? new ArrayList<>() : new ArrayList<>(posts);
        notifyDataSetChanged();
    }

    public void setDistanceOrigin(@Nullable Double latitude, @Nullable Double longitude) {
        distanceOriginLat = latitude;
        distanceOriginLng = longitude;
        if (getItemCount() > 0) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public void startExpiryUpdates() {
        if (expiryUpdatesStarted) {
            return;
        }
        expiryUpdatesStarted = true;
        expiryHandler.post(expiryUpdateRunnable);
    }

    public void stopExpiryUpdates() {
        expiryUpdatesStarted = false;
        expiryHandler.removeCallbacks(expiryUpdateRunnable);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewFoodCardBinding binding = ViewFoodCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position,
            @NonNull List<Object> payloads
    ) {
        if (payloads.contains(PAYLOAD_EXPIRY)) {
            holder.bindExpiry(posts.get(position));
        } else {
            onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ViewFoodCardBinding binding;

        ViewHolder(ViewFoodCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FoodPost post) {
            binding.tvFoodTitle.setText(post.getTitle() == null ? "" : post.getTitle());
            binding.tvQuantityServings.setText(String.format(Locale.getDefault(), "%s %s • feeds ~%d",
                    post.getQuantity(), post.getUnit() == null ? "" : post.getUnit(), post.getServings()));
            if (distanceOriginLat != null && distanceOriginLng != null
                    && post.getLat() != null && post.getLng() != null
                    && GeoUtils.isValidCoordinates(post.getLat(), post.getLng())) {
                float distance = GeoUtils.getDistanceBetween(
                        distanceOriginLat, distanceOriginLng, post.getLat(), post.getLng());
                binding.tvDistance.setText(GeoUtils.formatDistance(distance));
            } else {
                binding.tvDistance.setText(post.getPickupAddress() == null ? "" : post.getPickupAddress());
            }
            binding.chipStatus.setStatus(post.getStatus());
            bindExpiry(post);

            Glide.with(binding.ivFoodPhoto.getContext())
                    .load(post.getPhotoUrl())
                    .centerCrop()
                    .placeholder(R.color.shimmer_base)
                    .into(binding.ivFoodPhoto);

            itemView.setOnClickListener(v -> listener.onPostClick(post));
        }

        void bindExpiry(FoodPost post) {
            if (Constants.STATUS_EXPIRED.equals(post.getStatus())) {
                binding.tvExpiryCountdown.setText("Expired");
                binding.tvExpiryCountdown.setTextColor(
                        binding.tvExpiryCountdown.getContext().getColor(R.color.error));
                return;
            }
            if (post.getExpiryTime() == null) {
                binding.tvExpiryCountdown.setText("");
                return;
            }

            long remainingMillis = post.getExpiryTime().toDate().getTime() - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                binding.tvExpiryCountdown.setText("Expired");
                binding.tvExpiryCountdown.setTextColor(
                        binding.tvExpiryCountdown.getContext().getColor(R.color.error));
                return;
            }

            long remainingMinutes = (remainingMillis + 59_999L) / 60_000L;
            long hours = remainingMinutes / 60;
            long minutes = remainingMinutes % 60;
            String countdown = hours > 0
                    ? String.format(Locale.getDefault(), "Expires in %dh %dm", hours, minutes)
                    : String.format(Locale.getDefault(), "Expires in %dm", minutes);
            binding.tvExpiryCountdown.setText(countdown);
            int color = remainingMillis < 60 * 60_000L ? R.color.accent : R.color.text_secondary;
            binding.tvExpiryCountdown.setTextColor(
                    binding.tvExpiryCountdown.getContext().getColor(color));
        }
    }
}
