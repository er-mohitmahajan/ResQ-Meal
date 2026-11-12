package com.resqmeal.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resqmeal.app.databinding.ItemClaimBinding;
import com.resqmeal.app.model.Claim;
import com.resqmeal.app.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClaimAdapter extends RecyclerView.Adapter<ClaimAdapter.ViewHolder> {
    public interface OnClaimClickListener {
        void onClaimClick(Claim claim);
    }

    private final OnClaimClickListener listener;
    private final List<Claim> claims = new ArrayList<>();

    public ClaimAdapter(OnClaimClickListener listener) {
        this.listener = listener;
    }

    public void setClaims(List<Claim> claims) {
        this.claims.clear();
        if (claims != null) {
            this.claims.addAll(claims);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemClaimBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(claims.get(position));
    }

    @Override
    public int getItemCount() {
        return claims.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemClaimBinding binding;
        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

        ViewHolder(ItemClaimBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Claim claim) {
            binding.tvClaimTitle.setText(claim.getPostTitle() == null ? "" : claim.getPostTitle());
            binding.tvClaimDonor.setText("From "
                    + (claim.getDonorName() == null ? "Donor" : claim.getDonorName()));
            binding.tvClaimedAt.setText(claim.getClaimedAt() == null
                    ? "" : dateFormat.format(claim.getClaimedAt().toDate()));
            if (Constants.CLAIM_STATUS_ACTIVE.equals(claim.getStatus())) {
                binding.chipClaimStatus.setChipText("Active");
            } else if (Constants.CLAIM_STATUS_COMPLETED.equals(claim.getStatus())) {
                binding.chipClaimStatus.setChipText("Completed");
            } else {
                binding.chipClaimStatus.setChipText("Cancelled");
            }
            binding.getRoot().setOnClickListener(v -> listener.onClaimClick(claim));
        }
    }
}
