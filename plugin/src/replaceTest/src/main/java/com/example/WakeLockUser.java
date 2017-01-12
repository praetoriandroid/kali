package com.example;

import android.os.PowerManager;

public class WakeLockUser {
    public void useWakeLock(PowerManager.WakeLock wakeLock) {
        wakeLock.acquire(100);
        wakeLock.release();
    }
}