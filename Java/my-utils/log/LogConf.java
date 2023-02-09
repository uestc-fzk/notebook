package com.fzk.log;

/**
 * 日志配置类
 *
 * @author fzk
 * @datetime 2023-02-09 21:35:30
 */
public class LogConf {
    private String logPath;
    private String logLevel;
    private int logQueueSize;// 日志队列大小，建议1024
    private long logFileSize;// 日志文件大小，建议16MB，即16*1024*1024


    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public int getLogQueueSize() {
        return logQueueSize;
    }

    public void setLogQueueSize(int logQueueSize) {
        this.logQueueSize = logQueueSize;
    }

    public long getLogFileSize() {
        return logFileSize;
    }

    public void setLogFileSize(long logFileSize) {
        this.logFileSize = logFileSize;
    }

    public static LogConf getDefaultLogConf() {
        LogConf conf = new LogConf();
        conf.setLogLevel("info");
        conf.setLogPath("logs/info.log");
        conf.setLogQueueSize(1024);
        conf.setLogFileSize(16 * 1024 * 1024);// 16MB
        return conf;
    }

    @Override
    public String toString() {
        return "LogConf{" +
                "logPath='" + logPath + '\'' +
                ", logLevel='" + logLevel + '\'' +
                ", logQueueSize=" + logQueueSize +
                ", logFileSize=" + logFileSize +
                '}';
    }
}
