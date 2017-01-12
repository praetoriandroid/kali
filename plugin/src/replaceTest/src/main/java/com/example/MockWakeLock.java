package com.example;

import android.os.PowerManager;

import java.util.List;

public class MockWakeLock {

    private static List<String> callLog;


    public static void setCallLog(List<String> callLog) {
        MockWakeLock.callLog = callLog;
    }

    public static void acquire(PowerManager.WakeLock wakeLock, long timeout) {
        callLog.add("acquire(" + timeout + ")");
    }

    public static void release(PowerManager.WakeLock wakeLock) {
        callLog.add("release()");
    }

}
