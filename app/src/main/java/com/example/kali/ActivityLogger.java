package com.example.kali;

import android.app.Activity;
import android.util.Log;

@SuppressWarnings("TryWithIdenticalCatches")
public class ActivityLogger {

    private static final String TAG = ActivityLogger.class.getSimpleName();

    public static void setContentView(Activity activity, int resId) {
        Log.d(TAG, activity.getClass() + ".setContentView(" + resId + ")");
        activity.setContentView(resId);
    }
}
