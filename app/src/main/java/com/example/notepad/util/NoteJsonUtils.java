package com.example.notepad.util;

import com.example.notepad.data.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 笔记的 JSON 序列化 / 反序列化，用于导出、备份与恢复。
 */
public final class NoteJsonUtils {

    public static final String APP_TAG = "NodePad";
    private static final int VERSION = 2;

    private NoteJsonUtils() {
    }

    public static String toJson(List<Note> notes) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", APP_TAG);
        root.put("version", VERSION);
        root.put("exportedAt", System.currentTimeMillis());

        JSONArray array = new JSONArray();
        for (Note note : notes) {
            JSONObject item = new JSONObject();
            item.put("title", note.title);
            item.put("content", note.content);
            item.put("category", note.category);
            item.put("createdAt", note.createdAt);
            item.put("modifiedAt", note.modifiedAt);
            item.put("pinned", note.pinned);
            item.put("type", note.type);
            item.put("colorIndex", note.colorIndex);
            item.put("imageUri", note.imageUri == null ? "" : note.imageUri);
            array.put(item);
        }
        root.put("notes", array);
        return root.toString(2);
    }

    public static List<Note> fromJson(String raw) throws JSONException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new JSONException("empty content");
        }
        String text = raw.trim();
        JSONArray array;
        if (text.startsWith("[")) {
            array = new JSONArray(text);
        } else {
            array = new JSONObject(text).optJSONArray("notes");
        }
        if (array == null) {
            throw new JSONException("missing notes array");
        }

        List<Note> notes = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            Note note = new Note();
            note.title = item.optString("title", "");
            note.content = item.optString("content", "");
            note.category = item.optString("category", "");
            note.createdAt = item.optLong("createdAt", now);
            note.modifiedAt = item.optLong("modifiedAt", note.createdAt);
            note.pinned = item.optBoolean("pinned", false);
            note.type = item.optInt("type", Note.TYPE_TEXT);
            note.colorIndex = item.optInt("colorIndex", -1);
            note.imageUri = item.optString("imageUri", "");
            if (note.createdAt <= 0) {
                note.createdAt = now;
            }
            if (note.modifiedAt <= 0) {
                note.modifiedAt = note.createdAt;
            }
            notes.add(note);
        }
        return notes;
    }
}
