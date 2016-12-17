package com.example.kali;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

public class FakeLock {

    public static void verboseAcquire(PowerManager.WakeLock wakeLock, long timeout) {
        Log.d("###", "static acquire(" + timeout + ")");
        wakeLock.acquire(timeout);
    }

    public static void verboseAcquire(PowerManager.WakeLock wakeLock) {
        Log.d("###", "static acquire()");
        wakeLock.acquire();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void verboseRelease(PowerManager.WakeLock wakeLock, int flags) {
        Log.d("###", "static release(" + flags + ")");
        wakeLock.release(flags);
    }

    public static void verboseRelease(PowerManager.WakeLock wakeLock) {
        Log.d("###", "static release()");
        wakeLock.release();
    }
}
