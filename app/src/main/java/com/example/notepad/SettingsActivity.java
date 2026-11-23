package com.example.notepad;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.notepad.ui.ColorPickerDialog;
import com.example.notepad.ui.CuteBackgroundDrawable;
import com.example.notepad.util.NoteColors;
import com.example.notepad.util.Prefs;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvFontSizeLabel;
    private TextView tvFontPreview;
    private SeekBar seekFontSize;
    private View viewDefaultColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.settingsRoot).setBackground(new CuteBackgroundDrawable(this));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupTheme();
        setupNoteStyle();
        setupPaper();
        setupSort();
        setupFontSize();
    }

    private void setupTheme() {
        RadioGroup group = findViewById(R.id.rgTheme);
        int theme = Prefs.getThemeMode(this);
        int checked = theme == Prefs.THEME_LIGHT ? R.id.rbThemeLight
                : theme == Prefs.THEME_DARK ? R.id.rbThemeDark : R.id.rbThemeSystem;
        group.check(checked);

        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            int mode;
            if (checkedId == R.id.rbThemeLight) {
                mode = Prefs.THEME_LIGHT;
            } else if (checkedId == R.id.rbThemeDark) {
                mode = Prefs.THEME_DARK;
            } else {
                mode = Prefs.THEME_SYSTEM;
            }
            if (mode != Prefs.getThemeMode(this)) {
                Prefs.setThemeMode(this, mode);
                Prefs.applyTheme(this);
            }
        });
    }

    private void setupNoteStyle() {
        viewDefaultColor = findViewById(R.id.viewDefaultColor);
        updateDefaultColorSwatch();

        findViewById(R.id.rowDefaultColor).setOnClickListener(v ->
                ColorPickerDialog.show(this, Prefs.getDefaultNoteColor(this), index -> {
                    Prefs.setDefaultNoteColor(this, index);
                    updateDefaultColorSwatch();
                }));
    }

    private void updateDefaultColorSwatch() {
        if (viewDefaultColor == null) {
            return;
        }
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(NoteColors.background(this, Prefs.getDefaultNoteColor(this)));
        drawable.setStroke(dp(1), ContextCompat.getColor(this, R.color.outline_variant));
        viewDefaultColor.setBackground(drawable);
    }

    private void setupPaper() {
        RadioGroup group = findViewById(R.id.rgPaper);
        int style = Prefs.getPaperStyle(this);
        group.check(style == Prefs.PAPER_LINED ? R.id.rbPaperLined : R.id.rbPaperPlain);
        group.setOnCheckedChangeListener((radioGroup, checkedId) ->
                Prefs.setPaperStyle(this,
                        checkedId == R.id.rbPaperLined ? Prefs.PAPER_LINED : Prefs.PAPER_PLAIN));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setupSort() {
        RadioGroup group = findViewById(R.id.rgSort);
        int sort = Prefs.getSortOrder(this);
        int checked = sort == Prefs.SORT_MODIFIED_ASC ? R.id.rbSortModifiedAsc
                : sort == Prefs.SORT_CREATED_DESC ? R.id.rbSortCreatedDesc
                : sort == Prefs.SORT_TITLE_ASC ? R.id.rbSortTitleAsc
                : R.id.rbSortModifiedDesc;
        group.check(checked);

        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            int order;
            if (checkedId == R.id.rbSortModifiedAsc) {
                order = Prefs.SORT_MODIFIED_ASC;
            } else if (checkedId == R.id.rbSortCreatedDesc) {
                order = Prefs.SORT_CREATED_DESC;
            } else if (checkedId == R.id.rbSortTitleAsc) {
                order = Prefs.SORT_TITLE_ASC;
            } else {
                order = Prefs.SORT_MODIFIED_DESC;
            }
            Prefs.setSortOrder(this, order);
        });
    }

    private void setupFontSize() {
        tvFontSizeLabel = findViewById(R.id.tvFontSizeLabel);
        tvFontPreview = findViewById(R.id.tvFontPreview);
        seekFontSize = findViewById(R.id.seekFontSize);
        seekFontSize.setMax(Prefs.MAX_FONT_SIZE - Prefs.MIN_FONT_SIZE);

        int fontSize = Prefs.getEditorFontSize(this);
        seekFontSize.setProgress(fontSize - Prefs.MIN_FONT_SIZE);
        applyFontPreview(fontSize);

        seekFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = Prefs.MIN_FONT_SIZE + progress;
                applyFontPreview(size);
                if (fromUser) {
                    Prefs.setEditorFontSize(SettingsActivity.this, size);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void applyFontPreview(int size) {
        tvFontSizeLabel.setText(getString(R.string.font_size_label, size));
        tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
