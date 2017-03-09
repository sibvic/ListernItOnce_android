package com.sibvic.listernitonce.Options;

import android.content.Context;

import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Options for the applications.
 * Options stored in the private storage.
 */
public class Options {
    private String targetFolder;

    public String getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder(String targetFolder) {
        this.targetFolder = targetFolder;
    }

    static Options fromJson(String json) {
        return new Gson().fromJson(json, Options.class);
    }

    String toJson() {
        return new Gson().toJson(this);
    }
}
