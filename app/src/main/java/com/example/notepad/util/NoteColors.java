package com.example.notepad.util;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

/**
 * 便签背景色板：浅色主题使用柔和色，深色主题自动调暗以保证文字可读。
 */
public final class NoteColors {

    /** 色板大小。 */
    public static final int COUNT = 8;

    private static final int[] PALETTE = {
            0xFFFDF1C4, // 暖黄
            0xFFD9F2D9, // 浅绿
            0xFFD8E6FF, // 浅蓝
            0xFFFFDCE5, // 浅粉
            0xFFEBDDFF, // 浅紫
            0xFFFFE3C4, // 浅橙
            0xFFCFF3EE, // 浅青
            0xFFE9E9F0  // 浅灰
    };

    private static final int DARK_SURFACE = 0xFF1B1B1F;

    private NoteColors() {
    }

    /** 色板中的原始颜色，用于设置页的色块预览。 */
    public static int baseColor(int index) {
        if (index < 0 || index >= PALETTE.length) {
            return Color.TRANSPARENT;
        }
        return PALETTE[index];
    }

    /**
     * 当前主题下实际使用的背景颜色。
     *
     * @return 非法下标返回 {@link Color#TRANSPARENT}
     */
    public static int background(Context context, int index) {
        if (index < 0 || index >= PALETTE.length) {
            return Color.TRANSPARENT;
        }
        if (isNight(context)) {
            return blend(PALETTE[index], DARK_SURFACE, 0.82f);
        }
        return PALETTE[index];
    }

    public static boolean isNight(Context context) {
        int mode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int blend(int from, int to, float ratio) {
        int r = (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * ratio);
        int g = (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * ratio);
        int b = (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * ratio);
        return Color.rgb(r, g, b);
    }
}
