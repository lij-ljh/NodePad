package com.example.notepad.data;

/**
 * 一条笔记的数据模型。
 */
public class Note {
    /** 纯文字笔记。 */
    public static final int TYPE_TEXT = 0;
    /** 待办清单。 */
    public static final int TYPE_TODO = 1;
    /** 图片笔记。 */
    public static final int TYPE_IMAGE = 2;

    public long id;
    public String title = "";
    public String content = "";
    public String category = "";
    public long createdAt;
    public long modifiedAt;
    public boolean pinned;

    /** 笔记类型，见 TYPE_*。 */
    public int type = TYPE_TEXT;
    /** 便签背景颜色下标，-1 表示使用默认样式。 */
    public int colorIndex = -1;
    /** 图片笔记的图片 URI。 */
    public String imageUri = "";

    public String plainTitle() {
        return title == null ? "" : title.trim();
    }

    public String previewText() {
        if (content == null) {
            return "";
        }
        return content.replace('\n', ' ').trim();
    }
}
