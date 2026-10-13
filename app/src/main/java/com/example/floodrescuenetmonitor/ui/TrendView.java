package com.example.floodrescuenetmonitor.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import com.example.floodrescuenetmonitor.data.MonitorState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TrendView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Double> values = new ArrayList<>();
    public TrendView(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setReadings(List<Map<String, Object>> readings) {
        values = new ArrayList<>();
        for (int i = Math.min(30, readings.size()) - 1; i >= 0; i--) {
            Double value = MonitorState.number(readings.get(i), "waterCm");
            if (value != null && value >= 0 && value <= 20) values.add(value);
        }
        setContentDescription(values.size() < 2 ? "Waiting for trend data" : "Water level trend from the latest " + values.size() + " samples");
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.size() < 2) return;
        Path line = new Path();
        for (int i = 0; i < values.size(); i++) {
            float x = 4 + i * (getWidth() - 8f) / (values.size() - 1);
            float y = 4 + (float)(1 - values.get(i) / 20) * (getHeight() - 8f);
            if (i == 0) line.moveTo(x, y); else line.lineTo(x, y);
        }
        paint.setColor(0xff4f46e5); paint.setStrokeWidth(3 * getResources().getDisplayMetrics().density);
        paint.setStyle(Paint.Style.STROKE); canvas.drawPath(line, paint);
    }
}
