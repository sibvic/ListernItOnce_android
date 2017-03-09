package com.sibvic.listernitonce.Options;

import android.content.Context;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Creates options.
 */
public class OptionsManager {

    public static void save(Context context, Options options) {
        String FILENAME = "lio_options";

        FileOutputStream fos;
        try {
            fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        PrintWriter writer = new PrintWriter(fos);
        writer.print(options.toJson());
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Options create(Context context) {
        String FILENAME = "lio_options";

        FileInputStream stream;
        try {
            stream = context.openFileInput(FILENAME);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new Options();
        }
        StringBuilder fileContent = new StringBuilder("");
        byte[] buffer = new byte[1024];
        int n;
        try {
            while ((n = stream.read(buffer)) != -1)
            {
                fileContent.append(new String(buffer, 0, n));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new Options();
        }

        String json = fileContent.toString();
        return Options.fromJson(json);
    }
}
