package com.sibvic.listernitonce.Media;

import java.io.File;

/**
 * Media file with playing information.
 */
public class MediaFile {
    private File file;
    private long length;
    private long currentPosition;

    public MediaFile(File file, long length, long currentPosition) {
        this.file = file;
        this.length = length;
        this.currentPosition = currentPosition;
    }

    public String getFileName() {
        return file.getName();
    }

    public File getFile() {
        return file;
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

    public void setLength(long length) {
        this.length = length;
    }
}
