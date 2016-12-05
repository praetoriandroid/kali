package com.example.gte;

import android.content.Context;
import android.os.PowerManager;

public class FakeManager {
    private final PowerManager powerManager;

    private FakeManager(Context context) {
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    public static Object getSystemService(Context context, String name) {
        if (Context.POWER_SERVICE.equals(name)) {
            return new FakeManager(context);
        } else {
            return context.getSystemService(name);
        }
    }

    public FakeLock newWakeLock(int levelAndFlags, String tag) {
        return new FakeLock(powerManager, levelAndFlags, tag);
    }
}
