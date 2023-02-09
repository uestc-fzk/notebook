package com.fzk.log;

import com.fzk.env.MyEnv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    private static final LogConf defaultLogConf = MyEnv.getLogConf();
    private static final LogLevel globalLevel;// 当前设置的日志级别，低于此级别的不会打印
    private static volatile ArrayList<LogRecord> queueWrite;// 各个日志写入此队列
    private static volatile ArrayList<LogRecord> queueRead;// flush线程从此队列处理日志
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition emptyCond = lock.newCondition();
    private static final FlushThread flushTread = new FlushThread();
    private static FileChannel file;

    static {
        try {
            System.out.println(defaultLogConf);
            // 1.目录处理
            Path logPath = Path.of(defaultLogConf.getLogPath());
            if (Files.notExists(logPath.getParent()))
                Files.createDirectories(logPath.getParent());
            // 2.打开日志文件通道
            // 如果旧文件存在则备份
            if (Files.exists(logPath)) {
                splitLogFile();
            } else {
                file = FileChannel.open(logPath, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE));
            }

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
                String content = String.format("%s %s %s %s\n", record.level, format.format(record.time), record.caller, record.msg);
                file.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
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
    // 调用这个方法，日志文件一定是存在的
    private static void splitLogFile() throws IOException {
        // 1.关闭文件
        if (file != null && file.isOpen())
            file.close();
        // 2.文件替换：以文件最后修改时间命名，因为不知道为啥以创建时间有bug?
        Path origin = Path.of(defaultLogConf.getLogPath());
        LocalDateTime modifiedTime = LocalDateTime.ofInstant(Files.getLastModifiedTime(origin).toInstant(), ZoneId.systemDefault());
        Path target = Path.of(String.format("%s_%04d%02d%02d_%02d%02d%02d.%09d", defaultLogConf.getLogPath(),
                modifiedTime.getYear(), modifiedTime.getMonth().getValue(), modifiedTime.getDayOfMonth(),
                modifiedTime.getHour(), modifiedTime.getMinute(), modifiedTime.getSecond(), modifiedTime.getNano()));
        Files.move(origin, target);
        // 3.创建新文件
        file = FileChannel.open(origin, Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW));
    }
}
