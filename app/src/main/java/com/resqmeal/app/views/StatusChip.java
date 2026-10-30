package com.resqmeal.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.resqmeal.app.R;
import com.resqmeal.app.utils.Constants;

public class StatusChip extends FrameLayout {

    private TextView textView;
    private GradientDrawable backgroundDrawable;

    public StatusChip(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public StatusChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public StatusChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        textView = new TextView(context);
        textView.setTextSize(12);
        textView.setTypeface(ResourcesCompat.getFont(context, R.font.poppins_medium));
        textView.setGravity(Gravity.CENTER);

        int padH = (int) (12 * getResources().getDisplayMetrics().density);
        int padV = (int) (4 * getResources().getDisplayMetrics().density);
        setPadding(padH, padV, padH, padV);

        backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setCornerRadius(16 * getResources().getDisplayMetrics().density);
        setBackground(backgroundDrawable);

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        addView(textView, lp);

        setStatus(Constants.STATUS_AVAILABLE);
    }

    public void setStatus(String status) {
        if (status == null) {
            status = Constants.STATUS_AVAILABLE;
        }

        int bgColor;
        int textColor;
        String displayText;

        switch (status.toLowerCase()) {
            case Constants.STATUS_CLAIMED:
                bgColor = ContextCompat.getColor(getContext(), R.color.status_claimed_bg);
                textColor = ContextCompat.getColor(getContext(), R.color.status_claimed_text);
                displayText = "Claimed";
                break;
            case Constants.STATUS_COMPLETED:
                bgColor = ContextCompat.getColor(getContext(), R.color.status_completed_bg);
                textColor = ContextCompat.getColor(getContext(), R.color.status_completed_text);
                displayText = "Completed";
                break;
            case Constants.STATUS_EXPIRED:
                bgColor = ContextCompat.getColor(getContext(), R.color.status_expired_bg);
                textColor = ContextCompat.getColor(getContext(), R.color.status_expired_text);
                displayText = "Expired";
                break;
            case Constants.STATUS_CANCELLED:
                bgColor = ContextCompat.getColor(getContext(), R.color.status_cancelled_bg);
                textColor = ContextCompat.getColor(getContext(), R.color.status_cancelled_text);
                displayText = "Cancelled";
                break;
            case Constants.STATUS_AVAILABLE:
            default:
                bgColor = ContextCompat.getColor(getContext(), R.color.status_available_bg);
                textColor = ContextCompat.getColor(getContext(), R.color.status_available_text);
                displayText = "Available";
                break;
        }

        backgroundDrawable.setColor(bgColor);
        textView.setTextColor(textColor);
        textView.setText(displayText);
    }

    public String getText() {
        return textView.getText().toString();
    }
}

