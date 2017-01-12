package android.os;

public class PowerManager {
    public static class WakeLock {
        public void acquire() {
            throw new IllegalStateException('Method was not replaced!')
        }

        public void release() {
            throw new IllegalStateException('Method was not replaced!')
        }
    }
}
