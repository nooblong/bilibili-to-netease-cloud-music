package github.nooblong.btncm.entity;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ExpiringCache<T> {
    private volatile T value;
    private volatile long expireTime = 0;

    private final long ttlMillis;
    private final Supplier<T> loader;
    private final ReentrantLock lock = new ReentrantLock();

    public ExpiringCache(long ttlMillis, Supplier<T> loader) {
        this.ttlMillis = ttlMillis;
        this.loader = loader;
    }

    public T get() {
        long now = System.currentTimeMillis();

        if (value != null && now < expireTime) {
            return value;
        }

        lock.lock();
        try {
            // double-check 防止重复刷新
            now = System.currentTimeMillis();
            if (value == null || now >= expireTime) {
                value = loader.get();
                expireTime = now + ttlMillis;
            }
            return value;
        } finally {
            lock.unlock();
        }
    }
}
