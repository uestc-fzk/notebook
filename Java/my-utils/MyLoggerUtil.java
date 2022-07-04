package util;

import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 提供者Supplier函数式接口 提供日志信息
 * <p>
 * 7个日志级别, 从上到下依次递减
 * 默认只记录前3个级别
 *
 * @author fzk
 * @date 2021-11-17 10:26
 */
@SuppressWarnings("unused")
public class MyLoggerUtil {

    public static void severe(Supplier<String> supplier) {
        getLogger().severe(supplier);
    }

    public static void warning(String msg) {
        getLogger().warning(msg);
    }

    public static void info(String msg) {
        getLogger().info(msg);
    }

    public static void config(Supplier<String> supplier) {
        getLogger().config(supplier);
    }

    public static void fine(Supplier<String> supplier) {
        getLogger().fine(supplier);
    }

    public static void finer(Supplier<String> supplier) {
        getLogger().finer(supplier);
    }

    public static void finest(Supplier<String> supplier) {
        getLogger().finest(supplier);
    }

    public static Logger getLogger() {
        return LoggerHolder.logger;
    }

    /**
     * 单例模式：
     * 登记式/静态内部类
     */
    private static class LoggerHolder {
        static final Logger logger;

        static {
            logger = Logger.getLogger("my.logger");
            // 默认是INFO级别，由于处理器默认是INFO级别，因此要修改，必须连同处理器也一起修改，如下面这种方式
            /*
            try {
                logger.setLevel(Level.ALL);
                logger.setUseParentHandlers(false);
                // 以下配置将打印日志到文件中：系统目录下
                FileHandler fileHandler = new FileHandler();
                fileHandler.setLevel(Level.FINE);
                fileHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(fileHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            /*当前配置为控制台打印所有级别信息*/
            {
                logger.setLevel(Level.ALL);
                logger.setUseParentHandlers(false);
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(Level.ALL);
                consoleHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(consoleHandler);
            }
        }

        private LoggerHolder() {
        }
    }
}