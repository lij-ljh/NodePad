package com.example.notepad.util;

import com.example.notepad.data.Note;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 本地备份管理：把全部笔记写入应用外部私有目录，可再通过系统分享上传到网盘实现云备份。
 */
public final class BackupManager {

    private static final int MAX_BACKUPS = 10;
    private static final String SUFFIX = ".json";

    private BackupManager() {
    }

    public static File dir(android.content.Context context) {
        File base = context.getExternalFilesDir(null);
        if (base == null) {
            base = context.getFilesDir();
        }
        File dir = new File(base, "backups");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** 立即备份全部笔记，并只保留最近的 10 份备份。 */
    public static File create(android.content.Context context, List<Note> notes) throws Exception {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File file = new File(dir(context), "notepad-backup-" + stamp + SUFFIX);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(NoteJsonUtils.toJson(notes).getBytes(StandardCharsets.UTF_8));
        }
        prune(context);
        return file;
    }

    public static List<File> list(android.content.Context context) {
        File[] files = dir(context).listFiles((d, name) -> name.endsWith(SUFFIX));
        List<File> backups = new ArrayList<>();
        if (files != null) {
            backups.addAll(Arrays.asList(files));
        }
        backups.sort(Comparator.comparingLong(File::lastModified).reversed());
        return backups;
    }

    public static List<Note> read(File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return NoteJsonUtils.fromJson(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    public static void delete(File file) throws IOException {
        if (file != null && file.exists() && !file.delete()) {
            throw new IOException("cannot delete backup");
        }
    }

    public static String displayName(File file) {
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date(file.lastModified()));
        return stamp + "　·　" + Math.max(1, file.length() / 1024) + " KB";
    }

    private static void prune(android.content.Context context) {
        List<File> backups = list(context);
        for (int i = MAX_BACKUPS; i < backups.size(); i++) {
            // 只保留最近的若干份，删除失败不影响本次备份结果。
            //noinspection ResultOfMethodCallIgnored
            backups.get(i).delete();
        }
    }
}
