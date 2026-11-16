package com.example.notepad.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notepad.R;
import com.example.notepad.data.Note;
import com.example.notepad.util.NoteColors;
import com.example.notepad.util.TimeUtils;
import com.example.notepad.util.TodoUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface Listener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note);
    }

    private final List<Note> notes = new ArrayList<>();
    private final Listener listener;

    public NoteAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submit(List<Note> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).id;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        Context context = holder.itemView.getContext();

        String title = note.plainTitle();
        if (title.isEmpty()) {
            title = context.getString(R.string.untitled);
        }
        holder.tvTitle.setText(title);

        String preview;
        if (note.type == Note.TYPE_TODO) {
            int done = TodoUtils.doneCount(note.content);
            int total = TodoUtils.totalCount(note.content);
            String first = TodoUtils.firstItemText(note.content);
            preview = context.getString(R.string.todo_preview, done, total);
            if (!first.isEmpty()) {
                preview = preview + " · " + first;
            }
        } else {
            preview = note.previewText();
            if (preview.isEmpty() && note.type == Note.TYPE_IMAGE) {
                preview = context.getString(R.string.image_note);
            }
        }
        holder.tvPreview.setText(preview.isEmpty()
                ? context.getString(R.string.empty_content)
                : preview.replace('\n', ' '));

        String category = note.category == null ? "" : note.category.trim();
        holder.tvCategory.setText(category.isEmpty()
                ? context.getString(R.string.uncategorized)
                : category);

        if (note.type == Note.TYPE_TODO) {
            holder.tvType.setVisibility(View.VISIBLE);
            holder.tvType.setText(R.string.type_todo);
        } else if (note.type == Note.TYPE_IMAGE) {
            holder.tvType.setVisibility(View.VISIBLE);
            holder.tvType.setText(R.string.type_image);
        } else {
            holder.tvType.setVisibility(View.GONE);
        }

        boolean hasImage = note.type == Note.TYPE_IMAGE
                && note.imageUri != null && !note.imageUri.isEmpty();
        if (hasImage) {
            holder.ivThumb.setVisibility(View.VISIBLE);
            try {
                holder.ivThumb.setImageURI(Uri.parse(note.imageUri));
            } catch (Exception e) {
                holder.ivThumb.setImageResource(R.drawable.ic_note_empty);
            }
        } else {
            holder.ivThumb.setVisibility(View.GONE);
            holder.ivThumb.setImageDrawable(null);
        }

        int background = NoteColors.background(context, note.colorIndex);
        if (background == Color.TRANSPARENT) {
            background = MaterialColors.getColor(holder.card,
                    com.google.android.material.R.attr.colorSurface);
        }
        holder.card.setCardBackgroundColor(background);

        holder.tvTime.setText(TimeUtils.formatDateTime(note.modifiedAt));
        holder.ivPin.setVisibility(note.pinned ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onNoteLongClick(note);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvTitle;
        final TextView tvPreview;
        final TextView tvCategory;
        final TextView tvType;
        final TextView tvTime;
        final ImageView ivPin;
        final ImageView ivThumb;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvType = itemView.findViewById(R.id.tvType);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivPin = itemView.findViewById(R.id.ivPin);
            ivThumb = itemView.findViewById(R.id.ivThumb);
        }
    }
}
