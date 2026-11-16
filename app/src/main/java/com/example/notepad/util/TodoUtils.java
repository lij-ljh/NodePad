package com.example.notepad.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 待办清单的文本格式工具：每一项占一行，已完成项以 "[x] " 开头，未完成项以 "[ ] " 开头。
 */
public final class TodoUtils {

    public static final class Item {
        public String text;
        public boolean done;

        public Item(String text, boolean done) {
            this.text = text;
            this.done = done;
        }
    }

    private TodoUtils() {
    }

    public static List<Item> parse(String raw) {
        List<Item> items = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return items;
        }
        for (String line : raw.split("\n")) {
            String value = line.trim();
            if (value.isEmpty()) {
                continue;
            }
            boolean done = false;
            if (value.startsWith("[x]") || value.startsWith("[X]")) {
                done = true;
                value = value.substring(3).trim();
            } else if (value.startsWith("[ ]")) {
                value = value.substring(3).trim();
            }
            if (!value.isEmpty()) {
                items.add(new Item(value, done));
            }
        }
        return items;
    }

    public static String serialize(List<Item> items) {
        StringBuilder builder = new StringBuilder();
        for (Item item : items) {
            if (item.text == null || item.text.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(item.done ? "[x] " : "[ ] ").append(item.text.trim());
        }
        return builder.toString();
    }

    public static int totalCount(String raw) {
        return parse(raw).size();
    }

    public static int doneCount(String raw) {
        int done = 0;
        for (Item item : parse(raw)) {
            if (item.done) {
                done++;
            }
        }
        return done;
    }

    public static String firstItemText(String raw) {
        List<Item> items = parse(raw);
        return items.isEmpty() ? "" : items.get(0).text;
    }
}
