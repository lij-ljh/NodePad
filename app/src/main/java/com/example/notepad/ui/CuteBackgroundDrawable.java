package com.example.notepad.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * 可爱卡通风格背景：柔和渐变搭配云朵、星星与圆点点缀。
 */
public class CuteBackgroundDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path starPath = new Path();
    private final boolean night;
    private final float density;

    private Shader gradient;

    public CuteBackgroundDrawable(Context context) {
        night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        int[] colors = night
                ? new int[]{0xFF201721, 0xFF241A27, 0xFF172128}
                : new int[]{0xFFFFF7F2, 0xFFFFF0F7, 0xFFEDF9FF};
        gradient = new LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.bottom,
                colors, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        paint.setShader(gradient);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(bounds, paint);
        paint.setShader(null);

        float w = bounds.width();
        float h = bounds.height();
        int cloud = night ? 0x14FFFFFF : 0x66FFFFFF;
        int pink = night ? 0x30FFB0C6 : 0x33FF8FAF;
        int mint = night ? 0x227FE0CC : 0x2E56C8B4;
        int apricot = night ? 0x1EFFCF8F : 0x26FFB65C;

        cloud(canvas, w * 0.80f, h * 0.09f, 1.1f, cloud);
        cloud(canvas, w * 0.16f, h * 0.32f, 0.8f, cloud);
        cloud(canvas, w * 0.72f, h * 0.68f, 0.65f, cloud);

        sparkle(canvas, w * 0.90f, h * 0.24f, 10f, pink);
        sparkle(canvas, w * 0.10f, h * 0.16f, 7f, mint);
        sparkle(canvas, w * 0.24f, h * 0.58f, 8f, apricot);
        sparkle(canvas, w * 0.86f, h * 0.52f, 6f, mint);
        sparkle(canvas, w * 0.60f, h * 0.88f, 7f, pink);

        dot(canvas, w * 0.06f, h * 0.72f, 5f, mint);
        dot(canvas, w * 0.94f, h * 0.82f, 6f, apricot);
        dot(canvas, w * 0.44f, h * 0.20f, 4f, pink);
        dot(canvas, w * 0.52f, h * 0.47f, 3.5f, mint);
    }

    private void cloud(Canvas canvas, float cx, float cy, float scale, int color) {
        float unit = density * scale;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(cx, cy, 20f * unit, paint);
        canvas.drawCircle(cx - 18f * unit, cy + 5f * unit, 14f * unit, paint);
        canvas.drawCircle(cx + 17f * unit, cy + 4f * unit, 12f * unit, paint);
        canvas.drawCircle(cx - 2f * unit, cy + 8f * unit, 16f * unit, paint);
    }

    private void sparkle(Canvas canvas, float cx, float cy, float radiusDp, int color) {
        float r = radiusDp * density;
        float inner = r * 0.18f;
        starPath.reset();
        starPath.moveTo(cx, cy - r);
        starPath.quadTo(cx + inner, cy - inner, cx + r, cy);
        starPath.quadTo(cx + inner, cy + inner, cx, cy + r);
        starPath.quadTo(cx - inner, cy + inner, cx - r, cy);
        starPath.quadTo(cx - inner, cy - inner, cx, cy - r);
        starPath.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(starPath, paint);
    }

    private void dot(Canvas canvas, float cx, float cy, float radiusDp, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(cx, cy, radiusDp * density, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        // 背景装饰不支持单独透明度，保持默认。
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // 背景装饰不支持额外滤镜。
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
