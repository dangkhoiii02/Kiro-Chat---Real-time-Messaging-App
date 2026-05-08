package com.ptithcm.kiro_mobile.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.ptithcm.kiro_mobile.R;
import com.ptithcm.kiro_mobile.config.AppConfig;

/**
 * Centralized image loading utility.
 * Automatically normalizes MinIO/backend URLs for emulator compatibility.
 */
public class ImageLoader {

    private ImageLoader() {}

    /** Load a circular avatar image, replacing localhost URLs for emulator. */
    public static void loadAvatar(Context context, String url, ImageView target) {
        String fixedUrl = normalize(url);
        Glide.with(context)
                .load(fixedUrl)
                .apply(new RequestOptions()
                        .circleCrop()
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder))
                .into(target);
    }

    /** Load a regular image (non-circular). */
    public static void loadImage(Context context, String url, ImageView target) {
        String fixedUrl = normalize(url);
        Glide.with(context)
                .load(fixedUrl)
                .placeholder(android.R.color.darker_gray)
                .error(R.drawable.ic_avatar_placeholder)
                .into(target);
    }

    /**
     * Replace localhost URLs with emulator-accessible addresses.
     * localhost:9000 → 10.0.2.2:9000 (MinIO)
     * localhost:8080 → 10.0.2.2:8080 (Backend)
     */
    public static String normalize(String url) {
        if (url == null || url.isEmpty()) return null;
        return url
                .replace("http://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                .replace("https://localhost:9000", AppConfig.MINIO_PUBLIC_URL)
                .replace("http://localhost:8080", "http://10.0.2.2:8080")
                .replace("https://localhost:8080", "http://10.0.2.2:8080");
    }
}
