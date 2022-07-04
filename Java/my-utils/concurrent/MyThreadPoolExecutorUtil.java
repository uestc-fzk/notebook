package com.fzk.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 由于Executors类帮助生产的线程池有一定弊端，这里用ThreadPoolExecutor直接构造线程池
 * 其实也可以看看Executors类新建线程池的步骤，用于参考，在这里更好的创建线程池
 * 可以利用 Executors 的一些静态方法可以把 Runnable 与 Callable 任务互相转换
 *
 * @author fzk
 * @apiNote 一次性涌入大量任务，如果正在处理任务+待处理任务超过了 线程池最大线程量+等待队列容量,
 * 默认会拒绝接受任务，抛出RejectedExecutionException，这是运行时异常，要见机行事
 * @date 2021-11-21 16:09
 * @see java.util.concurrent.ThreadPoolExecutor
 */
@SuppressWarnings("unused")
public class MyThreadPoolExecutorUtil {
    /**
     * 在等待队列达到上限之后，新建线程，线程达到上限而等待队列也上限了，
     * 再加任务默认会报错RejectedExecutionException
     */
    private static final int CORE_POOL_SIZE = 2; // 池中最少线程,设置0的话，第1个任务到了会新建1个线程，然后会等到任务队列满了之后才新建，这样不好
    private static final int MAX_POOL_SIZE = 5;  // 池中最多线程
    /**
     * 如果池中当前有超过 corePoolSize 的线程，则多余的线程如果空闲时间超过 keepAliveTime 将被终止
     */
    private static final int KEEP_ALIVE_TIME = 1000 * 10;
    private static final TimeUnit unit = TimeUnit.MILLISECONDS;
    /**
     * 任何BlockingQueue都可用于传输和保存提交的任务。 此队列的使用与池大小交互：
     * 1.如果正在运行的线程少于 corePoolSize，则 Executor 总是喜欢添加新线程而不是排队。
     * 2.如果 corePoolSize 或更多线程正在运行，Executor 总是喜欢将请求排队而不是添加新线程。
     * 3.如果请求无法排队，则会创建一个新线程，除非这会超过 maximumPoolSize，在这种情况下，任务将被拒绝。
     * <p>
     * 排队的一般策略有以下三种：
     * 1.直接交接:
     * 工作队列的一个很好的默认选择是SynchronousQueue, 它将任务移交给线程而不用其他方式保留它们.
     * 在这里，如果没有线程可立即运行，则将任务排队的尝试将失败，因此将构建一个新线程。
     * 在处理可能具有内部依赖性的请求集时，此策略可避免锁定。 直接切换通常需要无限的maximumPoolSizes 以避免拒绝新提交的任务。
     * 这反过来又承认了当命令平均到达速度快于它们可以处理的速度时，线程无限增长的可能性。
     * 2.无界队列:
     * 使用无界队列,没有预定义容量的LinkedBlockingQueue
     * 将导致新任务在所有 corePoolSize 线程都忙时在队列中等待。 因此，不会创建超过 corePoolSize 的线程。
     * （因此maximumPoolSize的值没有任何影响。）当每个任务完全独立于其他任务时，这可能是合适的，因此任务不会影响彼此的执行； 例如，在网页服务器中。 虽然这种排队方式在平滑请求的瞬时爆发方面很有用，但它承认当命令的平均到达速度超过它们的处理速度时，工作队列可能会无限增长。
     * 3.有界队列:
     * 有界队列（ArrayBlockingQueue）在与有限的 maximumPoolSizes 一起使用时有助于防止资源耗尽，但可能更难以调整和控制。
     * 队列大小和最大池大小可以相互权衡：使用大队列和小池可以最大限度地减少 CPU 使用率、操作系统资源和上下文切换开销，但会导致人为地降低吞吐量。 如果任务频繁阻塞（例如，如果它们受 I/O 限制），则系统可能能够为比您允许的更多线程安排时间。 使用小队列通常需要更大的池大小，这会使 CPU 更忙，但可能会遇到不可接受的调度开销，这也会降低吞吐量。
     */
    private static final int QUEUE_CAPACITY = 10;// 有界队列容量10

    /**
     * 线程池缓存
     */
    private static volatile ThreadPoolExecutor threadPool = null;

    /*双重校验DCL*/
    public static ThreadPoolExecutor getThreadPool() {
        if (threadPool == null) {
            synchronized (MyThreadPoolExecutorUtil.class) {
                if (threadPool == null) {
                    // 线程池一般创建方法，排队策略选择：有界队列ArrayBlockingQueue
                    threadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, unit,
                            new ArrayBlockingQueue<>(QUEUE_CAPACITY), new NamingThreadFactory("这个线程池该叫啥呢"));
                }
            }
        }
        return threadPool;
    }

    public static <T> List<Future<T>> invokeAll(List<Callable<T>> callables) throws InterruptedException {

        ThreadPoolExecutor threadPool = getThreadPool();
        // 注意：在任务数过多时，这里还有可能抛出RejectedExecutionException
        return threadPool.invokeAll(callables);// 阻塞到任务完成
    }

    /**
     * 建议用这个方法进行任务处理
     *
     * @param callables 需要处理的任务列表
     * @return 任务列表返回的结果集合
     */
    public static <T> List<Future<T>> execute(List<Callable<T>> callables) {
        assert callables != null && callables.size() != 0 : "线程调度任务不能为null";

        ThreadPoolExecutor cachedThreadPool = getThreadPool();
        List<Future<T>> futures = new ArrayList<>(callables.size());
        try {
            // 所有任务提交执行，这个玩意任务量过多应该是一直排队
            callables.forEach(callable -> futures.add(cachedThreadPool.submit(callable)));

        } catch (Throwable t) {// 这里的写法来自于invokeAll方法
            futures.forEach(task -> task.cancel(true));
            throw t;
        }
        return futures;
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

        // 1.新建执行器(线程池)？？？新建个毛线，用已经有的这个缓存线程池
        // ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        ExecutorService cachedThreadPool = getThreadPool();

        // 2.新建ExecutorCompletionService，会管理Future对象的一个阻塞队列，其中包含所提交任务的结果(一旦结果可用，就会放入队列)
        ExecutorCompletionService<T> completionService =
                new ExecutorCompletionService<>(cachedThreadPool);
        ArrayList<Future<T>> futures = new ArrayList<>(len);// 存放任务列表返回的结果集合
        T result = null;
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
        // 1.执行异步任务
        // 如果不提供执行器，任务会在默认执行器上执行(ForkJoinPool.commonPool()返回的执行器).通常你可能不希望这样做
        CompletableFuture<T> f = CompletableFuture.supplyAsync(supplier, getThreadPool());

        // 2.任务完成之后的回调
        f.whenComplete(action);
    }

    /**
     * 将Runnable任务转为Callable任务，FutureTask返回结果应该是null
     * 其是完全可以自己转换，没必要是吧
     *
     * @param runnableList 需要转换的Runnable
     * @return Callable
     */
    public static List<Callable<Object>> RunnableToCallable(List<Runnable> runnableList) {
        // 对于null和空的处理策略，应该采取原样返回
        if (runnableList == null) return null;
        if (runnableList.size() == 0) return new ArrayList<>(0);

        ArrayList<Callable<Object>> callables = new ArrayList<>(runnableList.size());
        runnableList.forEach(task -> callables.add(Executors.callable(task)));
        return callables;
    }

    private static class NamingThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNum = new AtomicInteger(1);
        private final ThreadFactory delegate;
        private final String namePrefix;

        public NamingThreadFactory(String namePrefix) {
            this.delegate = Executors.defaultThreadFactory();// 借我用一下, 嘿嘿
            this.namePrefix = namePrefix;
        }

        /**
         * 创建一个带名字的线程池生产工厂
         */
        public NamingThreadFactory(ThreadFactory delegate, String namePrefix) {
            this.delegate = delegate;
            this.namePrefix = namePrefix; // TODO consider uniquifying this
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = delegate.newThread(r);
            t.setName(namePrefix + " [#" + threadNum.getAndIncrement() + "]");
            return t;
        }
    }

    /*
    private static class ThreadPoolHolder {
        //缓存线程池
        private static volatile ThreadPoolExecutor cachedThreadPool = null;

        //双重校验：DCL
        public static ThreadPoolExecutor getCachedThreadPool() {
            if (cachedThreadPool == null) {
                synchronized (ThreadPoolHolder.class) {
                    if (cachedThreadPool == null) {
                        // 缓存池的精髓就在于corePoolSize=0和SynchronousQueue
                        // 用这个SynchronousQueue有个问题，一有任务就新建线程，建不了线程就抛RejectedExecutionException
                        // 我tm服了，那么这样就不能用SynchronousQueue
                        cachedThreadPool = new ThreadPoolExecutor(0, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                                unit, new SynchronousQueue<>());
                    }
                }
            }
            return cachedThreadPool;
        }

        private ThreadPoolHolder() {
        }
    }
    */

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor threadPool = getThreadPool();
        try {
            for (int i = 0; i < 10; i++) {
                //创建WorkerThread对象（WorkerThread类实现了Runnable 接口）
                Runnable task = new MyRunnable(String.valueOf(i));
                //执行Runnable
                threadPool.execute(task);
            }
        } finally {
            // 终止线程池
            threadPool.shutdown();
            // 将主线程阻塞到所有任务终止
            while (!threadPool.awaitTermination(1000 * 10, TimeUnit.MILLISECONDS)) {
                System.out.println("我应该只执行了一次吧,执行第2次说明这么多时间都没有等待全部线程关闭，即线程还在做任务");
            }
            System.out.println("Finished all threads");
        }
    }

    /**
     * 这是一个简单的Runnable类，需要大约5秒钟来执行其任务。
     *
     * @author shuang.kou
     */
    private static class MyRunnable implements Runnable {

        private final String command;

        public MyRunnable(String s) {
            this.command = s;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " Start. Time = " + new Date());
            processCommand();
            System.out.println(Thread.currentThread().getName() + " End. Time = " + new Date());
        }

        private void processCommand() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return this.command;
        }
    }
}
