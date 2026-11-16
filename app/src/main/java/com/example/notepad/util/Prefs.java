package com.example.notepad.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * 偏好设置：主题、排序方式、编辑器字号、分类列表。
 */
public final class Prefs {

    private static final String FILE = "notepad_prefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_SORT = "sort_order";
    private static final String KEY_FONT_SIZE = "editor_font_size";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_DEFAULT_COLOR = "default_note_color";
    private static final String KEY_PAPER_STYLE = "editor_paper_style";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final int PAPER_PLAIN = 0;
    public static final int PAPER_LINED = 1;

    public static final int SORT_MODIFIED_DESC = 0;
    public static final int SORT_MODIFIED_ASC = 1;
    public static final int SORT_CREATED_DESC = 2;
    public static final int SORT_TITLE_ASC = 3;

    public static final int MIN_FONT_SIZE = 12;
    public static final int MAX_FONT_SIZE = 24;

    private static final String DEFAULT_CATEGORIES = "[\"工作\",\"学习\",\"生活\",\"灵感\"]";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME, THEME_SYSTEM);
    }

    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME, mode).apply();
    }

    public static int getSortOrder(Context context) {
        return prefs(context).getInt(KEY_SORT, SORT_MODIFIED_DESC);
    }

    public static void setSortOrder(Context context, int order) {
        prefs(context).edit().putInt(KEY_SORT, order).apply();
    }

    public static int getEditorFontSize(Context context) {
        int size = prefs(context).getInt(KEY_FONT_SIZE, 18);
        return Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
    }

    public static void setEditorFontSize(Context context, int size) {
        prefs(context).edit().putInt(KEY_FONT_SIZE, size).apply();
    }

    /** 新建笔记时默认使用的便签颜色下标。 */
    public static int getDefaultNoteColor(Context context) {
        return prefs(context).getInt(KEY_DEFAULT_COLOR, 0);
    }

    public static void setDefaultNoteColor(Context context, int colorIndex) {
        prefs(context).edit().putInt(KEY_DEFAULT_COLOR, colorIndex).apply();
    }

    /** 编辑器背景样式：纯色 / 横线纸。 */
    public static int getPaperStyle(Context context) {
        return prefs(context).getInt(KEY_PAPER_STYLE, PAPER_PLAIN);
    }

    public static void setPaperStyle(Context context, int style) {
        prefs(context).edit().putInt(KEY_PAPER_STYLE, style).apply();
    }

    public static List<String> getCategories(Context context) {
        String raw = prefs(context).getString(KEY_CATEGORIES, DEFAULT_CATEGORIES);
        List<String> categories = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "").trim();
                if (!value.isEmpty() && !categories.contains(value)) {
                    categories.add(value);
                }
            }
        } catch (JSONException ignored) {
        }
        if (categories.isEmpty()) {
            categories.add("工作");
            categories.add("学习");
            categories.add("生活");
            categories.add("灵感");
        }
        return categories;
    }

    public static void addCategory(Context context, String category) {
        String value = category == null ? "" : category.trim();
        if (value.isEmpty()) {
            return;
        }
        List<String> categories = getCategories(context);
        if (!categories.contains(value)) {
            categories.add(value);
            JSONArray array = new JSONArray();
            for (String item : categories) {
                array.put(item);
            }
            prefs(context).edit().putString(KEY_CATEGORIES, array.toString()).apply();
        }
    }

    /** 根据偏好设置应用日间 / 夜间主题。 */
    public static void applyTheme(Context context) {
        int nightMode;
        switch (getThemeMode(context)) {
            case THEME_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case THEME_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
