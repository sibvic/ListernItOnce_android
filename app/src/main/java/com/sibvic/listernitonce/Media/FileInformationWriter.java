package com.sibvic.listernitonce.Media;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Writes playback information for the media file.
 */
public class FileInformationWriter {

    public static void saveInformation(MediaFile currentFile) {
        File infoFile = currentFile.getMetaInformationFile();
        try {
            PrintWriter out = new PrintWriter(infoFile);
            out.println(Long.toString(currentFile.getCurrentPosition()));
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
