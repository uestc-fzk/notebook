package com.fzk.log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 实现组提交的日志打印
 *
 * @author fzk
 * @datetime 2023-02-09 12:02:06
 */
@SuppressWarnings("unused")
public class Logger {
    public static void fatal(String msg) {
        addMsg(LogLevel.FATAL, msg);
    }

    public static void error(String msg) {
        addMsg(LogLevel.ERROR, msg);
    }

    public static void warning(String msg) {
        addMsg(LogLevel.WARNING, msg);
    }

    public static void info(String msg) {
        addMsg(LogLevel.INFO, msg);
    }

    public static void debug(String msg) {
        addMsg(LogLevel.DEBUG, msg);
    }

    public static void fine(String msg) {
        addMsg(LogLevel.FINE, msg);
    }

    private static void addMsg(LogLevel level, String msg) {
        // 低于全局日志级别的日志忽略
        if (level.lower(globalLevel)) return;
        LogRecord logRecord = new LogRecord(level, msg, LocalDateTime.now(), 4);
        try {
            lock.lockInterruptibly();
            try {
                // 写满了，等待, 必须用while，会有很多情况下会唤醒
                while (queueWrite.size() >= defaultLogConf.getLogQueueSize()) {
                    emptyCond.await();
                }
                queueWrite.add(logRecord);
                flushTread.wakeUp();// 唤醒刷新线程
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static final LogConf defaultLogConf = LogConf.getDefaultLogConf();
    private static final LogLevel globalLevel;// 当前设置的日志级别，低于此级别的不会打印
    private static volatile ArrayList<LogRecord> queueWrite;// 各个日志写入此队列
    private static volatile ArrayList<LogRecord> queueRead;// flush线程从此队列处理日志
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition emptyCond = lock.newCondition();
    private static final FlushThread flushTread = new FlushThread();
    private static FileChannel file;

    static {
        try {
            // 1.创建或切割日志
            Path logPath = Path.of(defaultLogConf.getLogPath());
            createLogFile(logPath);

            // 3.队列初始化
            queueWrite = new ArrayList<>(defaultLogConf.getLogQueueSize());
            queueRead = new ArrayList<>(defaultLogConf.getLogQueueSize());
            // 4.日志level设置
            globalLevel = LogLevel.getLevel(defaultLogConf.getLogLevel());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 5.日志刷新线程启动
        flushTread.setDaemon(true);// 必须设为后台线程
        flushTread.start();
    }

    /**
     * 刷新线程：已实现组提交
     */
    private static class FlushThread extends Thread {
        public volatile boolean isAwake = false;// 刷新线程活跃状态：避免冗余唤醒
        private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 轮转队列: 组提交
        private void turnQueue() {
            if (queueWrite.size() > 0) {
                // 轮转队列
                ArrayList<LogRecord> tmp = queueWrite;
                queueWrite = queueRead;
                queueRead = tmp;
                emptyCond.signalAll();// 唤醒所有等待线程
            }
        }

        public void wakeUp() {
            if (!isAwake) {// 避免冗余唤醒
                LockSupport.unpark(flushTread);
                //System.out.println("唤醒");
            }
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    isAwake = true;
                    // 轮转队列
                    lock.lockInterruptibly();
                    try {
                        turnQueue();
                    } finally {
                        lock.unlock();
                    }

                    if (queueRead.size() > 0) {
                        handleRead();
                    } else {
                        isAwake = false;
                        LockSupport.parkNanos(10_000_000_000L);// 等待10s
                    }
                }
            } catch (InterruptedException | IOException e) {
                System.err.println("flush log thread occurs error: " + e);
            } finally {
                try {
                    file.close();
                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
                }
            }
        }

        public void handleRead() throws IOException {
            // 1.处理日志队列
            for (LogRecord record : queueRead) {
                // level time caller msg
                // warning长度为7
                String content = String.format("%-7s %s %s %s\n", record.level, format.format(record.time), record.caller, record.msg);
                file.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
                // 控制台染色
                if (record.level.higher(LogLevel.INFO))
                    System.out.printf("%s%s%s", ConsoleColors.RED, content, ConsoleColors.RESET);
                else System.out.print(content);
            }
            queueRead.clear();// 清空队列
            // 2.落盘
            file.force(true);
            // 3.切割日志
            if (file.size() >= defaultLogConf.getLogFileSize()) {
                splitLogFile();
            }
        }
    }

    // 日志切割，按文件大小切割
    // 注意：调用这个方法，日志文件一定是存在的
    private static void splitLogFile() throws IOException {
        // 1.关闭文件
        if (file != null && file.isOpen()) {
            file.close();
        }
        // 2.文件替换：以写入文件第一行的时间命名，因为不知道为啥以创建时间有bug?
        Path origin = Path.of(defaultLogConf.getLogPath());
        Path target = null;
        // 注意要关闭资源
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(origin.toFile()));) {
            String s = bufferedReader.readLine();
            String[] splits = s.split("\\.");
            if (splits.length != 2) throw new RuntimeException("写入文件第一行的时间错误：" + s);
            long second = Long.parseLong(splits[0]);
            int nano = Integer.parseInt(splits[1]);
            LocalDateTime createTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(second, nano), ZoneId.systemDefault());
            target = Path.of(String.format("%s_%04d%02d%02d_%02d%02d%02d.%09d", defaultLogConf.getLogPath(),
                    createTime.getYear(), createTime.getMonth().getValue(), createTime.getDayOfMonth(),
                    createTime.getHour(), createTime.getMinute(), createTime.getSecond(), createTime.getNano()));
        }
        // 注意：这里必须先关闭资源，再进行移动，否则会报错：另一个程序正在使用此文件，进程无法访问。
        Files.move(origin, target, StandardCopyOption.ATOMIC_MOVE);
        // 3.创建新文件
        createLogFile(origin);
    }

    // 创建日志文件，并将创建时间写入第一行
    private static void createLogFile(Path path) throws IOException {
        // 1.先确保目录已经创建
        if (Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        // 2.日志文件若存在，则切割
        if (Files.exists(path)) {
            splitLogFile();
        } else {
            // 3.创建日志文件，并将创建时间写入第一行
            Instant now = Instant.now();// 注意：测试发现目前不管是Instant还是LocalDateTime都只能精确到微秒
            file = FileChannel.open(path, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));
            // 创建时间: second.nano\n
            String createTime = String.format("%d.%d\n", now.getEpochSecond(), now.getNano());
            file.write(ByteBuffer.wrap(createTime.getBytes(StandardCharsets.UTF_8)));
            file.force(true);
        }
    }
}
