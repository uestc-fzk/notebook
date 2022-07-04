package util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author fzk
 * @date 2021-11-22 17:18
 */
@SuppressWarnings("unused")
public class MyStreamUtil {

    public static List<Integer> getRandomList(int length) {
        assert length > 0 : "length 必须大于0";
        return Stream.generate(ThreadLocalRandom.current()::nextInt)
                .limit(length)
                //.sorted() // 应该不需要排序
//                .collect(Collectors.toList()); // 这两个是差不多的，但是自定义数组初始容量更好点嘛
                .collect(() -> new ArrayList<>(length), List::add, List::addAll);
    }

    public static List<Integer> getRandomList(int length, int origin, int bound) {
        assert length > 0 : "length 必须大于0";
        assert origin < bound : "origin must smaller than bound";

        ThreadLocalRandom random = ThreadLocalRandom.current();
        return IntStream.generate(() -> random.nextInt(origin, bound))
                .limit(length)
                //.sorted()
                .collect(() -> new ArrayList<>(length), List::add, List::addAll);
    }

    public static int[] getRandomInts(int length) {
        assert length > 0 : "length 必须大于0";

        return ThreadLocalRandom.current()
                .ints()
                .limit(length)
                .toArray();
    }

    /*返回序列的范围在[origin,bound) 即左闭右开*/
    public static int[] getRandomInts(int length, int origin, int bound) {
        assert length > 0 : "length 必须大于0";
        assert origin < bound : "origin must smaller than bound";

        return ThreadLocalRandom.current()
                .ints(length, origin, bound)
                .toArray();
    }

    public static IntSummaryStatistics summaryStatistics(List<Integer> list) {
        assert list != null && list.size() != 0 : "list 不能为空";
        return list.stream()
                .collect(Collectors.summarizingInt(i -> i));
    }

    /**
     * reduce 规约 最终操作
     * 允许通过指定的函数来将stream中的多个元素规约为一个元素
     * 闭区间[start,end]
     */
    public static int minVal(int[] nums, int start, int end) {
        return Arrays.stream(nums, start, end + 1)
                .reduce(nums[0], Math::min);// nums[0]作为起始种子
    }
}
