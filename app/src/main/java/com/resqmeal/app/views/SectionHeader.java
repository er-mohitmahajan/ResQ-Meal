package com.resqmeal.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.resqmeal.app.R;

public class SectionHeader extends LinearLayout {

    private TextView tvHeaderTitle;
    private TextView tvHeaderAction;

    public SectionHeader(Context context) {
        super(context);
        init(context, null);
    }

    public SectionHeader(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SectionHeader(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int padV = (int) (8 * getResources().getDisplayMetrics().density);
        setPadding(0, padV, 0, padV);

        LayoutInflater.from(context).inflate(R.layout.view_section_header, this, true);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderAction = findViewById(R.id.tvHeaderAction);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SectionHeader);
            String title = a.getString(R.styleable.SectionHeader_headerTitle);
            String action = a.getString(R.styleable.SectionHeader_actionText);
            boolean showAction = a.getBoolean(R.styleable.SectionHeader_showAction, false);
            a.recycle();

            if (title != null) {
                tvHeaderTitle.setText(title);
            }
            if (action != null) {
                tvHeaderAction.setText(action);
            }
            tvHeaderAction.setVisibility(showAction ? VISIBLE : GONE);
        }
    }

    public void setTitle(CharSequence title) {
        tvHeaderTitle.setText(title);
    }

    public void setAction(CharSequence actionText, OnClickListener listener) {
        tvHeaderAction.setText(actionText);
        tvHeaderAction.setVisibility(VISIBLE);
        tvHeaderAction.setOnClickListener(listener);
    }
}

