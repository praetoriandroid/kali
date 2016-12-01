package com.example.gte;

import android.content.Context;
import android.os.PowerManager;

public class FakeManager {
    private final PowerManager powerManager;

    private FakeManager(Context context) {
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    public static Object create(Context context, String name) {
        if (!Context.POWER_SERVICE.equals(name)) {
            throw new IllegalArgumentException("oops...");
        }
        return new FakeManager(context);
    }

    public FakeLock newWakeLock(int levelAndFlags, String tag) {
        return new FakeLock(powerManager, levelAndFlags, tag);
    }
}
