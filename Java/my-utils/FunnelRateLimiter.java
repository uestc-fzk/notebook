package utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 漏斗限流器
 *
 * @author fzk
 * @datetime 2022-07-11 21:25
 */
public class FunnelRateLimiter {
    private final ConcurrentHashMap<String, Funnel> funnels = new ConcurrentHashMap<>();
    private final long DEFAULT_CAPACITY;
    private final double DEFAULT_FLOW_RATE;// 流速，单位(个/ms)

    public
    FunnelRateLimiter(long DEFAULT_CAPACITY, double DEFAULT_FLOW_RATE) {
        this.DEFAULT_CAPACITY = DEFAULT_CAPACITY;
        this.DEFAULT_FLOW_RATE = DEFAULT_FLOW_RATE;
    }

    // 判断是否允许放行
    public boolean isActionAllow(String key, long water) {
        Funnel funnel = funnels.get(key);
        if (funnel == null) {
            funnel = new Funnel(DEFAULT_CAPACITY, DEFAULT_FLOW_RATE);
            funnels.put(key, funnel);
        }
        return funnel.pushWater(water);
    }

    // 加入一个漏斗
    public boolean makeFunnel(String key, long capacity, double flowRate) {
        assert capacity > 0 && flowRate > 0;
        boolean exits = funnels.containsKey(key);
        if (!exits)
            funnels.put(key, new Funnel(capacity, flowRate));
        return !exits;
    }

    public Funnel removeFunnel(String key) {
        return funnels.remove(key);
    }

    static class Funnel {
        private final long capacity;// 漏斗容量
        private final double flowRate;// 流速
        private final AtomicLong leftSpace;// 剩余空间
        private volatile long lastFlowTimeStamp;// 上次流水时间戳
        private final Lock lock;

        public Funnel(long capacity, double flowRate) {
            this.capacity = capacity;
            this.flowRate = flowRate;
            this.leftSpace = new AtomicLong(capacity);
            this.lastFlowTimeStamp = System.currentTimeMillis();
            this.lock = new ReentrantLock();
        }

        // 漏水，返回剩余空间，需保证此方法最多1个线程执行
        long leakWater() {
            long now = System.currentTimeMillis();
            long timeDiff = now - lastFlowTimeStamp;
            long diffWater = (long) (timeDiff * flowRate);
            if (diffWater < 0 || diffWater > capacity) {
                // 溢出，说明太久没漏水了，直接清空漏斗
                System.out.println("清空漏洞:" + diffWater);
                leftSpace.set(capacity);
                lastFlowTimeStamp = now;
                return capacity;
            }
            if (diffWater != 0) {
                System.out.println("放水: " + diffWater);
                long left = leftSpace.addAndGet(diffWater);
                if (left < 0) leftSpace.set(0);
                return left < 0 ? 0 : left;
            }
            return leftSpace.get();
        }

        // 入水
        boolean pushWater(long water)   {
            if (water > capacity) return false;
            long old = leftSpace.get();
            if (old < water) {
                if (!lock.tryLock()) return false;
                try {
                    old = leakWater();
                } finally {
                    lock.unlock();
                }
            }
            if (old < water) return false;

            leftSpace.getAndAdd(-water);
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FunnelRateLimiter limiter = new FunnelRateLimiter(100, 1);
        CountDownLatch countDownLatch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 100; ) {
                        Thread.sleep(ThreadLocalRandom.current().nextLong(10, 100));
                        if (!limiter.isActionAllow("testKey", 1)) {
                            System.out.println(Thread.currentThread().getName() + " flow limit");
                            continue;
                        }
                        j++;
                    }
                    countDownLatch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, "thread-" + i).start();
        }

        countDownLatch.await();
    }
}
