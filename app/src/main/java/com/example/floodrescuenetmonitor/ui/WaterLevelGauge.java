package com.example.floodrescuenetmonitor.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** A full-circle 0–20 cm ring (optionally with a tinted water fill); an unknown value draws only the track. */
public final class WaterLevelGauge extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double fraction;
    private int color = Color.GRAY;
    private boolean filled;
    public WaterLevelGauge(Context context, AttributeSet attrs) { super(context, attrs); }
    /** Live Monitoring style: tints the inside of the ring up to the current level. */
    public void setFilled(boolean filled) { this.filled = filled; invalidate(); }
    public void setLevel(Double cm, String tier) {
        fraction = cm == null ? 0 : Math.max(0, Math.min(1, cm / 20));
        color = "DANGER".equals(tier) ? Color.rgb(220,38,38) : "WARNING".equals(tier)
                ? Color.rgb(217,119,6) : "NORMAL".equals(tier) ? Color.rgb(22,163,74) : Color.GRAY;
        setContentDescription(cm == null ? "Water level unavailable" : String.format(java.util.Locale.getDefault(), "Water level %.1f of 20 centimetres, %s", cm, tier));
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = 10 * getResources().getDisplayMetrics().density;
        float size = Math.min(getWidth(), getHeight()) - stroke * 2;
        float x = (getWidth() - size) / 2, y = (getHeight() - size) / 2;
        RectF bounds = new RectF(x, y, x + size, y + size);
        if (filled && fraction > 0) {
            float inner = size / 2 - stroke / 2;
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float top = cy + inner - (float) (2 * inner * fraction);
            canvas.save();
            android.graphics.Path circle = new android.graphics.Path();
            circle.addCircle(cx, cy, inner, android.graphics.Path.Direction.CW);
            canvas.clipPath(circle);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawRect(cx - inner, top, cx + inner, cy + inner, paint);
            canvas.restore();
        }
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(stroke); paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(238,238,246)); canvas.drawArc(bounds, -90, 360, false, paint);
        if (fraction > 0) { paint.setColor(color); canvas.drawArc(bounds, -90, (float)(360 * fraction), false, paint); }
    }
}
