package com.fzk.log;

/**
 * @author fzk
 * @datetime 2023-02-09 11:46:54
 */
public final class LogLevel {
    private final String name;
    private final int value;

    public int getValue() {
        return this.value;
    }

    public String getName() {
        return this.name;
    }

    public boolean higher(LogLevel level) {
        return this.value > level.value;
    }

    public boolean lower(LogLevel level) {
        return this.value < level.value;
    }

    public boolean equal(LogLevel level) {
        return this.value == level.value;
    }

    private LogLevel(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static LogLevel getLevel(String name) {
        String level = name.toUpperCase();
        switch (level) {
            case "FATAL":
                return FATAL;
            case "ERROR":
                return ERROR;
            case "WARNING":
                return WARNING;
            case "INFO":
                return INFO;
            case "DEBUG":
                return DEBUG;
            case "FINE":
                return FINE;
            default:
                throw new RuntimeException("can not find the log level of: " + name);
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static final LogLevel FATAL = new LogLevel("FATAL", 5);
    public static final LogLevel ERROR = new LogLevel("ERROR", 4);
    public static final LogLevel WARNING = new LogLevel("WARNING", 3);
    public static final LogLevel INFO = new LogLevel("INFO", 2);
    public static final LogLevel DEBUG = new LogLevel("DEBUG", 1);
    public static final LogLevel FINE = new LogLevel("FINE", 0);
}
