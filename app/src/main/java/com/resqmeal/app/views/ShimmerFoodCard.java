package com.resqmeal.app.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.resqmeal.app.R;

public class ShimmerFoodCard extends ShimmerFrameLayout {

    public ShimmerFoodCard(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ShimmerFoodCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ShimmerFoodCard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_shimmer_food_card, this, true);

        Shimmer customShimmer = new Shimmer.ColorHighlightBuilder()
                .setBaseColor(ContextCompat.getColor(context, R.color.shimmer_base))
                .setHighlightColor(ContextCompat.getColor(context, R.color.shimmer_highlight))
                .setDuration(1200)
                .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
                .setAutoStart(true)
                .build();

        setShimmer(customShimmer);
    }
}
