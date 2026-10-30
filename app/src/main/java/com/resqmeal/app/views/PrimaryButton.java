package com.resqmeal.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.resqmeal.app.R;

public class PrimaryButton extends FrameLayout {

    private MaterialButton btnAction;
    private CircularProgressIndicator progressBar;
    private CharSequence buttonText = "";
    private boolean isLoading = false;
    private boolean isButtonEnabled = true;

    public PrimaryButton(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public PrimaryButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PrimaryButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_primary_button, this, true);
        btnAction = findViewById(R.id.btnAction);
        progressBar = findViewById(R.id.progressBar);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PrimaryButton);
            buttonText = a.getString(R.styleable.PrimaryButton_buttonText);
            isButtonEnabled = a.getBoolean(R.styleable.PrimaryButton_buttonEnabled, true);
            boolean loading = a.getBoolean(R.styleable.PrimaryButton_buttonLoading, false);
            a.recycle();

            if (buttonText != null) {
                btnAction.setText(buttonText);
            }
            btnAction.setEnabled(isButtonEnabled);
            setLoading(loading);
        }
    }

    public void setText(CharSequence text) {
        this.buttonText = text;
        if (!isLoading) {
            btnAction.setText(text);
        }
    }

    public CharSequence getText() {
        return buttonText;
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            btnAction.setText("");
            btnAction.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnAction.setText(buttonText);
            btnAction.setEnabled(isButtonEnabled);
            progressBar.setVisibility(View.GONE);
        }
    }

    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.isButtonEnabled = enabled;
        if (!isLoading) {
            btnAction.setEnabled(enabled);
        }
    }

    @Override
    public boolean isEnabled() {
        return isButtonEnabled;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        btnAction.setOnClickListener(v -> {
            if (!isLoading && isButtonEnabled && l != null) {
                l.onClick(PrimaryButton.this);
            }
        });
    }
}

