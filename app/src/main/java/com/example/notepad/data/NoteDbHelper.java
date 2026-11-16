package com.example.notepad.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 SQLite 的笔记存储。所有查询都支持“标题或内容”关键字检索。
 */
public class NoteDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "notepad.db";
    private static final int DB_VERSION = 2;

    public static final String TABLE = "notes";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_CATEGORY = "category";
    public static final String COL_CREATED = "created_at";
    public static final String COL_MODIFIED = "modified_at";
    public static final String COL_PINNED = "pinned";
    public static final String COL_TYPE = "note_type";
    public static final String COL_COLOR = "color_index";
    public static final String COL_IMAGE = "image_uri";

    private static NoteDbHelper sInstance;

    public static synchronized NoteDbHelper get(Context context) {
        if (sInstance == null) {
            sInstance = new NoteDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private NoteDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL DEFAULT '', "
                + COL_CONTENT + " TEXT NOT NULL DEFAULT '', "
                + COL_CATEGORY + " TEXT NOT NULL DEFAULT '', "
                + COL_CREATED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_MODIFIED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_PINNED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_TYPE + " INTEGER NOT NULL DEFAULT 0, "
                + COL_COLOR + " INTEGER NOT NULL DEFAULT -1, "
                + COL_IMAGE + " TEXT NOT NULL DEFAULT '')");
        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 保留旧数据，仅补充多类型笔记所需字段。
            db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_TYPE
                    + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_COLOR
                    + " INTEGER NOT NULL DEFAULT -1");
            db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + COL_IMAGE
                    + " TEXT NOT NULL DEFAULT ''");
        }
    }

    private void seed(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        insertSeed(db, "欢迎使用 NodePad",
                "这是一款支持时间戳、搜索、分类、排序、置顶、主题切换、多类型笔记与备份恢复的记事本。\n\n"
                        + "· 顶部搜索框可同时检索标题和内容\n"
                        + "· 右下角 + 可新建文字、待办或图片笔记\n"
                        + "· 长按列表中的笔记可以置顶或删除\n"
                        + "· 右上角菜单提供排序、类型筛选、导出、备份与偏好设置",
                "学习", Note.TYPE_TEXT, 0, now - 2 * 24 * 60 * 60 * 1000L);
        insertSeed(db, "本周待办", "[ ] 完成移动软件开发期中实验\n[ ] 整理 NotePad 源码分析笔记\n[ ] 复习 SQLite 与 ContentProvider",
                "工作", Note.TYPE_TODO, 1, now - 24 * 60 * 60 * 1000L);
        insertSeed(db, "灵感碎片",
                "好的设计是尽可能少的设计。\n把复杂留给自己，把简单留给用户。",
                "灵感", Note.TYPE_TEXT, 3, now - 30 * 60 * 1000L);
    }

    private void insertSeed(SQLiteDatabase db, String title, String content,
                            String category, int type, int colorIndex, long time) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_CONTENT, content);
        values.put(COL_CATEGORY, category);
        values.put(COL_CREATED, time);
        values.put(COL_MODIFIED, time);
        values.put(COL_PINNED, 0);
        values.put(COL_TYPE, type);
        values.put(COL_COLOR, colorIndex);
        values.put(COL_IMAGE, "");
        db.insert(TABLE, null, values);
    }

    /**
     * @param keyword   标题或内容关键字，空值表示不过滤
     * @param category  分类，null 表示全部分类
     * @param type      笔记类型，{@link Note#TYPE_TEXT} 等；-1 表示全部类型
     * @param sortOrder 见 Prefs.SORT_*
     */
    public List<Note> query(String keyword, String category, int type, int sortOrder) {
        StringBuilder selection = new StringBuilder();
        List<String> args = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            selection.append("(").append(COL_TITLE).append(" LIKE ? OR ")
                    .append(COL_CONTENT).append(" LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (category != null && !category.isEmpty()) {
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            selection.append(COL_CATEGORY).append(" = ?");
            args.add(category);
        }
        if (type >= 0) {
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
            selection.append(COL_TYPE).append(" = ?");
            args.add(String.valueOf(type));
        }

        String orderBy = COL_PINNED + " DESC, " + sortClause(sortOrder) + ", " + COL_ID + " DESC";
        List<Note> notes = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE, null,
                selection.length() == 0 ? null : selection.toString(),
                args.isEmpty() ? null : args.toArray(new String[0]),
                null, null, orderBy)) {
            while (cursor.moveToNext()) {
                notes.add(fromCursor(cursor));
            }
        }
        return notes;
    }

    private String sortClause(int sortOrder) {
        switch (sortOrder) {
            case 1:
                return COL_MODIFIED + " ASC";
            case 2:
                return COL_CREATED + " DESC";
            case 3:
                return "LOWER(" + COL_TITLE + ") ASC";
            case 0:
            default:
                return COL_MODIFIED + " DESC";
        }
    }

    public List<Note> allNotes() {
        return query(null, null, -1, 0);
    }

    public Note get(long id) {
        try (Cursor cursor = getReadableDatabase().query(TABLE, null,
                COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        }
        return null;
    }

    public long insert(Note note) {
        long id = getWritableDatabase().insert(TABLE, null, toValues(note));
        note.id = id;
        return id;
    }

    public void update(Note note) {
        getWritableDatabase().update(TABLE, toValues(note),
                COL_ID + " = ?", new String[]{String.valueOf(note.id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteAll() {
        getWritableDatabase().delete(TABLE, null, null);
    }

    public void setPinned(long id, boolean pinned) {
        ContentValues values = new ContentValues();
        values.put(COL_PINNED, pinned ? 1 : 0);
        getWritableDatabase().update(TABLE, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int count() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    /** 返回已经在使用的分类（去重、按字母排序）。 */
    public List<String> categories() {
        List<String> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(true, TABLE,
                new String[]{COL_CATEGORY}, COL_CATEGORY + " <> ''",
                null, null, null, COL_CATEGORY + " ASC", null)) {
            while (cursor.moveToNext()) {
                String value = cursor.getString(0);
                if (value != null && !value.trim().isEmpty()) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private ContentValues toValues(Note note) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, note.title == null ? "" : note.title);
        values.put(COL_CONTENT, note.content == null ? "" : note.content);
        values.put(COL_CATEGORY, note.category == null ? "" : note.category);
        values.put(COL_CREATED, note.createdAt);
        values.put(COL_MODIFIED, note.modifiedAt);
        values.put(COL_PINNED, note.pinned ? 1 : 0);
        values.put(COL_TYPE, note.type);
        values.put(COL_COLOR, note.colorIndex);
        values.put(COL_IMAGE, note.imageUri == null ? "" : note.imageUri);
        return values;
    }

    private Note fromCursor(Cursor cursor) {
        Note note = new Note();
        note.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        note.title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
        note.content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
        note.category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY));
        note.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED));
        note.modifiedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_MODIFIED));
        note.pinned = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PINNED)) == 1;
        note.type = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TYPE));
        note.colorIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COLOR));
        note.imageUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE));
        return note;
    }
}
