package com.example.notepad;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.notepad.data.Note;
import com.example.notepad.data.NoteDbHelper;
import com.example.notepad.ui.ColorPickerDialog;
import com.example.notepad.ui.LinedPaperDrawable;
import com.example.notepad.util.NoteColors;
import com.example.notepad.util.Prefs;
import com.example.notepad.util.TimeUtils;
import com.example.notepad.util.TodoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class NoteEditorActivity extends AppCompatActivity {

    private static final String EXTRA_NOTE_ID = "extra_note_id";
    private static final String EXTRA_NOTE_TYPE = "extra_note_type";

    private NoteDbHelper db;
    private Note note;
    private String category = "";
    private boolean pinned;
    private boolean dirty;
    private boolean deleted;

    private int type = Note.TYPE_TEXT;
    private int colorIndex = -1;
    private String imageUri = "";

    private TextInputEditText etTitle;
    private EditText etContent;
    private TextView tvMeta;
    private Chip chipCategory;
    private Chip chipType;
    private ImageButton btnPin;
    private LinearLayout contentContainer;

    private LinearLayout todoSection;
    private LinearLayout todoList;
    private TextView tvTodoEmpty;
    private TextInputEditText etTodoInput;
    private List<TodoUtils.Item> todoItems = new ArrayList<>();

    private MaterialCardView imageCard;
    private ImageView ivNoteImage;
    private MaterialButton btnPickImage;

    private ActivityResultLauncher<String[]> imageLauncher;

    public static Intent newIntent(Context context, long noteId) {
        return newIntent(context, noteId, Note.TYPE_TEXT);
    }

    public static Intent newIntent(Context context, long noteId, int noteType) {
        Intent intent = new Intent(context, NoteEditorActivity.class);
        intent.putExtra(EXTRA_NOTE_ID, noteId);
        intent.putExtra(EXTRA_NOTE_TYPE, noteType);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = NoteDbHelper.get(this);
        long noteId = getIntent().getLongExtra(EXTRA_NOTE_ID, -1);
        Note loaded = noteId > 0 ? db.get(noteId) : null;
        if (loaded == null) {
            note = new Note();
            note.createdAt = System.currentTimeMillis();
            note.modifiedAt = note.createdAt;
            type = getIntent().getIntExtra(EXTRA_NOTE_TYPE, Note.TYPE_TEXT);
            colorIndex = Prefs.getDefaultNoteColor(this);
        } else {
            note = loaded;
            type = note.type;
            colorIndex = note.colorIndex;
            imageUri = note.imageUri == null ? "" : note.imageUri;
        }
        pinned = note.pinned;

        contentContainer = findViewById(R.id.contentContainer);
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        tvMeta = findViewById(R.id.tvMeta);
        chipCategory = findViewById(R.id.chipCategory);
        chipType = findViewById(R.id.chipType);
        btnPin = findViewById(R.id.btnPin);

        todoSection = findViewById(R.id.todoSection);
        todoList = findViewById(R.id.todoList);
        tvTodoEmpty = findViewById(R.id.tvTodoEmpty);
        etTodoInput = findViewById(R.id.etTodoInput);

        imageCard = findViewById(R.id.imageCard);
        ivNoteImage = findViewById(R.id.ivNoteImage);
        btnPickImage = findViewById(R.id.btnPickImage);

        imageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onImagePicked);

        populate();
        setupTypeUi();
        applyBackground();

        etContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, Prefs.getEditorFontSize(this));

        AfterTextChanged dirtyWatcher = new AfterTextChanged() {
            @Override
            public void afterTextChanged(Editable s) {
                dirty = true;
            }
        };
        etTitle.addTextChangedListener(dirtyWatcher);
        etContent.addTextChangedListener(dirtyWatcher);

        chipCategory.setOnClickListener(v -> showCategoryDialog());
        btnPin.setOnClickListener(v -> {
            pinned = !pinned;
            dirty = true;
            updatePinIcon();
        });
        findViewById(R.id.btnAddTodo).setOnClickListener(v -> addTodoItem());
        etTodoInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTodoItem();
                return true;
            }
            return false;
        });
        btnPickImage.setOnClickListener(v ->
                imageLauncher.launch(new String[]{"image/*"}));
    }

    private void populate() {
        etTitle.setText(note.title);
        etContent.setText(note.content);
        etTitle.setSelection(etTitle.length());

        category = note.category == null ? "" : note.category;
        if (!category.isEmpty()) {
            Prefs.addCategory(this, category);
        }
        chipCategory.setText(category.isEmpty()
                ? getString(R.string.uncategorized) : category);

        updatePinIcon();
        updateMeta();
    }

    private void setupTypeUi() {
        chipType.setText(typeLabel(type));
        etContent.setVisibility(type == Note.TYPE_TODO ? View.GONE : View.VISIBLE);
        todoSection.setVisibility(type == Note.TYPE_TODO ? View.VISIBLE : View.GONE);
        imageCard.setVisibility(type == Note.TYPE_IMAGE ? View.VISIBLE : View.GONE);

        if (type == Note.TYPE_TODO) {
            todoItems = new ArrayList<>(TodoUtils.parse(note.content));
            rebuildTodoList();
        }
        if (type == Note.TYPE_IMAGE) {
            updateImageSection();
        }
    }

    private String typeLabel(int value) {
        switch (value) {
            case Note.TYPE_TODO:
                return getString(R.string.type_todo);
            case Note.TYPE_IMAGE:
                return getString(R.string.type_image);
            case Note.TYPE_TEXT:
            default:
                return getString(R.string.type_text);
        }
    }

    private void applyBackground() {
        int background = NoteColors.background(this, colorIndex);
        if (background == Color.TRANSPARENT) {
            background = ContextCompat.getColor(this, R.color.surface);
        }
        if (Prefs.getPaperStyle(this) == Prefs.PAPER_LINED) {
            contentContainer.setBackground(new LinedPaperDrawable(this, background));
        } else {
            contentContainer.setBackgroundColor(background);
        }
    }

    private void updateImageSection() {
        boolean hasImage = imageUri != null && !imageUri.isEmpty();
        btnPickImage.setText(hasImage ? R.string.replace_image : R.string.pick_image);
        if (hasImage) {
            try {
                ivNoteImage.setImageURI(Uri.parse(imageUri));
            } catch (Exception e) {
                ivNoteImage.setImageResource(R.drawable.ic_note_empty);
            }
        } else {
            ivNoteImage.setImageResource(R.drawable.ic_note_empty);
        }
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // 部分文件提供方不支持持久授权，本次会话内仍可正常显示。
        }
        imageUri = uri.toString();
        dirty = true;
        updateImageSection();
    }

    private void rebuildTodoList() {
        todoList.removeAllViews();
        for (int i = 0; i < todoItems.size(); i++) {
            final TodoUtils.Item item = todoItems.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_todo, todoList, false);
            CheckBox checkBox = row.findViewById(R.id.cbTodo);
            checkBox.setText(item.text);
            checkBox.setChecked(item.done);
            checkBox.setOnCheckedChangeListener((button, checked) -> {
                item.done = checked;
                dirty = true;
            });
            row.findViewById(R.id.btnDeleteTodo).setOnClickListener(v -> {
                todoItems.remove(item);
                dirty = true;
                rebuildTodoList();
            });
            todoList.addView(row);
        }
        tvTodoEmpty.setVisibility(todoItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void addTodoItem() {
        String text = etTodoInput.getText() == null
                ? "" : etTodoInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        todoItems.add(new TodoUtils.Item(text, false));
        etTodoInput.setText("");
        dirty = true;
        rebuildTodoList();
    }

    private void updatePinIcon() {
        int color = ContextCompat.getColor(this,
                pinned ? R.color.brand_primary : R.color.pin_inactive);
        btnPin.setImageTintList(ColorStateList.valueOf(color));
        btnPin.setContentDescription(getString(pinned ? R.string.unpin : R.string.pin_note));
    }

    private void updateMeta() {
        if (note.id <= 0) {
            tvMeta.setText(R.string.meta_new_note);
            return;
        }
        tvMeta.setText(getString(R.string.meta_template,
                TimeUtils.formatDateTime(note.createdAt),
                TimeUtils.formatDateTime(note.modifiedAt)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save) {
            persist();
            finish();
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        } else if (id == R.id.action_color) {
            ColorPickerDialog.show(this, colorIndex, index -> {
                colorIndex = index;
                dirty = true;
                applyBackground();
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dirty) {
            persist();
        }
    }

    private void persist() {
        if (deleted) {
            return;
        }
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        String content = type == Note.TYPE_TODO
                ? TodoUtils.serialize(todoItems)
                : (etContent.getText() == null ? "" : etContent.getText().toString());
        if (note.id <= 0 && title.isEmpty() && content.trim().isEmpty()
                && (imageUri == null || imageUri.isEmpty())) {
            return;
        }

        long now = System.currentTimeMillis();
        note.title = title;
        note.content = content;
        note.category = category;
        note.pinned = pinned;
        note.type = type;
        note.colorIndex = colorIndex;
        note.imageUri = imageUri == null ? "" : imageUri;
        note.modifiedAt = now;

        if (note.id <= 0) {
            note.createdAt = now;
            db.insert(note);
        } else {
            db.update(note);
        }
        dirty = false;
        updateMeta();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_note_title)
                .setMessage(R.string.delete_note_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleted = true;
                    if (note.id > 0) {
                        db.delete(note.id);
                    }
                    Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCategoryDialog() {
        List<String> categories = Prefs.getCategories(this);
        if (!category.isEmpty() && !categories.contains(category)) {
            categories.add(category);
        }
        String[] items = new String[categories.size() + 1];
        for (int i = 0; i < categories.size(); i++) {
            items[i] = categories.get(i);
        }
        items[categories.size()] = getString(R.string.new_category);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pick_category)
                .setItems(items, (dialog, which) -> {
                    if (which == categories.size()) {
                        showNewCategoryDialog();
                    } else {
                        setCategory(categories.get(which));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showNewCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.new_category_hint);
        input.setSingleLine(true);

        FrameLayout container = new FrameLayout(this);
        int padding = dp(20);
        container.setPadding(padding, dp(8), padding, 0);
        container.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_category)
                .setView(container)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError(getString(R.string.category_name_required));
                return;
            }
            if (Prefs.getCategories(this).contains(name)) {
                input.setError(getString(R.string.category_exists));
                return;
            }
            Prefs.addCategory(this, name);
            setCategory(name);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void setCategory(String value) {
        category = value == null ? "" : value;
        chipCategory.setText(category.isEmpty()
                ? getString(R.string.uncategorized) : category);
        dirty = true;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private abstract static class AfterTextChanged implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
