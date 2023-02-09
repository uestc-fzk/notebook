package com.fzk.log;

import java.time.LocalDateTime;

/**
 * @author fzk
 * @datetime 2023-02-09 11:28:31
 */
public class LogRecord {
    public final LogLevel level;
    public final String msg;
    public final LocalDateTime time;
    public final StackTraceElement caller;

    public LogRecord(LogLevel level, String msg, LocalDateTime time, int callDepth) {
        this.level = level;
        this.msg = msg;
        this.time = time;
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        // 应该是3
        this.caller = stackTraces[callDepth];
    }
}
