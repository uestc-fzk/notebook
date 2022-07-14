/**
 * @author fzk
 * @datetime 2022-07-14 22:03
 */
public class MyIntList {
    private int[] elements;
    private int size;

    public MyIntList(int capacity) {
        assert capacity > -1;
        this.elements = new int[capacity];
        size = 0;
    }

    // 返回插入的索引
    public int add(int val) {
        if (size == elements.length) resize();
        elements[size++] = val;
        return size - 1;
    }

    public int get(int index) {
        if (index >= size) throw new IndexOutOfBoundsException("index=" + index + " size=" + size);
        return elements[index];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return elements.length;
    }

    // 将有效部分转为数组
    public int[] toArray() {
        int[] arr = new int[size];
        System.arraycopy(elements, 0, arr, 0, size);
        return arr;
    }

    private void resize() {
        int[] newEle;
        if (elements.length < 2) newEle = new int[2];// 小于2时扩大1.5被无法生效
        else newEle = new int[elements.length + (elements.length >> 1)];// 这里必须加括号，+优先级高于>>
        System.arraycopy(elements, 0, newEle, 0, elements.length);
        elements = newEle;
    }
}
