package com.sibvic.listernitonce;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sibvic.listernitonce.Media.MediaFile;

/**
 * List adapter for the media file.
 */
public class FileListAdapter extends ArrayAdapter<MediaFile> {
    private final Context context;
    private final MediaFile[] values;

    public FileListAdapter(Context context, MediaFile[] values) {
        super(context, R.layout.row_layout, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.row_layout, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        MediaFile mediaFile = values[position];
        textView.setText(String.format("%1$s (%2$d)", mediaFile.getFileName(), mediaFile.getLength()));
        return rowView;
    }
}
