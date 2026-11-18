package com.example.sunchan.goodsaying;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import java.io.File;

final class DisplayStyle {
    static final int MIN_SIZE = 16;
    static final int MAX_SIZE = 64;
    static final int DEFAULT_PHOTO_OVERLAY = 35;
    static final int[] COLORS = {Color.WHITE, 0xfffff3db, 0xffdff2e7, 0xffe2ebfa, 0xff263044};
    int font;
    int size;
    int color;
    String imageFile;
    float photoX = 0.5f;
    float photoY = 0.5f;
    float photoZoom = 1f;
    int photoOverlay = DEFAULT_PHOTO_OVERLAY;

    static DisplayStyle load(Context context) {
        SharedPreferences prefs = preferences(context);
        DisplayStyle style = new DisplayStyle();
        style.font = prefs.getInt("font", 0);
        style.size = prefs.getInt("size", 45);
        style.color = prefs.getInt("color", 0);
        style.imageFile = prefs.getString("image", null);
        style.photoX = prefs.getFloat("photo_x", style.photoX);
        style.photoY = prefs.getFloat("photo_y", style.photoY);
        style.photoZoom = prefs.getFloat("photo_zoom", style.photoZoom);
        style.photoOverlay = prefs.getInt("photo_overlay", style.photoOverlay);
        return style;
    }

    boolean save(Context context) {
        return preferences(context).edit().putInt("font", font).putInt("size", size)
                .putInt("color", color).putString("image", imageFile)
                .putFloat("photo_x", photoX).putFloat("photo_y", photoY)
                .putFloat("photo_zoom", photoZoom).putInt("photo_overlay", photoOverlay).commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("saying_appearance", Context.MODE_PRIVATE);
    }

    Bitmap readSavedImage(Context context) {
        return imageFile == null ? null : BitmapFactory.decodeFile(
                new File(context.getFilesDir(), imageFile).getPath());
    }

    void apply(View background, PhotoBackgroundView image, TextView text, Bitmap bitmap) {
        applyText(text, bitmap != null);
        background.setBackgroundColor(COLORS[color]);
        image.setPhoto(bitmap, photoX, photoY, photoZoom, photoOverlay);
        image.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
    }

    void applyText(TextView text, boolean hasPhoto) {
        Typeface typeface;
        switch (font) {
            case 1: typeface = Typeface.SANS_SERIF; break;
            case 2: typeface = Typeface.SERIF; break;
            default: typeface = Typeface.createFromAsset(text.getContext().getAssets(), "NanumPen.ttf");
        }
        text.setTypeface(typeface, Typeface.BOLD);
        text.setTextSize(size);
        boolean darkBackground = color == 4;
        text.setTextColor(darkBackground ? Color.WHITE : 0xff131b30);
        if (darkBackground && hasPhoto) {
            // Keep white text legible when it crosses a light part of the photo.
            text.setShadowLayer(2f, 0f, 1f, Color.BLACK);
        } else {
            text.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        }
    }
}
