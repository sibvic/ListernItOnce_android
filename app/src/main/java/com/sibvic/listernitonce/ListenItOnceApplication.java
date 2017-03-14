package com.sibvic.listernitonce;

import android.app.Application;
import android.content.Context;

import org.acra.*;
import org.acra.annotation.*;

/**
 * Main application class
 */

@ReportsCrashes(
        mailTo = "sibvic@gmail.com",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_report_text
)
public class ListenItOnceApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
