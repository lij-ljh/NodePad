package com.example.notepad.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.notepad.R;

/**
 * 横线纸背景：纯色底 + 等距横线，用于“记事本背景”样式。
 */
public class LinedPaperDrawable extends Drawable {

    private final Paint backgroundPaint = new Paint();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float spacing;
    private final float inset;

    public LinedPaperDrawable(Context context, int backgroundColor) {
        float density = context.getResources().getDisplayMetrics().density;
        spacing = 28f * density;
        inset = 14f * density;

        backgroundPaint.setColor(backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);

        int lineColor = ContextCompat.getColor(context, R.color.text_muted);
        linePaint.setColor(Color.argb(64, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)));
        linePaint.setStrokeWidth(Math.max(1f, density));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawRect(getBounds(), backgroundPaint);
        float left = getBounds().left + inset;
        float right = getBounds().right - inset;
        for (float y = getBounds().top + spacing; y < getBounds().bottom; y += spacing) {
            canvas.drawLine(left, y, right, y, linePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        backgroundPaint.setAlpha(alpha);
        linePaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        backgroundPaint.setColorFilter(colorFilter);
        linePaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
