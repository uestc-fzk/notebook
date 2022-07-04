package util;


import java.util.Arrays;

/**
 * 记录一些排序算法模板
 *
 * @author fzk
 * @date 2021-11-27 13:06
 */
public class MySortUtil {

    /**
     * 插入排序 <br/>
     * 稳定性 <br/>
     * 时间复杂度: 平均:O(N^2)  最好: O(N)  最坏: O(N^2) <br/>
     * 空间复杂度：O(1) <br/>
     */
    public static void insertSort(int[] arr, int left, int right) {
        // 从左向右插入, 保证 左边有序
        for (int i = left, j = i; i < right; j = ++i) {
            int ai = arr[i + 1];
            // 这里不采用swap，而是右移，可以减少一半交换
            while (ai < arr[j]) {
                arr[j + 1] = arr[j];
                if (j-- == left) break;
            }
            arr[j + 1] = ai;
        }
    }

    /**
     * 二分插入排序
     * 在插入排序的基础上采用了二分查找，优化了查找性能，在基本有序的情况下，效率反而因二分查找下降
     * 不稳定
     * 时间复杂度：平均O(N*N) 最好：O(N) 最坏：N(N^2)
     *
     * @apiNote 在和基本插入排序相比中，数组长度越长，优化越明显，但是在长数组中，快速排序效率会好很多
     */
    public static void divideInsertSort(int[] arr, int left, int right) {
        // 二分查找插入排序
        for (int i = left, j = i; i < right; j = ++i) {
            int ai = arr[i + 1];
            int lo = left, high = i;
            while (lo <= high) {
                int mid = (lo + high) >> 1;
                if (arr[mid] > ai) high = mid - 1;
                else if (arr[mid] == ai) {
                    lo = mid + 1;
                    break;
                } else
                    lo = mid + 1;
            }
            // 将ai移动到arr[lo]哦
            while (lo <= j) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = ai;
        }
    }

    /**
     * 归并排序: 性能不受输入数据的影响, 始终都是O(NlogN)的时间复杂度, 代价是需要额外的内存空间    <br/>
     * 采用分治法(Divide and Conquer) 的一个非常典型的应用     <br/>
     * 稳定性      <br/>
     * 时间复杂度：O(NlogN)
     * <P>
     * 需要1/2NlgN~NlgN次比较，最多需要访问数组6NlgN次；
     * 归并树状图层数为lgN，每层归并最多访问6N次数组，
     * 2N用于复制，2N用于移动，2N用于比较；
     * </p>
     * <p>
     * 性能优化：递归会使得小规模问题中方法的调用过于频繁，
     * 可以尝试用插入排序处理小规模数组(如n<15)，一般能将归并排序的
     * 运行时间缩短10%~15%。
     * </p>
     * 空间复杂度：O(N)
     */
    public static int[] mergeSort(int[] nums) {
        int len = nums.length;
        int[] res = new int[len];
        System.arraycopy(nums, 0, res, 0, len);//拷贝副本
        merge(res, new int[len], 0, len - 1);// 将副本排序，不破坏原数组
        return res;
    }

    private static void merge(int[] nums, int[] cache, int lo, int high) {
        if (lo >= high) return;

        // 小规模问题直接插入排序
        if (lo + 3 > high) {
            insertSort(nums, lo, high);
            return;
        }

        int mid = (lo + high) >> 1;
        merge(nums, cache, lo, mid);// 左边排序
        merge(nums, cache, mid + 1, high);// 右边排序
        // 归并操作
        // 将nums[lo..mid]和nums[mid+1..hi]归并
        // 将nums[lo..hi]复制到cache[lo..hi]
        System.arraycopy(nums, lo, cache, lo, high + 1 - lo);

        int i = lo, j = mid + 1, index = lo;
        while (i <= mid && j <= high) {
            if (cache[i] <= cache[j])
                nums[index++] = cache[i++];
            else nums[index++] = cache[j++];
        }
        while (i <= mid) nums[index++] = cache[i++];
        while (j <= high) nums[index++] = cache[j++];
    }

    /**
     * 基于最小堆的排序，这里会比最大堆排序稍微麻烦一点，最大堆排序看笔记
     * 每次只能选出堆顶最小的一个元素，将其和堆尾进行交换，再缩小堆长度，从堆顶进行下沉操作
     * 此时得到的是降序的，可以翻转为升序的
     */
    public static void heapSort(int[] arr) {
        int n = arr.length;// n代表堆长度
        // 1.以下沉构建最小堆
        for (int k = (n >> 1) - 1; k >= 0; k--)
            sink(arr, k, n);

        // 2.while循环将a[0]和a[n-1]交换并修复堆（此时n--，堆长减1）,通过sink()重新选择最小元素到a[0]
        while (n > 1) {
            int tmp = arr[0];
            arr[0] = arr[n - 1];
            arr[n - 1] = tmp;
            // 堆缩容并下沉操作
            sink(arr, 0, --n);
        }
        // 3.将降序数组翻转为升序(如果是最大堆，则不需要此步骤)
        for (int left = 0, right = arr.length - 1; left < right; left++, right--) {
            int tmp = arr[left];
            arr[left] = arr[right];
            arr[right] = tmp;
        }
    }

    /**
     * 下沉，最小堆
     *
     * @param arr 堆
     * @param k   需要下沉的堆顶索引，其左子树为2k+1，右子树为2k+2
     * @param n   堆长度
     */
    public static void sink(int[] arr, int k, int n) {
        int leftIndex, rightIndex, minIndex;
        while ((leftIndex = (k << 1) + 1) < n) {// 即存在左子树
            minIndex = k;
            if (arr[leftIndex] < arr[k]) minIndex = leftIndex;

            // 如果存在右子树
            if ((rightIndex = (k << 1) + 2) < n && arr[rightIndex] < arr[minIndex])
                minIndex = rightIndex;

            // 将最小的移到父节点k处
            if (k != minIndex) {
                int tmp = arr[k];
                arr[k] = arr[minIndex];
                arr[minIndex] = tmp;
                k = minIndex;// 循环下沉
            } else break;// 父节点是最小结点，则下沉结束
        }
    }

    /**
     * 上浮，最小堆
     *
     * @param arr 堆
     * @param k   需要上浮的索引，必须是刚加入堆的最后一个结点
     */
    public static void floatUp(int[] arr, int k) {
        int parentIndex;// 父节点索引为(k-1)/2
        while ((parentIndex = ((k - 1) >> 1)) >= 0) {
            if (arr[k] < arr[parentIndex]) {
                int tmp = arr[parentIndex];
                arr[parentIndex] = arr[k];
                arr[k] = tmp;
                k = parentIndex;// 循环上浮
            } else break;// 上浮完成
        }
    }

    /**
     * 基本快速排序 二分法
     * 不稳定
     * <p>
     * 时间复杂度: 平均：O(NlogN) 最坏: O(N^2) 最好: O(NlogN) <br/>
     * 空间复杂度：O(log n) 递归调用栈
     */
    public static void quickSort(int[] nums, int left, int right) {
        if (left < right) {
            // 小数组 插入排序
            if (left + 5 > right) {
                insertSort(nums, left, right);
                return;
            }

            // 以第一个元素进行切分
            int sentinel = nums[left];// 以左边元素为哨兵
            int i = left, j = right;
            while (i < j) {
                while (i <= j && nums[i] <= sentinel) i++; // 找到左边大于哨兵的
                while (i <= j && nums[j] > sentinel) j--;   // 找到右边小于等于哨兵的

                // 这里存在将等于哨兵的进行交换的情况：如果非常多的重复元素，建议用三向切分快速排序
                //  i < j 代表未切分完成，则交换这两个元素
                if (i < j) {
                    int tem = nums[i];
                    nums[i] = nums[j];
                    nums[j] = tem;
                }
            }
            // 切分完成后：此时 j右边都是大于哨兵的  i左边都是小于等于哨兵的 i=j+1
            nums[left] = nums[j]; // 将哨兵与右边第一个小于等于哨兵的元素进行交换
            nums[j] = sentinel;

            quickSort(nums, left, j - 1);
            quickSort(nums, i, right);
        }
    }

    /**
     * 双枢轴快速排序: 来源于jdk
     *
     * @see java.util.DualPivotQuicksort
     */
    public static void dualPivotQuickSort(int[] a, int left, int right) {
        int length = right - left + 1;

        // Use insertion dualPivotQuickSort on tiny arrays
        if (length < 47) {
            insertSort(a, left, right);
            return;
        }

        // 长度除以7的近似值
        int seventh = (length >> 3) + (length >> 6) + 1;
        // 选择与中心元素间隔均匀的5个基准元素
        int e3 = (left + right) >>> 1; // 中心元素
        int e2 = e3 - seventh;
        int e1 = e2 - seventh;
        int e4 = e3 + seventh;
        int e5 = e4 + seventh;

        // 对5个基准元素按插入排序进行排序
        if (a[e2] < a[e1]) {
            int t = a[e2];
            a[e2] = a[e1];
            a[e1] = t;
        }

        if (a[e3] < a[e2]) {
            int t = a[e3];
            a[e3] = a[e2];
            a[e2] = t;
            if (t < a[e1]) {
                a[e2] = a[e1];
                a[e1] = t;
            }
        }
        if (a[e4] < a[e3]) {
            int t = a[e4];
            a[e4] = a[e3];
            a[e3] = t;
            if (t < a[e2]) {
                a[e3] = a[e2];
                a[e2] = t;
                if (t < a[e1]) {
                    a[e2] = a[e1];
                    a[e1] = t;
                }
            }
        }
        if (a[e5] < a[e4]) {
            int t = a[e5];
            a[e5] = a[e4];
            a[e4] = t;
            if (t < a[e3]) {
                a[e4] = a[e3];
                a[e3] = t;
                if (t < a[e2]) {
                    a[e3] = a[e2];
                    a[e2] = t;
                    if (t < a[e1]) {
                        a[e2] = a[e1];
                        a[e1] = t;
                    }
                }
            }
        }


        // Pointers
        int less = left;  // center part 的第一个元素
        int great = right; // right part 的第一个元素前一个索引，切分完成后center part的最后一个元素

        // 这5个基准元素中只要有相同的存在就转为单枢轴快速排序
        if (a[e1] != a[e2] && a[e2] != a[e3] && a[e3] != a[e4] && a[e4] != a[e5]) {
            /*
             * 使用五个排序基准元素中的第二个和第四个作为枢轴
             * pivot1必须小于pivot2
             */
            int pivot1 = a[e2];
            int pivot2 = a[e4];

            /*
             * 要排序的第一个和最后一个元素被移动到以前由枢轴占据的位置。
             * 分区完成后，枢轴被交换回其最终位置，并从后续排序中排除。
             */
            a[e2] = a[left];
            a[e4] = a[right];

            /*
             * 跳过小于或大于枢轴值的元素
             */
            while (a[++less] < pivot1) ;
            while (a[--great] > pivot2) ;

            /*
             * Partitioning:
             *
             *   left part           center part                   right part
             * +--------------------------------------------------------------+
             * |  < pivot1  |  pivot1 <= && <= pivot2  |    ?    |  > pivot2  |
             * +--------------------------------------------------------------+
             *               ^                          ^       ^
             *               |                          |       |
             *              less                        k     great
             *
             * Invariants:
             *
             *              all in (left, less)   < pivot1
             *    pivot1 <= all in [less, k)     <= pivot2
             *              all in (great, right) > pivot2
             *
             * Pointer k is the first index of ?-part.
             */
            outer:
            for (int k = less - 1; ++k <= great; ) {
                int ak = a[k];
                if (ak < pivot1) { // 移动 a[k] 到 left part
                    a[k] = a[less];
                    a[less] = ak;
                    ++less;
                } else if (ak > pivot2) { // 移动 a[k] 到 right part
                    while (a[great] > pivot2) {
                        if (great-- == k) {
                            break outer;
                        }
                    }
                    if (a[great] < pivot1) { // a[great] <= pivot2
                        a[k] = a[less];
                        a[less] = a[great];
                        ++less;
                    } else { // pivot1 <= a[great] <= pivot2
                        a[k] = a[great];
                    }
                    a[great] = ak;
                    --great;
                }
            }

            // 交换枢轴到它们最终位置
            a[left] = a[less - 1];
            a[less - 1] = pivot1;
            a[right] = a[great + 1];
            a[great + 1] = pivot2;

            // 递归排序左右部分，不包括已知的枢轴
            dualPivotQuickSort(a, left, less - 2);
            dualPivotQuickSort(a, great + 2, right);

            /*
             * 如果中心部分太大（即> 4/7），将内部枢轴值交换到末端
             */
            if (less < e1 && e5 < great) {
                // 跳过与枢轴值相等的元素
                while (a[less] == pivot1) {
                    ++less;
                }

                while (a[great] == pivot2) {
                    --great;
                }

                /*
                 * Partitioning:
                 *
                 *   left part         center part                  right part
                 * +----------------------------------------------------------+
                 * | == pivot1 |  pivot1 < && < pivot2  |    ?    | == pivot2 |
                 * +----------------------------------------------------------+
                 *              ^                        ^       ^
                 *              |                        |       |
                 *             less                      k     great
                 *
                 * Invariants:
                 *
                 *              all in (*,  less) == pivot1
                 *     pivot1 < all in [less,  k)  < pivot2
                 *              all in (great, *) == pivot2
                 *
                 * Pointer k is the first index of ?-part.
                 */
                outer:
                for (int k = less - 1; ++k <= great; ) {
                    int ak = a[k];
                    if (ak == pivot1) { // Move a[k] to left part
                        a[k] = a[less];
                        a[less] = ak;
                        ++less;
                    } else if (ak == pivot2) { // Move a[k] to right part
                        while (a[great] == pivot2) {
                            if (great-- == k) {
                                break outer;
                            }
                        }
                        if (a[great] == pivot1) { // a[great] < pivot2
                            a[k] = a[less];
                            /*
                             * 即使a[great] 等于pivot1，如果a[great] 和pivot1 是不同符号的浮点零，
                             * 赋值a[less] = pivot1 也可能是不正确的。
                             * 因此，在 float 和 double 排序方法中，我们必须使用更准确的赋值 a[less] = a[great]。
                             */
                            a[less] = pivot1;
                            ++less;
                        } else { // pivot1 < a[great] < pivot2
                            a[k] = a[great];
                        }
                        a[great] = ak;
                        --great;
                    }
                }
            }

            // 递归排序中间部分
            dualPivotQuickSort(a, less, great);

        }
        // 5个基准元素存在相同的情况下，就执行单枢轴排序
        else {
            // 以e3作为枢轴
            int pivot = a[e3];

            /*
             * 三向切分
             *
             *   left part    center part              right part
             * +-------------------------------------------------+
             * |  < pivot  |   == pivot   |     ?    |  > pivot  |
             * +-------------------------------------------------+
             *              ^              ^        ^
             *              |              |        |
             *             less            k      great
             *
             * Invariants:
             *
             *   all in (left, less)   < pivot
             *   all in [less, k)     == pivot
             *   all in (great, right) > pivot
             *
             * Pointer k is the first index of ?-part.
             */
            for (int k = less; k <= great; ++k) {
                if (a[k] == pivot) {
                    continue;
                }
                int ak = a[k];
                if (ak < pivot) { // 移动 a[k] 到 left part
                    a[k] = a[less];
                    a[less] = ak;
                    ++less;
                } else { // a[k] > pivot - 移动 a[k] 到 right part
                    while (a[great] > pivot) {
                        --great;
                    }
                    if (a[great] < pivot) { // a[great] <= pivot
                        a[k] = a[less];
                        a[less] = a[great];
                        ++less;
                    } else { // a[great] == pivot
                        /* 即使 a[great] 等于 pivot，赋值 a[k] = pivot 也可能是不正确的，
                         * 如果 a[great] 和 pivot 是不同符号的浮点零。因此，在 float 和 double 排序方法中，
                         * 我们必须使用更准确的赋值 a[k] = a[great]。
                         */
                        a[k] = pivot;
                    }
                    a[great] = ak;
                    --great;
                }
            }

            // 递归排序left part和 right part
            dualPivotQuickSort(a, left, less - 1);
            dualPivotQuickSort(a, great + 1, right);
        }
    }

    /**
     * 归并排序中的最大运行次数
     */
    private static final int MAX_RUN_COUNT = 67;

    public static void timSort(int[] a, int left, int right) {
        doTimSort(a, left, right, null, 0, 0);
    }

    /**
     * TimSort 高级merge sort 来源于jdk
     *
     * @param work     工作数组,可为null
     * @param workBase 工作数组中可用空间的来源
     * @param workLen  工作数组可用大小
     * @see java.util.DualPivotQuicksort#sort(int[], int, int, int[], int, int)
     */
    private static void doTimSort(int[] a, int left, int right, int[] work, int workBase, int workLen) {
        // 小数组用快速排序
        if (right - left < 286) {
            dualPivotQuickSort(a, left, right);
            return;
        }

        /*索引 run[i] 是第 i 个run的开始索引（升序或降序）*/
        int[] run = new int[MAX_RUN_COUNT + 1];
        int count = 0;
        run[0] = left;

        // 检查数组是否已经有序
        for (int k = left; k < right; run[count] = k) {
            // 数组开头的相等项
            while (k < right && a[k] == a[k + 1])
                k++;
            if (k == right) break;  // 全部相等则退出循环
            if (a[k] < a[k + 1]) { // 升序run
                while (++k <= right && a[k - 1] <= a[k]) ;
            } else if (a[k] > a[k + 1]) { // 降序run
                while (++k <= right && a[k - 1] >= a[k]) ;
                // 将降序run调整为升序run
                for (int lo = run[count] - 1, hi = k; ++lo < --hi; ) {
                    int t = a[lo];
                    a[lo] = a[hi];
                    a[hi] = t;
                }
            }

            // 合并因调整序列之后出现的连续升序的两个run
            if (run[count] > left && a[run[count]] >= a[run[count] - 1]) {
                count--;
            }

            /*
             * 如果数组切分出来的run非常多，说明数组不是高度结构化的，
             * 使用快速排序而不是归并排序。
             */
            if (++count == MAX_RUN_COUNT) {
                dualPivotQuickSort(a, left, right);
                return;
            }
        }

        // 这些 不变量 应该为true:
        //    run[0] = 0
        //    run[<last>] = right + 1; (terminator)

        if (count == 0) {
            // 1个run，直接返回
            return;
        } else if (count == 1 && run[count] > right) {
            /* 无论是一个升序run，还是一个降序被调整为升序的run
               总是检查最后一个run是不是一个正确的终止符
               否则我们将有一个未终止的尾随运行，以处理下游。*/
            return;
        }
        right++;
        if (run[count] < right) {
            /*转角情况：最后的运行不是终结者。
            如果最终run是equal run ,或者最后是单元素run，则可能会发生这种情况。
            通过在末尾添加适当的终止符来修复。请注意，我们以 (right + 1) 终止*/
            run[++count] = right;
        }

        // 确定合并的交替基准
        byte odd = 0;
        for (int n = 1; (n <<= 1) < count; odd ^= 1) ;

        // 使用或创建缓存数组来归并
        int[] b;                 // 缓冲数组;
        int ao, bo;              // 'left' 的数组偏移量
        int blen = right - left; // b数组需要的长度
        if (work == null || workLen < blen || workBase + blen > work.length) {
            work = new int[blen];
            workBase = 0;
        }
        if (odd == 0) {
            System.arraycopy(a, left, work, workBase, blen);
            b = a;
            bo = 0;
            a = work;
            ao = workBase - left;
        } else {
            b = work;
            ao = 0;
            bo = workBase - left;
        }

        // 合并
        for (int last; count > 1; count = last) {
            for (int k = (last = 0) + 2; k <= count; k += 2) {
                int hi = run[k], mi = run[k - 1];
                for (int i = run[k - 2], p = i, q = mi; i < hi; ++i) {
                    if (q >= hi || p < mi && a[p + ao] <= a[q + ao]) {
                        b[i + bo] = a[p++ + ao];
                    } else {
                        b[i + bo] = a[q++ + ao];
                    }
                }
                run[++last] = hi;
            }
            if ((count & 1) != 0) {
                for (int i = right, lo = run[count - 1]; --i >= lo;
                     b[i + bo] = a[i + ao]
                )
                    ;
                run[++last] = right;
            }
            int[] t = a;
            a = b;
            b = t;
            int o = ao;
            ao = bo;
            bo = o;
        }
    }

    private static void swap(int[] nums, int i, int j) {
        int tmp = nums[i];
        nums[i] = nums[j];
        nums[j] = tmp;
    }
}
