package util;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 数据结构工具类
 *
 * @author fzk
 * @date 2021-12-30 9:20
 */
public class MyDataStructureUtil {
    /**
     * k个一组翻转链表
     *
     * @param head 待翻转结点
     * @param k    几个一组
     * @return 翻转后的头结点
     */
    public static ListNode reverseKGroup(ListNode head, int k) {
        ListNode firstOfGroup = head, lastOfGroup,
                lastTail = null, result = null;
        while (firstOfGroup != null) {
            lastOfGroup = firstOfGroup;
            int groupLen;// 代表此组长度
            for (groupLen = 1; groupLen < k && lastOfGroup.next != null; groupLen++)
                lastOfGroup = lastOfGroup.next;

            ListNode firstOfNextGroup = lastOfGroup.next;
            // 最后一组，且不为k的话，则不翻转
            if (groupLen < k) {
                if (lastTail == null) result = head;// 说明第一组就不为k了
                else
                    lastTail.next = firstOfGroup;// 衔接上一组

                break;
            }

            ListNode headOfGroup = reversePart(firstOfGroup, lastOfGroup);// 这里翻转的可能是k个，也可能没有k个
            if (lastTail == null) // 说明是第一组
                result = headOfGroup;
            else // 接上上一组
                lastTail.next = headOfGroup;
            lastTail = firstOfGroup;

            firstOfGroup = firstOfNextGroup;
        }

        return result;
    }

    /*部分翻转，返回翻转后的头结点*/
    public static ListNode reversePart(ListNode head, ListNode tail) {
        if (head == null) return null;

        ListNode pre = null, curr = head;
        while (tail != pre) {
            ListNode next = curr.next;
            curr.next = pre;
            pre = curr;
            curr = next;
        }

        return pre;// 翻转前的尾结点，翻转后的头结点
    }

    /*链表翻转*/
    public static ListNode reverse(ListNode head) {
        if (head == null) return null;

        ListNode pre = null, curr = head;
        while (curr != null) {
            ListNode next = curr.next;

            curr.next = pre;
            pre = curr;
            curr = next;
        }
        return pre;
    }


    /**
     * 表示空树，来保证队列中不存在null
     */
    private static final TreeNode nullTreeNode = new TreeNode(-1);

    /**
     * 按照力扣的二叉树序列化方式进行序列化和反序列化
     * 使用广度优先思想
     *
     * @param root 待序列化树
     * @return 序列化结果
     * @author fzk
     * @date 2021-12-30 0:17
     */
    public static String serialize(TreeNode root) {
        if (root == null) return "[]";

        StringBuilder builder = new StringBuilder();
        builder.append("[");

        Queue<TreeNode> queue = new LinkedList<>();
        // 保证队列中不存在null
        queue.offer(root);
        while (queue.size() != 0) {
            TreeNode poll = queue.poll();
            if (poll != nullTreeNode) {
                builder.append(poll.val).append(",");
                queue.offer(poll.left == null ? nullTreeNode : poll.left);
                queue.offer(poll.right == null ? nullTreeNode : poll.right);
            } else
                builder.append("null,");
        }
        builder.deleteCharAt(builder.length() - 1);// 删掉最后的','
        builder.append("]");

        String s = builder.substring(1, builder.length() - 1);// 准备删除末尾的null
        String[] nodes = s.split(",");
        int tailNullCount = 0;// 末尾的null的数量
        for (int i = nodes.length - 1; i > -1; i--) {
            if ("null".equals(nodes[i])) tailNullCount++;
            else break;
        }

        return builder.delete(builder.length() - 1 - 5 * tailNullCount, builder.length() - 1)
                .toString();
    }

    /**
     * 反序列化树
     *
     * @param data 字符串
     * @return 树或者null
     */
    public static TreeNode deserialize(String data) {
        if ("[]".equals(data)) return null;

        data = data.substring(1, data.length() - 1);
        String[] nodes = data.split(",");
        Queue<TreeNode> queue = new LinkedList<>();
        TreeNode root = new TreeNode(Integer.parseInt(nodes[0]));
        queue.offer(root);
        int index = 1;
        while (queue.size() != 0 && index < nodes.length) {
            TreeNode poll = queue.poll();
            // 左子树处理
            if (!"null".equals(nodes[index++])) {
                poll.left = new TreeNode(Integer.parseInt(nodes[index - 1]));
                queue.offer(poll.left);
            }
            // 右子树处理
            if (index >= nodes.length) break;// 先判断是否会越界
            if (!"null".equals(nodes[index++])) {
                poll.right = new TreeNode(Integer.parseInt(nodes[index - 1]));
                queue.offer(poll.right);
            }
        }
        return root;
    }

    // 最简单的单链链表
    private static class ListNode {
        public int val;
        public ListNode next;

        public ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }

        @Override
        public String toString() {
            return "ListNode{" +
                    "val=" + val +
                    ", next=" + next +
                    '}';
        }
    }

    // 最简单的二叉树
    private static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "TreeNode{" +
                    "val=" + val +
                    ", left=" + left +
                    ", right=" + right +
                    '}';
        }
    }

    // 二叉树的迭代器
    public static class BSTIterator {
        //中序遍历二叉树迭代器
        BSTIterator.ListNode head;
        BSTIterator.ListNode tail;

        public BSTIterator(TreeNode root) {
            // 链表头结点
            head = new BSTIterator.ListNode(Integer.MIN_VALUE, null);
            tail = head;
            dfs(root);
        }

        private void dfs(TreeNode root) {
            if (root != null) {
                dfs(root.left);
                tail.next = new BSTIterator.ListNode(root.val, null);
                tail = tail.next;
                dfs(root.right);
            }
        }

        public int next() {// 会使得指针右移
            head = head.next;
            return head.val;
        }

        public boolean hasNext() {
            return head.next != null;
        }

        private static class ListNode {
            public int val;
            public BSTIterator.ListNode next;

            public ListNode(int val, BSTIterator.ListNode next) {
                this.val = val;
                this.next = next;
            }
        }
    }
}
