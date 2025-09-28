package com.example.remedi;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class NotesAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> titles;
    private final List<String> noteIds;

    public NotesAdapter(Context context, List<String> titles, List<String> noteIds) {
        super(context, R.layout.activity_notes_adapter, titles);
        this.context = context;
        this.titles = titles;
        this.noteIds = noteIds;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(context).inflate(R.layout.activity_notes_adapter, parent, false);
        }

        TextView titleView = row.findViewById(R.id.noteTitle);
        titleView.setText(titles.get(position));

        row.setOnClickListener(v -> {
            Intent intent = new Intent(context, IndividualNotePage.class);
            intent.putExtra("NOTE_ID", noteIds.get(position));
            context.startActivity(intent);
        });

        return row;
    }
}
