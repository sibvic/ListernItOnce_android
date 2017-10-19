package com.sibvic.listernitonce;

import java.util.ArrayList;

/**
 * Contains list of FileInfo
 */

class FileInfoList {
    private ArrayList<FileInfo> _listItems = new ArrayList<>();

    int indexOf(FileInfo fileInfo) {
        return _listItems.indexOf(fileInfo);
    }

    void remove(int index) {
        _listItems.remove(index);
    }

    void add(FileInfo fileInfo) {
        _listItems.add(fileInfo);
    }

    ArrayList<FileInfo> toArrayList() {
        return _listItems;
    }

    FileInfo findFileById(String fileId) {
        for (FileInfo file : _listItems) {
            if (file.getId().equals(fileId)) {
                return file;
            }
        }
        return null;
    }
}
