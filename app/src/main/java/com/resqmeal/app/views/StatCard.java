package com.resqmeal.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.resqmeal.app.R;

public class StatCard extends MaterialCardView {

    private TextView tvStatNumber;
    private TextView tvStatLabel;
    private ImageView ivStatIcon;

    public StatCard(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public StatCard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public StatCard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setRadius(16 * getResources().getDisplayMetrics().density);
        setCardElevation(2 * getResources().getDisplayMetrics().density);
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));

        LayoutInflater.from(context).inflate(R.layout.view_stat_card, this, true);
        tvStatNumber = findViewById(R.id.tvStatNumber);
        tvStatLabel = findViewById(R.id.tvStatLabel);
        ivStatIcon = findViewById(R.id.ivStatIcon);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StatCard);
            String number = a.getString(R.styleable.StatCard_statNumber);
            String label = a.getString(R.styleable.StatCard_statLabel);
            int iconRes = a.getResourceId(R.styleable.StatCard_statIcon, 0);
            a.recycle();

            if (number != null) {
                tvStatNumber.setText(number);
            }
            if (label != null) {
                tvStatLabel.setText(label);
            }
            if (iconRes != 0) {
                ivStatIcon.setImageResource(iconRes);
            }
        }
    }

    public void setData(String number, String label, int iconRes) {
        tvStatNumber.setText(number);
        tvStatLabel.setText(label);
        if (iconRes != 0) {
            ivStatIcon.setImageResource(iconRes);
            ivStatIcon.setVisibility(VISIBLE);
        } else {
            ivStatIcon.setVisibility(GONE);
        }
    }

    public void setNumber(String number) {
        tvStatNumber.setText(number);
    }

    public void setLabel(String label) {
        tvStatLabel.setText(label);
    }

    public void setIcon(int iconRes) {
        ivStatIcon.setImageResource(iconRes);
        ivStatIcon.setVisibility(VISIBLE);
    }
}
