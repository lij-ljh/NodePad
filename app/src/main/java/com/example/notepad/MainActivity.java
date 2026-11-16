package com.example.notepad;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notepad.data.Note;
import com.example.notepad.data.NoteDbHelper;
import com.example.notepad.ui.NoteAdapter;
import com.example.notepad.util.BackupManager;
import com.example.notepad.util.NoteJsonUtils;
import com.example.notepad.util.Prefs;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NoteAdapter.Listener {

    private NoteDbHelper db;
    private NoteAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyView;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private TextInputEditText etSearch;
    private ChipGroup chipGroup;
    private Toolbar toolbar;

    private String selectedCategory;
    private int typeFilter = -1;
    private boolean buildingChips;
    private int loadToken;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ActivityResultLauncher<String> exportLauncher;
    private ActivityResultLauncher<String[]> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = NoteDbHelper.get(this);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        etSearch = findViewById(R.id.etSearch);
        chipGroup = findViewById(R.id.chipGroup);
        emptyView = findViewById(R.id.emptyView);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(this);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v -> showNewNoteChooser());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                reload();
            }
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (buildingChips || checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            selectedCategory = chip == null || chip.getTag() == null
                    ? null : chip.getTag().toString();
            reload();
        });

        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                this::onExportTarget);
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::onImportTarget);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildCategoryChips();
        reload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (id == R.id.action_filter_type) {
            showTypeFilterDialog();
            return true;
        } else if (id == R.id.action_backup) {
            backupNow();
            return true;
        } else if (id == R.id.action_restore) {
            showRestoreList();
            return true;
        } else if (id == R.id.action_export) {
            exportLauncher.launch(getString(R.string.export_file_name));
            return true;
        } else if (id == R.id.action_import) {
            importLauncher.launch(new String[]{
                    "application/json", "text/plain", "application/octet-stream"});
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reload() {
        final int token = ++loadToken;
        final String keyword = etSearch.getText() == null ? "" : etSearch.getText().toString();
        final String category = selectedCategory;
        final int sortOrder = Prefs.getSortOrder(this);

        io.execute(() -> {
            List<Note> notes = db.query(keyword, category, typeFilter, sortOrder);
            int total = db.count();
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed() || token != loadToken) {
                    return;
                }
                adapter.submit(notes);

                boolean empty = notes.isEmpty();
                boolean filtering = !keyword.trim().isEmpty() || category != null || typeFilter >= 0;
                emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                tvEmptyTitle.setText(filtering ? R.string.no_result_title : R.string.empty_title);
                tvEmptySubtitle.setText(filtering ? R.string.no_result_subtitle : R.string.empty_subtitle);

                if (filtering) {
                    toolbar.setSubtitle(getString(R.string.notes_count_filtered, notes.size()));
                } else {
                    toolbar.setSubtitle(getString(R.string.notes_count, total));
                }
            });
        });
    }

    private void buildCategoryChips() {
        buildingChips = true;
        chipGroup.removeAllViews();

        Chip allChip = inflateChip(getString(R.string.category_all), null);
        chipGroup.addView(allChip);

        boolean matched = false;
        for (String category : db.categories()) {
            Chip chip = inflateChip(category, category);
            chipGroup.addView(chip);
            if (category.equals(selectedCategory)) {
                chip.setChecked(true);
                matched = true;
            }
        }
        if (!matched) {
            selectedCategory = null;
            allChip.setChecked(true);
        }
        buildingChips = false;
    }

    private Chip inflateChip(String text, String tag) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_category, chipGroup, false);
        chip.setText(text);
        chip.setTag(tag);
        return chip;
    }

    private void showSortDialog() {
        String[] options = {
                getString(R.string.sort_modified_desc),
                getString(R.string.sort_modified_asc),
                getString(R.string.sort_created_desc),
                getString(R.string.sort_title_asc)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sort_title)
                .setSingleChoiceItems(options, Prefs.getSortOrder(this), (dialog, which) -> {
                    Prefs.setSortOrder(this, which);
                    dialog.dismiss();
                    reload();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showNewNoteChooser() {
        String[] options = {
                getString(R.string.new_text_note),
                getString(R.string.new_todo_note),
                getString(R.string.new_image_note)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_note_type)
                .setItems(options, (dialog, which) ->
                        startActivity(NoteEditorActivity.newIntent(this, -1, which)))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showTypeFilterDialog() {
        String[] options = {
                getString(R.string.type_all),
                getString(R.string.type_text),
                getString(R.string.type_todo),
                getString(R.string.type_image)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.filter_type_title)
                .setSingleChoiceItems(options, typeFilter + 1, (dialog, which) -> {
                    typeFilter = which - 1;
                    dialog.dismiss();
                    reload();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void backupNow() {
        io.execute(() -> {
            try {
                List<Note> notes = db.allNotes();
                File file = BackupManager.create(this, notes);
                mainHandler.post(() -> new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.backup_success_title)
                        .setMessage(getString(R.string.backup_success_message,
                                notes.size(), BackupManager.displayName(file)))
                        .setPositiveButton(R.string.share_backup,
                                (dialog, which) -> shareBackup(file))
                        .setNegativeButton(R.string.close, null)
                        .show());
            } catch (Exception e) {
                mainHandler.post(() -> toast(getString(R.string.backup_failed)));
            }
        });
    }

    private void showRestoreList() {
        io.execute(() -> {
            List<File> backups = BackupManager.list(this);
            mainHandler.post(() -> {
                if (backups.isEmpty()) {
                    toast(getString(R.string.no_backup));
                    return;
                }
                String[] items = new String[backups.size()];
                for (int i = 0; i < backups.size(); i++) {
                    items[i] = BackupManager.displayName(backups.get(i));
                }
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.restore_title)
                        .setItems(items, (dialog, which) -> confirmRestore(backups.get(which)))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        });
    }

    private void confirmRestore(File file) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_confirm_title)
                .setMessage(R.string.restore_confirm_message)
                .setPositiveButton(R.string.restore_merge,
                        (dialog, which) -> restoreFrom(file, false))
                .setNeutralButton(R.string.restore_replace,
                        (dialog, which) -> restoreFrom(file, true))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void restoreFrom(File file, boolean replace) {
        io.execute(() -> {
            try {
                List<Note> notes = BackupManager.read(file);
                if (replace) {
                    db.deleteAll();
                }
                for (Note note : notes) {
                    note.id = 0;
                    db.insert(note);
                    if (note.category != null && !note.category.trim().isEmpty()) {
                        Prefs.addCategory(this, note.category.trim());
                    }
                }
                mainHandler.post(() -> {
                    toast(getString(R.string.restore_success, notes.size()));
                    buildCategoryChips();
                    reload();
                });
            } catch (Exception e) {
                mainHandler.post(() -> toast(getString(R.string.restore_failed)));
            }
        });
    }

    private void shareBackup(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(
                    intent, getString(R.string.share_backup_chooser)));
        } catch (Exception e) {
            toast(getString(R.string.share_failed));
        }
    }

    private void onExportTarget(Uri uri) {
        if (uri == null) {
            return;
        }
        io.execute(() -> {
            try {
                List<Note> notes = db.allNotes();
                String json = NoteJsonUtils.toJson(notes);
                try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                    if (output == null) {
                        throw new IllegalStateException("cannot open output stream");
                    }
                    output.write(json.getBytes(StandardCharsets.UTF_8));
                }
                mainHandler.post(() -> toast(getString(R.string.export_success, notes.size())));
            } catch (Exception e) {
                mainHandler.post(() -> toast(getString(R.string.export_failed)));
            }
        });
    }

    private void onImportTarget(Uri uri) {
        if (uri == null) {
            return;
        }
        io.execute(() -> {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    throw new IllegalStateException("cannot open input stream");
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int read;
                while ((read = input.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                List<Note> imported = NoteJsonUtils.fromJson(
                        new String(buffer.toByteArray(), StandardCharsets.UTF_8));
                for (Note note : imported) {
                    db.insert(note);
                }
                mainHandler.post(() -> {
                    toast(getString(R.string.import_success, imported.size()));
                    buildCategoryChips();
                    reload();
                });
            } catch (Exception e) {
                mainHandler.post(() -> toast(getString(R.string.import_failed)));
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNoteClick(Note note) {
        startActivity(NoteEditorActivity.newIntent(this, note.id));
    }

    @Override
    public void onNoteLongClick(Note note) {
        String pinLabel = getString(note.pinned ? R.string.unpin : R.string.pin);
        String[] actions = {pinLabel, getString(R.string.delete)};
        String title = note.plainTitle().isEmpty()
                ? getString(R.string.untitled) : note.plainTitle();
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        io.execute(() -> {
                            db.setPinned(note.id, !note.pinned);
                            mainHandler.post(this::reload);
                        });
                    } else {
                        confirmDelete(note);
                    }
                })
                .show();
    }

    private void confirmDelete(Note note) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_note_title)
                .setMessage(R.string.delete_note_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> io.execute(() -> {
                    db.delete(note.id);
                    mainHandler.post(() -> {
                        toast(getString(R.string.note_deleted));
                        reload();
                    });
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
