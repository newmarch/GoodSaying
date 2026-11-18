package com.example.sunchan.goodsaying;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

final class ScreenInsets {
    private ScreenInsets() {}

    static void apply(Activity activity) {
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }

        ViewGroup content = activity.findViewById(android.R.id.content);
        View root = content.getChildAt(0);
        root.setFitsSystemWindows(false);
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets safeArea = windowInsets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(left + safeArea.left, top + safeArea.top,
                    right + safeArea.right, bottom + safeArea.bottom);
            return WindowInsets.CONSUMED;
        });
        root.requestApplyInsets();
    }
}
