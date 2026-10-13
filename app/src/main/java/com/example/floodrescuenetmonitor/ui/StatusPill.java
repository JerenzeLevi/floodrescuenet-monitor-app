package com.example.floodrescuenetmonitor.ui;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.example.floodrescuenetmonitor.R;

/** Reproduces the Compose StatusPill: a fully-rounded pill background with bound text + colors. */
public final class StatusPill {

    private StatusPill() {}

    public static void bind(TextView view, String text, int backgroundColor, int textColor) {
        view.setText(text);
        view.setTextColor(textColor);
        Drawable bg = ContextCompat.getDrawable(view.getContext(), R.drawable.bg_pill).mutate();
        DrawableCompat.setTint(bg, backgroundColor);
        view.setBackground(bg);
        int h = (int) (10 * view.getResources().getDisplayMetrics().density);
        int v = (int) (4 * view.getResources().getDisplayMetrics().density);
        view.setPadding(h, v, h, v);
    }
}
