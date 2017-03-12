package com.sibvic.listernitonce;

/**
 * Informatioin about the file
 */

class FileInfo {
    private String id;
    private String title;
    private int length;
    private int currentPosition;

    FileInfo(String id, String title, int length, int currentPosition) {
        this.id = id;
        this.title = title;
        this.length = length;
        this.currentPosition = currentPosition;
    }

    void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    int getLength() {
        return length;
    }

    int getCurrentPosition() {
        return currentPosition;
    }
}
