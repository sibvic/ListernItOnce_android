package com.sibvic.listernitonce

import java.util.ArrayList

/**
 * Contains list of FileInfo
 */

internal class FileInfoList {
    private val _listItems = ArrayList<FileInfo>()

    fun clear() {
        _listItems.clear()
    }

    fun indexOf(fileInfo: FileInfo): Int {
        return _listItems.indexOf(fileInfo)
    }

    fun remove(index: Int) {
        _listItems.removeAt(index)
    }

    fun add(fileInfo: FileInfo) {
        _listItems.add(fileInfo)
    }

    fun toArrayList(): ArrayList<FileInfo> {
        return _listItems
    }

    fun findFileById(fileId: String): FileInfo? {
        for (file in _listItems) {
            if (file.id == fileId) {
                return file
            }
        }
        return null
    }
}
