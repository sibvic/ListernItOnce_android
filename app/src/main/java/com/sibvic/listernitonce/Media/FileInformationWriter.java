package com.sibvic.listernitonce.Media;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Writes playback information for the media file.
 */
public class FileInformationWriter {

    @NonNull
    private static File getInformationFile(File file) {
        return new File(file.getAbsolutePath() + ".info");
    }

    public static void saveInformation(MediaFile currentFile) {
        File infoFile = getInformationFile(currentFile.getFile());
        try {
            PrintWriter out = new PrintWriter(infoFile);
            out.println(Long.toString(currentFile.getCurrentPosition()));
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
