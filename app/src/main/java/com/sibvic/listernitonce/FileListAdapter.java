package com.sibvic.listernitonce;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

/**
 * List adapter for the media file.
 */
class FileListAdapter extends ArrayAdapter<FileInfo> {
    private final Context context;
    private final ArrayList<FileInfo> values;

    FileListAdapter(Context context, ArrayList<FileInfo> values) {
        super(context, R.layout.row_layout, values);
        this.context = context;
        this.values = values;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_layout, parent, false);
        }
        updateRow(position, convertView);
        return convertView;
    }

    void updateRow(int position, View rowView) {
        FileInfo mediaFile = values.get(position);

        TextView textView = (TextView) rowView.findViewById(R.id.label);
        textView.setText(mediaFile.getTitle());

        TextView timeView = (TextView) rowView.findViewById(R.id.time);
        long length = mediaFile.getLength();
        long currentPosition = mediaFile.getCurrentPosition();
        if (length > 0) {
            int progress = (int) ((currentPosition * 100.0) / length);
            timeView.setText(String.format(Locale.getDefault(), "%3$s/%2$s, %1$d%%",
                    progress, formatTimeLength(length), formatTimeLength(currentPosition)));
        }
        else {
            timeView.setText(formatTimeLength(currentPosition));
        }

        ProgressBar progressBar = (ProgressBar) rowView.findViewById(R.id.progress);
        progressBar.setMax((int)length);
        progressBar.setProgress((int)currentPosition);
    }

    private String formatTimeLength(long timeInSeconds) {
        int seconds = (int)timeInSeconds % 60;
        int timeInMinutes = (int)timeInSeconds / 60;
        int minutes = timeInMinutes % 60;
        int hours = timeInMinutes / 60;
        return String.format(Locale.getDefault(), "%1$d:%2$02d:%3$02d", hours, minutes, seconds);
    }
}
