android-gradle-transform-example
================================
### An Android Gradle [Transform API](http://tools.android.com/tech-docs/new-build-system/transform-api) example

The intent was to wrap all [WakeLock](https://developer.android.com/reference/android/os/PowerManager.WakeLock.html) calls, including those that are in the libraries, to add some additional behaviour. So, here it is...

The example does not working right now: after the transformation the java.lang.VerifyError exception is thrown during the MainActivity launch.
