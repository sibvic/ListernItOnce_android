package com.sibvic.listernitonce.Media;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;

/**
 * Creates media file from a file.
 * Position is stored in the .info file.
 */
public class FileFactory {
    /**
     * Check whether file is a media file.
     * @param file Input file
     * @return true if the input file is a media file.
     */
    private static boolean isMediaFile(File file) {
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

    /**
     * Get file extension without separator.
     * @param file Input file
     * @return Extension without separator.
     */
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

    /**
     * Adds files from the specified folder including all subfolders.
     * @param files Out parameter
     * @param folder Folder to scan.
     */
    public static void addFilesFromFolder(ArrayList<MediaFile> files, File folder) {
        File[] filesInFolder = folder.listFiles();
        if (filesInFolder == null) {
            return;
        }
        for (File file : filesInFolder) {
            if (file.isDirectory()) {
                addFilesFromFolder(files, file);
                continue;
            }
            if (FileFactory.isMediaFile(file)) {
                files.add(FileFactory.getMediaFile(file));
            }
        }
    }

    /**
     * Get media file from the file
     * @param file Input file
     * @return Media file
     */
    @NonNull
    private static MediaFile getMediaFile(File file) {
        return new MediaFile(file);
    }
}
