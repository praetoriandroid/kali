package com.example.gte;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.PowerManager;
import android.os.WorkSource;
import android.util.Log;

public class FakeLock {
    private final PowerManager.WakeLock wakeLock;

    public FakeLock(PowerManager powerManager, int levelAndFlags, String tag) {
        Log.d("###", "WL.newWakeLock(" + levelAndFlags + ", '" + tag + "')");
        wakeLock = powerManager.newWakeLock(levelAndFlags, tag);
    }

    public void acquire(long timeout) {
        Log.d("###", "WL.acquire(" + timeout + ")");
        wakeLock.acquire(timeout);
    }

    public void acquire() {
        Log.d("###", "WL.acquire()");
        wakeLock.acquire();
    }

    public boolean isHeld() {
        boolean result = wakeLock.isHeld();
        Log.d("###", "WL.isHeld(): " + result);
        return result;
    }

    public void release() {
        Log.d("###", "WL.release()");
        wakeLock.release();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void release(int flags) {
        Log.d("###", "WL.release(" + flags + ")");
        wakeLock.release(flags);
    }

    public void setReferenceCounted(boolean value) {
        Log.d("###", "setReferenceCounted(" + value + ")");
        wakeLock.setReferenceCounted(value);
    }

    public void setWorkSource(WorkSource ws) {
        Log.d("###", "setWorkSource(" + ws + ")");
        wakeLock.setWorkSource(ws);
    }

    @Override
    public String toString() {
        return wakeLock.toString();
    }
}
