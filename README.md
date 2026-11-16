# NodePad 记事本

一个基于 Java + SQLite 的 Android 记事本应用，支持文字、待办、图片三种笔记类型，
具备分类、搜索、排序、便签颜色、主题切换、导出与本地/云端备份恢复等功能。

## 功能特性

- 笔记增删改查，自动记录创建时间与修改时间
- 标题 / 内容关键字搜索，可与分类、类型组合筛选
- 笔记分类：分类筛选、分类标签、编辑器内新建分类
- 多类型笔记：文字笔记、待办清单、图片笔记
- 待办清单：添加、勾选、删除，列表显示完成进度
- 便签美化：8 种便签背景色、横线纸背景、浅色 / 深色主题
- 排序：修改时间升降序、创建时间、标题；支持置顶
- 数据导出 / 导入（JSON，SAF 选择文件，无需存储权限）
- 本地备份与恢复：自动保留最近 10 份，支持合并或覆盖恢复
- 云备份：备份文件可一键分享到网盘、聊天工具
- 偏好设置：主题、默认排序、正文字号、默认便签颜色、背景样式

## 界面截图

| 笔记列表 | 待办清单 | 偏好设置 |
| :---: | :---: | :---: |
| ![列表](docs/screenshots/list.png) | ![待办](docs/screenshots/todo.png) | ![设置](docs/screenshots/settings.png) |

> 建议把截图放在 `docs/screenshots/` 目录，首屏截图最能决定别人是否继续看。

## 技术栈

- 语言：Java
- 数据存储：SQLite（SQLiteOpenHelper）
- UI：Material 3、RecyclerView、Chip、MaterialCardView
- 文件访问：Storage Access Framework、FileProvider
- 构建：Gradle 8.11.1、AGP 8.7.3
- SDK：compileSdk 35、minSdk 24、targetSdk 35

## 项目结构

```text
app/src/main/java/com/example/notepad/
├── MainActivity.java          # 列表、搜索、筛选、备份恢复
├── NoteEditorActivity.java    # 文字 / 待办 / 图片编辑
├── SettingsActivity.java      # 偏好设置
├── data/
│   ├── Note.java              # 笔记模型
│   └── NoteDbHelper.java      # SQLite 建表、迁移、查询
├── ui/
│   ├── NoteAdapter.java       # 列表适配器
│   ├── ColorPickerDialog.java # 便签颜色选择
│   └── LinedPaperDrawable.java# 横线纸背景
└── util/
    ├── Prefs.java             # 偏好设置
    ├── TodoUtils.java         # 待办文本解析
    ├── NoteColors.java        # 色板与深色适配
    ├── NoteJsonUtils.java     # JSON 序列化
    └── BackupManager.java     # 本地备份管理
```

## 运行方式

1. 用 Android Studio 打开项目根目录，等待 Gradle Sync 完成
2. Gradle JDK 选择 JDK 17
3. 选择模拟器或真机，运行 `app` 模块

命令行构建：

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 实现说明

- 数据库版本为 2，升级时使用 `ALTER TABLE` 增量添加字段，旧笔记不会丢失
- 待办内容以 `[ ] 条目` / `[x] 条目` 的纯文本格式存储，便于导出与备份
- 备份文件保存在应用外部私有目录 `backups/`，通过 FileProvider 分享到网盘
- 便签背景色在深色主题下自动调暗，保证文字可读性

## 作者

- 姓名 / 学号
- 课程：移动软件开发
