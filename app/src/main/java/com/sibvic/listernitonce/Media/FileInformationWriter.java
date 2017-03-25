package com.sibvic.listernitonce.Media;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Writes playback information for the media file.
 */
public class FileInformationWriter {

    public static void saveInformation(MediaFile currentFile) {
        File infoFile = currentFile.getMetaInformationFile();
        try {
            FileWriter outFile = new FileWriter(infoFile.getAbsoluteFile());
            PrintWriter out = new PrintWriter(outFile);
            out.println(Long.toString(currentFile.getCurrentPosition()));
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
