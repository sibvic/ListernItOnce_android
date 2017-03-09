package com.sibvic.listernitonce.Media;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Reads playback information for the file.
 */
class FileInformationReader {

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    static long readLength(File file) {
        return 0;//TODO: implement
    }

    static long readPosition(File file) {
        File infoFile = getInformationFile(file);
        if (!infoFile.exists() || infoFile.isDirectory()) {
            return 0;
        }
        try {
            FileInputStream fin = new FileInputStream(infoFile);
            String ret = convertStreamToString(fin);
            int indexOfNextLine = ret.indexOf("\n");
            if (indexOfNextLine > 0) {
                String numberString = ret.substring(0, indexOfNextLine);
                return Long.parseLong(numberString);
            }
            return Long.parseLong(ret);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @NonNull
    private static File getInformationFile(File file) {
        return new File(file.getAbsolutePath() + ".info");
    }
}
