package util;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

/**
 * 原本用Date类，渐渐改进为用 LocalDate 类以及 LocalDateTime 类 <br/>
 * LocalDate类辅以日期调整期类TemporalAdjusters类可以实现返回某年某月第n个星期的星期几
 * <p>
 * DateTimeFormatter 类被设计为代替 DateFormat 类
 * </p>
 *
 * @author fzk
 * @date 2021-07-15 21:34
 */
@SuppressWarnings("unused")
public class MyDateTimeUtil {
    /**
     * SimpleDateFormat不是线程安全的，多个线程同时调用format方法会使得内部数据结构被并发访问破坏
     * 使用同步开销大，使用局部变量有点浪费
     * 所以使用 <strong>线程局部变量<strong/>
     * <p>
     * 由于LocalDateTime的默认toString()方法是以'T'连接日期和时间，因此必须自定义DateTimeFormatter
     * DateTimeFormatter是线程安全的，是不必要放入到线程局部变量的呢
     * </p>
     */
    private static final ThreadLocal<DateTimeFormatter> dateTimeFormatter;

    static {
        dateTimeFormatter = ThreadLocal.withInitial(() ->
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 这个性能是SimpleDateFormat的几倍
     *
     * @return 日期，如：2021-11-24
     */
    public static String nowDate() {
        return LocalDate.now().toString();
    }

    /**
     * 获取当前日期时间, 如：2021-11-24 22:09:11
     *
     * @return 日期时间
     * @apiNote 性能是SimpleDateFormat的2倍
     */
    public static String nowDateTime() {
        return dateTimeFormatter.get().format(LocalDateTime.now());
    }

    /**
     * 自定义日期或时间格式
     *
     * @param pattern 自定义格式，如：yyyy-MM-dd HH:mm:ss 为24小时制;  yyyy-MM-dd KK:mm:ss 为12小时制
     * @return 返回当前的日期或时间
     * @apiNote 这个的性能又是SimpleDateFormatter的2倍
     */
    public static String nowDateOrTime(String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return formatter.format(LocalDateTime.now());
    }

    /**
     * 获取本月第一天，以日期调整器TemporalAdjuster调整LocalDate实现
     *
     * @return 本月第一天
     */
    public static String getFirstDayOfMonth() {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).toString();
    }

    /**
     * 获取本月最后一天，以日期调整器TemporalAdjuster调整LocalDate实现
     *
     * @return 本月最后一天
     */
    public static String getLastDayOfMonth() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).toString();
    }

    /**
     * 判断是否是每月第一天
     *
     * @param date 字符串即可
     * @return 只有字符串规范化且是每月第一天才会返回true
     */
    public static boolean isFirstDayOfMonth(String date) {
        try {
            LocalDate parse = LocalDate.parse(date);
            return parse.getDayOfMonth() == 1;
        } catch (DateTimeParseException e) {
            return false;
        }
    }


    /**
     * 判断是否是最后一天
     *
     * @param date 字符串即可
     * @return 只有是规范化的字符串且是每月最后一天才返回true
     */
    public static boolean isLastDayOfMonth(String date) {
        try {
            LocalDate parse = LocalDate.parse(date);
            return parse.plusDays(1).getDayOfMonth() == 1;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否符合日期规范 如 2021-07-22
     *
     * @param date 字符串
     * @return 传入字符串规范化返回true
     */
    public static boolean isDateFormat(String date) {
        try {
            LocalDate.parse(date);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否符合日期时间规范 如 2021-11-24 22:25:11
     *
     * @param dateTime 字符串
     * @return 传入字符串规范化返回true
     */
    public static boolean isDateTimeFormat(String dateTime) {
        try {
            dateTimeFormatter.get().parse(dateTime);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 用于判断开始日期和结束日期格式是否都正确且结束日期大于开始日期
     *
     * @param startDate 正确为2021-07-01这种
     * @param endDate   正确为2021-07-31这种
     * @return 正确返回true，其他情况返回false
     */
    public static boolean isStartAndEndRight(String startDate, String endDate) {
        return MyDateTimeUtil.isFirstDayOfMonth(startDate) &&
                MyDateTimeUtil.isLastDayOfMonth(endDate) &&
                startDate.compareTo(endDate) < 0;
    }
}
