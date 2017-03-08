package com.sibvic.listernitonce.Media;

import android.support.annotation.NonNull;

import java.io.File;

/**
 * Creates media file from a file.
 * Position is stored in the .info file.
 */
public class FileFactory {
    public static boolean isMediaFile(File file) {
        try {
            String extension = getFileExtension(file);
            return extension.equalsIgnoreCase("mp3");
        } catch (NullPointerException e) {
            //shouldn't happen
            return false;
        }
        catch (IllegalArgumentException e) {
            //shouldn't happen
            return false;
        }
    }

    private static String getFileExtension(File file) {
        if (file == null) {
            throw new NullPointerException("file argument was null");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("getFileExtension(File file)"
                    + " called on File object that wasn't an actual file"
                    + " (perhaps a directory or device?). file had path: "
                    + file.getAbsolutePath());
        }
        String fileName = file.getName();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        } else {
            return "";
        }
    }

    @NonNull
    public static MediaFile getMediaFile(File file) {
        long position = FileInformationReader.readPosition(file);
        long lengthInSeconds = FileInformationReader.readLength(file);
        return new MediaFile(file, lengthInSeconds, position);
    }
}
