package util;

import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

/**
 * @author fzk
 * @date 2021-11-14 20:41
 */
@SuppressWarnings("unused")
public class MyBinarySearchUtil {
    private static final int NOT_FIND = -1;
    private static final int ILLEGAL = -2;

    public static int binarySearch(int[] nums, int target) {
        if (nums == null) throw new IllegalArgumentException("nums can not be null");
        if (nums.length == 0) return ILLEGAL;

        int lo = 0, high = nums.length - 1;
        while (lo <= high) {
            int mid = (lo + high) >> 1;
            if (target < nums[mid]) high = mid - 1;
            else if (target > nums[mid]) lo = mid + 1;
            else return mid;
        }
        return NOT_FIND;
    }

    public static <T extends Comparable<? super T>> int binarySearch(T[] nums, Comparable<? super T> target) {
        if (nums == null || target == null) throw new IllegalArgumentException("nums or target may be null");
        if (nums.length == 0) return ILLEGAL;

        int lo = 0, high = nums.length - 1;
        while (lo <= high) {
            int mid = (lo + high) >> 1;
            int cmp = target.compareTo(nums[mid]);
            if (cmp < 0) high = mid - 1;
            else if (cmp > 0) lo = mid + 1;
            else return mid;
        }
        return NOT_FIND;
    }

    public static <T extends Comparable<? super T>> int binarySearch(List<T> list, Comparable<? super T> target) {
        if (list == null || target == null) throw new IllegalArgumentException("list or target may be null");
        if (list.size() == 0) return ILLEGAL;

        if (!(list instanceof RandomAccess))
            throw new IllegalArgumentException("your list should be random-access , such as ArrayList");

        int lo = 0, high = list.size() - 1;
        while (lo <= high) {
            int mid = (lo + high) >> 1;
            int cmp = target.compareTo(list.get(mid));
            if (cmp < 0) high = mid - 1;
            else if (cmp > 0) lo = mid + 1;
            else return mid;
        }
        return NOT_FIND;
    }

    public static <T> int binarySearch(T[] nums, T target, Comparator<T> comparator) {
        if (nums == null || target == null) throw new IllegalArgumentException("nums or target may be null");
        int lo = 0, high;
        if ((high = nums.length - 1) == -1) return ILLEGAL;

        while (lo <= high) {
            int mid = (lo + high) >> 1;
            int cmp = comparator.compare(target, nums[mid]);
            if (cmp < 0) high = mid - 1;
            else if (cmp > 0) lo = mid + 1;
            else return mid;
        }
        return NOT_FIND;
    }

    /**
     * 二分查找：小于target的最后一个索引
     *
     * @param nums   数组
     * @param target 目标
     * @return 最后一个小于target的索引 或 -1
     */
    public static int binarySearchLastMinForTarget(int[] nums, int target) {
        if (nums == null) throw new IllegalArgumentException("nums can not be null");
        if (nums.length == 0) return -1;
        if (nums[0] >= target) return -1;//全都 >= target
        //if (nums[nums.length - 1] < target) return nums.length - 1;//全都 < target

        int lo = 0, high = nums.length - 1;
        while (lo <= high) {
            int mid = (lo + high) >> 1;
            if (target < nums[mid]) high = mid - 1;
            else if (target > nums[mid]) lo = mid + 1;
            else {
                lo = mid;
                break;
            }
        }
        if (lo >= nums.length) return nums.length - 1;//全都比target小

        // 此时，nums[lo]>target，则是lo-1；nums[lo]==target，向前滑动
        while (nums[--lo] >= target) continue;// 应该不会减到-1
        return lo;
    }
}
