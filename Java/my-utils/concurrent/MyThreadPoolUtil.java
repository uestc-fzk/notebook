package com.fzk.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 阿里巴巴Java开发手册：[强制]
 * 线程池不允许使用 Executors 去创建，而是通过 ThreadPoolExecutor 的方式，
 * 这样的处理方式让写的同学更加明确线程池的运行规则，规避资源耗尽的风险
 * Executors 返回的线程池对象的弊端如下：
 *
 * <li> FixedThreadPool 和 SingleThreadPool </li>
 * 允许的请求队列长度为 Integer.MAX_VALUE，可能会堆积大量的请求，从而导致 OOM。
 *
 * <li> CachedThreadPool </li>
 * 允许的创建线程数量为 Integer.MAX_VALUE，可能会创建大量的线程，从而导致 OOM
 *
 * @author fzk
 * @date 2021-11-20 12:52
 */
@SuppressWarnings("unused")
@Deprecated
public class MyThreadPoolUtil {
    // 用可重入锁,不然用同步静态方法的话，锁的是Class类对象, 性能太低了
    private static final Lock single_lock = new ReentrantLock();
    private static final Lock fixed_lock = new ReentrantLock();
    private static final Lock cached_lock = new ReentrantLock();

    public static <T> List<Future<T>> singleThreadExecute(List<Callable<T>> callables) throws InterruptedException {
        assert callables != null && callables.size() != 0 : "线程调度任务不能为null";
        // 还是要同步才行
        single_lock.lock();// 进入临界区
        try {
            return ThreadPoolHolder.getSingleThreadPool().invokeAll(callables);
        }finally {
            single_lock.unlock();// 退出临界区
        }
    }

    public static <T> List<Future<T>> fixedThreadExecute(List<Callable<T>> callables) throws InterruptedException {
        assert callables != null && callables.size() != 0 : "线程调度任务不能为null";
        // 还是要同步才行
        fixed_lock.lock();// 进入临界区

        try {
            return ThreadPoolHolder.getFixedThreadPool().invokeAll(callables);
        } finally {
            fixed_lock.unlock();// 退出临界区
        }
    }

    /**
     * 建议用这个方法进行任务处理
     *
     * @param callables 需要处理的任务列表
     * @return 任务列表返回的结果集合
     * @throws InterruptedException 可能的异常
     */
    public static <T> List<Future<T>> cachedThreadExecute(List<Callable<T>> callables) throws InterruptedException {
        assert callables != null && callables.size() != 0 : "线程调度任务不能为null";

        /* warning：任务数过多引起创建大量线程，造成OOM，转向固定线程数 */
        if (callables.size() > 5) return fixedThreadExecute(callables);

        // 还是要同步才行
        cached_lock.lock();// 进入临界区

        try {
            return ThreadPoolHolder.getCachedThreadPool().invokeAll(callables);
        } finally {
            cached_lock.unlock();// 退出临界区
        }
    }

    /**
     * 对于传入的一系列任务，只要其中一个任务得到结果，计算停止并返回
     * 假设您想使用任务集的第一个非空结果，忽略任何遇到异常的结果，并在第一个任务准备好时取消所有其他任务
     *
     * @param callables 任务列表
     * @param <T>       返回的类型
     * @return 第一个完成的计算结果
     */
    public static <T> T executeForFirstResult(List<Callable<T>> callables) {
        assert callables != null && callables.size() != 0 : "线程调度任务不能为null";
        int len = callables.size();
        /* 因为会用到cachedTheadPool，所以用它的锁 */
        cached_lock.lock();// 进入临界区
        T result;

        try {
            // 1.新建执行器(线程池)？？？新建个毛线，用已经有的这个缓存线程池
            // ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
            ExecutorService cachedThreadPool = ThreadPoolHolder.getCachedThreadPool();

            // 2.新建ExecutorCompletionService，会管理Future对象的一个阻塞队列，其中包含所提交任务的结果(一旦结果可用，就会放入队列)
            ExecutorCompletionService<T> completionService =
                    new ExecutorCompletionService<>(cachedThreadPool);
            ArrayList<Future<T>> futures = new ArrayList<>(len);// 存放任务列表返回的结果集合
            result = null;
            try {
                // 3.将所有任务提交给执行器(即线程池)去执行
                callables.forEach(task -> futures.add(completionService.submit(task)));
                for (int i = len; i > 0; i--) {
                    try {
                        // 4.获取第一个计算完成的非空结果(执行出现异常返回是null)
                        T t = completionService.take().get();
                        if (t != null) {
                            result = t;
                            break;
                        }
                    } catch (ExecutionException | InterruptedException ignore) {
                    }
                }
            } finally {
                // 5.某个任务计算完成，取消剩余任务执行
                futures.forEach(task -> task.cancel(true));
                //cachedThreadPool.shutdown();//关闭线程池
            }
        } finally {
            cached_lock.unlock();// 退出临界区
        }
        return result;
    }

    /**
     * 异步计算：回调方法实现的异步计算：在任务完成之后要出现的动作注册一个回调
     * 主线程调用这个方法之后就退出这个方法了，这个方法调用其他线程执行任务，完成之后会调用回调方法
     * CompletableFuture执行完成后，得到一个结果或者捕获一个异常，需要使用whenComplete进行回调处理
     * <p>
     * 回调方法示例如下：
     * <pre>
     *       f.whenComplete((result, exception) -> {
     *             if (exception == null) {
     *                 // 处理结果
     *                 System.out.println(result);
     *             } else {
     *                 // 处理异常
     *                 exception.printStackTrace();
     *             }
     *         });
     * </pre>
     *
     * @param supplier 需要执行的任务，supplyAsync方法第一个参数不是Callable<T>，而是Supplier<T>, 不过其作用都是作为线程所需要执行的任务
     * @param action   任务完成之后的回调方法
     * @apiNote 需要注意的是当主线程死亡，这个后台线程也会死亡，回调方法可能就不会执行了
     */
    public static <T> void executeAsync(Supplier<T> supplier, BiConsumer<? super T, ? super Throwable> action) {
        /* 因为会用到cachedTheadPool，所以用它的锁 */
        cached_lock.lock(); // 进入临界区

        try {
            // 如果不提供执行器，任务会在默认执行器上执行(ForkJoinPool.commonPool()返回的执行器).通常你可能不希望这样做
            CompletableFuture<T> f = CompletableFuture.supplyAsync(supplier, ThreadPoolHolder.getCachedThreadPool());

            // 任务完成之后的回调
            f.whenComplete(action);
        } finally {
            cached_lock.unlock(); // 退出临界区
        }
    }

    /* 懒加载：DCL  缓存了一些线程池 */
    private static final class ThreadPoolHolder {
        /**
         * 单线程池子：退化了的线程池
         */
        private static volatile ExecutorService singleThreadPool;

        /**
         * 固定线程池子
         */
        private static volatile ExecutorService fixedThreadPool;

        /**
         * 缓存线程池：建议用这个，随任务量新建线程，无空闲线程则新建，线程空闲60s自动关闭。
         * 上面两种必须手动关系线程池子
         */
        private static volatile ExecutorService cachedThreadPool;

        public static ExecutorService getSingleThreadPool() {
            if (singleThreadPool != null)
                return singleThreadPool;
            // DCL
            synchronized (ThreadPoolHolder.class) {
                if (singleThreadPool != null) return singleThreadPool;

                singleThreadPool = Executors.newSingleThreadExecutor();
                return singleThreadPool;
            }
        }

        public static ExecutorService getFixedThreadPool() {
            if (fixedThreadPool != null)
                return fixedThreadPool;
            // DCL
            synchronized (ThreadPoolHolder.class) {
                if (fixedThreadPool != null) return fixedThreadPool;

                fixedThreadPool = Executors.newFixedThreadPool(2);
                return fixedThreadPool;
            }
        }

        public static ExecutorService getCachedThreadPool() {
            if (cachedThreadPool != null)
                return cachedThreadPool;
            // DCL
            synchronized (ThreadPoolHolder.class) {
                if (cachedThreadPool != null) return cachedThreadPool;

                cachedThreadPool = Executors.newCachedThreadPool();
                return cachedThreadPool;
            }
        }

        private ThreadPoolHolder() {
        }
    }
}
