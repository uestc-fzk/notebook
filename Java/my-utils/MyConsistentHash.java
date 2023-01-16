package util;

import java.util.*;
import java.util.zip.CRC32;

/**
 * 一致性hash实现
 * 我个人觉得，用数组模拟环，二分查找效率绝对比TreeMap高
 * [-100, 123 , 245 , 900 ]， 这种数组模拟情况下，根据hash值二分查找效率更高
 *
 * @author fzk
 * @datetime 2022-09-11 10:10
 */
public class MyConsistentHash {
    /**
     * 插入虚拟节点的目的是使得节点在环上尽可能的分布均匀，避免部分节点压力过大
     *
     * @param podNodes        key-->hostname value--> ip:port
     * @param visualNodeCount 虚拟节点数量，需>=1
     */
    public MyConsistentHash(Set<PodNode> podNodes, int visualNodeCount) {
        assert visualNodeCount >= 1;
        treeMap = new TreeMap<>();
        for (PodNode pod : podNodes) {
            // 插入真实节点
            int hash = hash(pod.hostname);
            treeMap.put(hash, pod);
            // 插入虚拟节点
            for (int i = 1; i < visualNodeCount; i++) {
                hash = hash(pod.hostname + "_visual_" + i);
                treeMap.put(hash, pod);
            }
        }
        //System.out.println(treeMap.keySet());
    }

    public PodNode getPodNode(String key) {
        // 环形匹配
        int hash = hash(key);
        SortedMap<Integer, PodNode> tailMap = treeMap.tailMap(hash);
        if (tailMap.size() != 0) return treeMap.get(tailMap.firstKey());
        return treeMap.firstEntry().getValue();
    }

    public static class PodNode {
        public String hostname;
        public String ip;
        public int port;

        public PodNode(String hostname) {
            this.hostname = hostname;
        }
    }

    // 一致性哈希环
    private final TreeMap<Integer, PodNode> treeMap;

    public static void main(String[] args) {
        Set<PodNode> set = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            set.add(new PodNode("node" + i));

        }
        MyConsistentHash hashSet = new MyConsistentHash(set, 3);
        for (int i = 0; i < 10; i++) {
            System.out.println(hashSet.getPodNode("user:" + i + "_2022").hostname);
            System.out.println(hashSet.getPodNode("blog:" + i + "_2022").hostname);
        }
    }

    // 默认情况未重写Hashcode方法时，其hashCode()返回地址，很可能是连续的
    private static int hash(String key) {
        CRC32 crc32 = new CRC32();
        crc32.update(key.getBytes());
        return (int) crc32.getValue();
    }
}
