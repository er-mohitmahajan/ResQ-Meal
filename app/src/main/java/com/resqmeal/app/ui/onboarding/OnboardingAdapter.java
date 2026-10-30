package com.resqmeal.app.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.resqmeal.app.databinding.ItemOnboardingSlideBinding;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnboardingItem> items;

    public OnboardingAdapter(List<OnboardingItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOnboardingSlideBinding binding = ItemOnboardingSlideBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new OnboardingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {

        private final ItemOnboardingSlideBinding binding;

        public OnboardingViewHolder(@NonNull ItemOnboardingSlideBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(OnboardingItem item) {
            binding.tvSlideTitle.setText(item.getTitle());
            binding.tvSlideDescription.setText(item.getDescription());
            binding.lottieSlide.setAnimation(item.getLottieRes());
            binding.lottieSlide.playAnimation();
        }
    }
}

