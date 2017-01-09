package com.example.kali;

import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;

import com.example.kali.tmp.TmpActivity;

import java.util.concurrent.TimeUnit;

public class MainActivity extends TmpActivity {

    private boolean untouched;
    private PowerManager.WakeLock wakeLock;
    private double test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "foo");
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                    button.setText(R.string.lock);
                } else {
                    wakeLock.acquire(TimeUnit.SECONDS.toMillis(10));
                    button.setText(R.string.unlock);
                }
            }
        });
    }

    private static class InnerClass {

        void innerMethod() {
            MainActivity activity = null;
            activity.wakeLock = null;
        }

        void innerMethod2() {
            MainActivity activity = null;
            activity.test++;
        }

        void innerMethod3() {
            MainActivity activity = null;
            activity.test += 1000;
        }

        void innerMethod4() {
            MainActivity activity = null;
            activity.protectedField = "";
        }

    }
}
