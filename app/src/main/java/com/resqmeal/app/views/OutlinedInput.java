package com.resqmeal.app.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.resqmeal.app.R;

public class OutlinedInput extends FrameLayout {

    private TextInputLayout textInputLayout;
    private TextInputEditText editText;

    public OutlinedInput(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public OutlinedInput(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OutlinedInput(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_outlined_input, this, true);
        textInputLayout = findViewById(R.id.textInputLayout);
        editText = findViewById(R.id.editText);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OutlinedInput);
            String hint = a.getString(R.styleable.OutlinedInput_inputHint);
            String error = a.getString(R.styleable.OutlinedInput_inputError);
            int inputType = a.getInt(R.styleable.OutlinedInput_android_inputType, -1);
            a.recycle();

            if (hint != null) {
                textInputLayout.setHint(hint);
            }
            if (error != null) {
                textInputLayout.setError(error);
            }
            if (inputType != -1) {
                editText.setInputType(inputType);
            }
        }
    }

    public String getText() {
        Editable editable = editText.getText();
        return editable != null ? editable.toString().trim() : "";
    }

    public void setText(CharSequence text) {
        editText.setText(text);
    }

    public void setHint(CharSequence hint) {
        textInputLayout.setHint(hint);
    }

    public void setError(@Nullable CharSequence error) {
        textInputLayout.setError(error);
    }

    public TextInputEditText getEditText() {
        return editText;
    }

    public TextInputLayout getTextInputLayout() {
        return textInputLayout;
    }

    public void addTextChangedListener(TextWatcher watcher) {
        editText.addTextChangedListener(watcher);
    }
}

