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
        long position = readPosition(file);
        long lengthInSeconds = readLength(file);
        return new MediaFile(file, lengthInSeconds, position);
    }

    private static long readLength(File file) {
        //TODO: implement
        return 0;
    }

    private static long readPosition(File file) {
        File infoFile = getInformationFile(file);
        if (!infoFile.exists() || infoFile.isDirectory()) {
            return 0;
        }
        //TODO: read data
        return 0;
    }

    @NonNull
    private static File getInformationFile(File file) {
        return new File(file.getAbsolutePath() + ".info");
    }

    public static void saveInformation(MediaFile currentFile) {
        File infoFile = getInformationFile(currentFile.getFile());
        //TODO: save
    }
}
