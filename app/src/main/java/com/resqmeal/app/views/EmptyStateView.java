package com.resqmeal.app.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.airbnb.lottie.LottieAnimationView;
import com.resqmeal.app.R;

public class EmptyStateView extends FrameLayout {

    private LottieAnimationView lottieAnimation;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptyMessage;
    private PrimaryButton btnEmptyAction;

    public EmptyStateView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_empty_state, this, true);
        lottieAnimation = findViewById(R.id.lottieAnimation);
        ivEmptyIcon = findViewById(R.id.ivEmptyIcon);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnEmptyAction = findViewById(R.id.btnEmptyAction);
    }

    public void setContent(String title, String message) {
        tvEmptyTitle.setText(title);
        tvEmptyMessage.setText(message);
    }

    public void setIcon(int iconRes) {
        ivEmptyIcon.setImageResource(iconRes);
        ivEmptyIcon.setVisibility(VISIBLE);
        lottieAnimation.setVisibility(GONE);
    }

    public void setLottieAnimation(@RawRes int rawRes) {
        lottieAnimation.setAnimation(rawRes);
        lottieAnimation.setVisibility(VISIBLE);
        lottieAnimation.playAnimation();
        ivEmptyIcon.setVisibility(GONE);
    }

    public void setAction(String actionText, OnClickListener listener) {
        btnEmptyAction.setText(actionText);
        btnEmptyAction.setVisibility(VISIBLE);
        btnEmptyAction.setOnClickListener(listener);
    }

    public void hideAction() {
        btnEmptyAction.setVisibility(GONE);
    }
}
