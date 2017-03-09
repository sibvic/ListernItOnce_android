package com.sibvic.listernitonce.Media;

import android.media.MediaMetadataRetriever;

import java.io.File;
import java.util.Locale;

/**
 * Media file with playing information.
 */
public class MediaFile {
    private File file;
    private long length;
    private long currentPosition;
    private String title;

    MediaFile(File file) {
        this.file = file;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(file.getAbsolutePath());
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        length = Long.parseLong(time) / 1000;
        updateTitle(retriever);

        this.currentPosition = FileInformationReader.readPosition(this);
    }

    private void updateTitle(MediaMetadataRetriever retriever) {
        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        if (title == null && artist == null) {
            this.title = file.getName();
        }
        else {
            this.title = String.format(Locale.getDefault(), "%1$s - %2$s",
                    artist == null ? "" : artist,
                    title == null ? "" : title);
        }
    }

    public String getTitle() { return title; }

    public File getFile() {
        return file;
    }

    public File getMetaInformationFile() {
        return new File(file.getAbsolutePath() + ".info");
    }

    public long getLength() {
        return length;
    }

    public long getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(long currentPosition) {
        this.currentPosition = currentPosition;
    }
}
