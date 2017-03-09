package com.sibvic.listernitonce;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sibvic.listernitonce.Media.MediaFile;

import java.util.ArrayList;
import java.util.Locale;

/**
 * List adapter for the media file.
 */
class FileListAdapter extends ArrayAdapter<MediaFile> {
    private final Context context;
    private final ArrayList<MediaFile> values;

    FileListAdapter(Context context, ArrayList<MediaFile> values) {
        super(context, R.layout.row_layout, values);
        this.context = context;
        this.values = values;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.row_layout, parent, false);
        updateRow(position, rowView);
        return rowView;
    }

    void updateRow(int position, View rowView) {
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        MediaFile mediaFile = values.get(position);
        long length = mediaFile.getLength();
        if (length > 0) {
            long currentPosition = mediaFile.getCurrentPosition();
            int progress = (int) ((currentPosition * 100.0) / length);
            textView.setText(String.format(Locale.getDefault(), "%1$s (%2$d%%, %3$s)", mediaFile.getFileName(),
                    progress, formatTime(length)));
        }
        else {
            textView.setText(String.format(Locale.getDefault(), "%1$s (%2$d)", mediaFile.getFileName(),
                    mediaFile.getCurrentPosition()));
        }
    }

    private String formatTime(long timeInSeconds) {
        //TODO: format in hh:mm:ss format
        return Long.toString(timeInSeconds);
    }
}
