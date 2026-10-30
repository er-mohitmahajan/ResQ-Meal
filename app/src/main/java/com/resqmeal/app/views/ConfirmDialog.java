package com.resqmeal.app.views;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.resqmeal.app.R;

public class ConfirmDialog {

    public interface OnConfirmListener {
        void onConfirm();
    }

    public interface OnCancelListener {
        void onCancel();
    }

    public static AlertDialog show(@NonNull Context context,
                                  @NonNull String title,
                                  @NonNull String message,
                                  @NonNull String positiveText,
                                  @Nullable String negativeText,
                                  @Nullable OnConfirmListener confirmListener,
                                  @Nullable OnCancelListener cancelListener) {

        ShapeAppearanceModel shape = ShapeAppearanceModel.builder()
                .setAllCorners(CornerFamily.ROUNDED, 16 * context.getResources().getDisplayMetrics().density)
                .build();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (dialog, which) -> {
                    if (confirmListener != null) {
                        confirmListener.onConfirm();
                    }
                });

        if (negativeText != null) {
            builder.setNegativeButton(negativeText, (dialog, which) -> {
                if (cancelListener != null) {
                    cancelListener.onCancel();
                }
            });
        }

        builder.setBackground(new com.google.android.material.shape.MaterialShapeDrawable(shape));
        AlertDialog dialog = builder.create();
        dialog.show();

        // Style buttons
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getColor(R.color.primary));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getColor(R.color.text_secondary));
        }

        return dialog;
    }
}

