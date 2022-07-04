package util;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 借助数组、可重入锁、Condition实现的阻塞队列
 *
 * @author fzk
 * @date 2022-01-03 22:14
 */
public class MyArrayBlockQueue<T> {
    private final Object[] items;
    private int addIndex, removeIndex, count;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;

    public MyArrayBlockQueue(int capacity) {
        items = new Object[capacity];
        count = 0;
        addIndex = 0;
        removeIndex = 0;
        lock = new ReentrantLock();
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }

    // 添加一个元素，如果队列慢，则进入等待状态
    public void add(T t) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length)// while防止虚假唤醒
                notFull.await();// 等非满信号

            items[addIndex] = t;
            if (++addIndex == items.length) addIndex = 0;
            ++count;
            notEmpty.signal();// 放一个非空信号
        } finally {
            lock.unlock();
        }
    }

    // 移除并返回队首元素，如果数组为空，则等待
    public T remove() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0)// while防止虚假唤醒
                notEmpty.await();// 等待非空信号

            Object t = items[removeIndex];
            items[removeIndex] = null;
            if (++removeIndex == items.length) removeIndex = 0;
            count--;
            notFull.signal();// 放非满信号
            return (T) t;
        } finally {
            lock.unlock();
        }
    }
}
