android-transform-gradle-plugin
===============================
### An Android [Transform](http://tools.android.com/tech-docs/new-build-system/transform-api) [Gradle Plugin](https://docs.gradle.org/current/userguide/custom_plugins.html)

The initial intent was to wrap [WakeLock](https://developer.android.com/reference/android/os/PowerManager.WakeLock.html) calls, including those that are in the libraries, to make some behaviour changes. So, here it is...

### What is does?
If you want to replace, for example, all [`PowerManager.WakeLock.acquire(timeout)`](https://developer.android.com/reference/android/os/PowerManager.WakeLock.html#acquire()) calls in your application, including those in any used library, by your own custom static calls like this:
```java
package com.example.gte;

public class FakeLock {
    // ...

    public static void smartAcquire(PowerManager.WakeLock wakeLock, long timeout) {
        if (timeout > MAX_WAKE_LOCK_TIMEOUT) {
            Log.w(TAG, "Long lasting WakeLock: timeout=" + timeout);
            timeout = MAX_WAKE_LOCK_TIMEOUT;
        }
        wakeLock.acquire(timeout);
    }
}
```
then all you need is to build and deploy this plugin and then add the next lines to your `build.gradle` file:
```gradle
buildscript {
    // ...

    dependencies {
        // ...
        classpath 'ru.mail:android-transformer-gradle-plugin:1.0'
    }
}

apply plugin: 'ru.mail.android-transformer'

transformer {
    ignoreClass 'com.example.gte.FakeLock'
    def wakeLockWrappers = [
            'android.os.PowerManager$WakeLock.acquire(J)V': 'com.example.gte.FakeLock.smartAcquire'
    ]
    replacements wakeLockWrappers
}
```

### `acquire(J)V`??? `V` for `void` return type? But what the `J` is that!?
Yeah, this is a bit of magic from the heart of Java. See [How to describe the types](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.2) and [what is in parentheses and after it](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3). Also note that inner class `WakeLock` is separated from the parent class by the '*$*', not by the *period*.

### Ok, what's next?
Well... more transformations, I guess.
