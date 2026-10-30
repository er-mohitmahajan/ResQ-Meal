package com.resqmeal.app.views;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

public class PhotoPicker {

    public interface OnPhotoSourceSelectedListener {
        void onCameraSelected();
        void onGallerySelected();
    }

    public static void show(@NonNull Context context, @NonNull OnPhotoSourceSelectedListener listener) {
        CharSequence[] options = {"Take Photo (Camera)", "Choose from Gallery"};

        ShapeAppearanceModel shape = ShapeAppearanceModel.builder()
                .setAllCorners(CornerFamily.ROUNDED, 16 * context.getResources().getDisplayMetrics().density)
                .build();

        new MaterialAlertDialogBuilder(context)
                .setTitle("Select Photo")
                .setBackground(new com.google.android.material.shape.MaterialShapeDrawable(shape))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        listener.onCameraSelected();
                    } else if (which == 1) {
                        listener.onGallerySelected();
                    }
                })
                .show();
    }
}
