package com.example.notepad.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.notepad.R;
import com.example.notepad.util.NoteColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 便签背景色选择弹窗（仿 ColorNote 的 Select Background Color）。
 */
public final class ColorPickerDialog {

    public interface OnColorPick {
        void onPick(int index);
    }

    private ColorPickerDialog() {
    }

    public static void show(Context context, int selected, OnColorPick callback) {
        int swatchSize = dp(context, 48);
        int margin = dp(context, 6);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 10);
        root.setPadding(padding, padding, padding, padding);

        final AlertDialog[] holder = new AlertDialog[1];
        LinearLayout row = null;
        for (int i = 0; i < NoteColors.COUNT; i++) {
            if (i % 4 == 0) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                root.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            View swatch = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, swatchSize, 1f);
            params.setMargins(margin, margin, margin, margin);
            swatch.setLayoutParams(params);
            swatch.setBackground(buildSwatch(context, i, i == selected));
            swatch.setContentDescription(context.getString(R.string.color_swatch, i + 1));
            final int index = i;
            swatch.setOnClickListener(v -> {
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
                callback.onPick(index);
            });
            row.addView(swatch);
        }

        holder[0] = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.color_title)
                .setView(root)
                .setNegativeButton(R.string.cancel, null)
                .create();
        holder[0].show();
    }

    private static GradientDrawable buildSwatch(Context context, int index, boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(NoteColors.background(context, index));
        if (selected) {
            drawable.setStroke(dp(context, 3),
                    ContextCompat.getColor(context, R.color.brand_primary));
        } else {
            drawable.setStroke(dp(context, 1),
                    ContextCompat.getColor(context, R.color.outline_variant));
        }
        return drawable;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
