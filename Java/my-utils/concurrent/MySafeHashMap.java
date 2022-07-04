package util;

import java.util.HashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 借助读写锁实现的HashMap安全映射
 *
 * @author fzk
 * @date 2022-01-03 21:31
 */
public class MySafeHashMap<K, V> {
    private final HashMap<K, V> map;
    private final ReentrantReadWriteLock readWriteLock;// 读写锁
    private final ReentrantReadWriteLock.ReadLock readLock;// 读锁
    private final ReentrantReadWriteLock.WriteLock writeLock; // 写锁

    public MySafeHashMap(int capacity) {
        map = new HashMap<>(capacity);
        readWriteLock = new ReentrantReadWriteLock();// 读写锁
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
    }

    public V get(K key) {
        readLock.lock();// 锁定操作不能放入try块，避免出现try中发生的获取锁(自定义锁)时发生异常导致锁无故释放
        try {
            return map.get(key);
        } finally {
            readLock.unlock();
        }
    }

    /*返回null或者旧的值*/
    public V put(K key, V value) {
        LockSupport.park(this);
        writeLock.lock();// 锁定操作不能放入try块，避免出现try中发生的获取锁(自定义锁)时发生异常导致锁无故释放
        try {
            return map.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    // 清空集合
    public void clear() {
        writeLock.lock();// 锁定操作不能放入try块，避免出现try中发生的获取锁(自定义锁)时发生异常导致锁无故释放
        try {
            map.clear();
        } finally {
            writeLock.unlock();
        }
    }
}
