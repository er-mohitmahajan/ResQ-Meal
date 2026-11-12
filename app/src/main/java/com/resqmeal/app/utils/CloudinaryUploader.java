package com.resqmeal.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryUploader {

    public interface UploadListener {
        void onSuccess(String url, String publicId);
        void onError(String error);
    }

    public static void init(Context context) {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", Constants.CLOUDINARY_CLOUD_NAME);
            MediaManager.init(context, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    public static void uploadImage(Context context, Uri imageUri, String folder, UploadListener listener) {
        File compressedFile = null;
        try {
            compressedFile = compressImage(context, imageUri);
            if (compressedFile == null) {
                listener.onError("Failed to compress image");
                return;
            }

            File uploadedFile = compressedFile;
            MediaManager.get().upload(uploadedFile.getAbsolutePath())
                    .unsigned(Constants.CLOUDINARY_UPLOAD_PRESET)
                    .option("folder", folder)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            String publicId = (String) resultData.get("public_id");
                            try {
                                if (url == null || publicId == null) {
                                    listener.onError("Cloudinary returned incomplete image information");
                                } else {
                                    listener.onSuccess(url, publicId);
                                }
                            } finally {
                                uploadedFile.delete();
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            try {
                                listener.onError(error.getDescription() == null
                                        ? "Cloudinary image upload failed"
                                        : error.getDescription());
                            } finally {
                                uploadedFile.delete();
                            }
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                        }
                    }).dispatch();
        } catch (Exception e) {
            if (compressedFile != null) {
                compressedFile.delete();
            }
            listener.onError(e.getMessage() == null
                    ? "Cloudinary image upload failed"
                    : e.getMessage());
        }
    }

    private static File compressImage(Context context, Uri imageUri) throws IOException {
        if (imageUri == null) {
            throw new IOException("No image was selected");
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) {
                throw new IOException("Unable to open the selected image");
            }
            BitmapFactory.decodeStream(inputStream, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("The selected file is not a valid image");
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = 1;
        while (bounds.outWidth / decodeOptions.inSampleSize > 1080) {
            decodeOptions.inSampleSize *= 2;
        }

        Bitmap bitmap;
        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) {
                throw new IOException("Unable to reopen the selected image");
            }
            bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions);
        }
        if (bitmap == null) {
            throw new IOException("Unable to decode the selected image");
        }

        Bitmap outputBitmap = bitmap;
        File compressedFile = null;
        try {
            if (bitmap.getWidth() > 1080) {
                int height = Math.round(bitmap.getHeight() * (1080f / bitmap.getWidth()));
                outputBitmap = Bitmap.createScaledBitmap(bitmap, 1080, height, true);
            }

            compressedFile = File.createTempFile("upload_", ".jpg", context.getCacheDir());
            try (FileOutputStream outputStream = new FileOutputStream(compressedFile)) {
                if (!outputBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)) {
                    throw new IOException("Unable to compress the selected image");
                }
                outputStream.flush();
            }
            return compressedFile;
        } catch (IOException | RuntimeException e) {
            if (compressedFile != null) {
                compressedFile.delete();
            }
            throw e;
        } finally {
            if (outputBitmap != bitmap) {
                outputBitmap.recycle();
            }
            bitmap.recycle();
        }
    }
}
