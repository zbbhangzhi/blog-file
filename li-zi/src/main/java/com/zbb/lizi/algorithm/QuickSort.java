package com.zbb.lizi.algorithm;

import java.util.Arrays;

/**
 * 快速排序
 * 是冒泡排序的改进 先选取一个元素 然后将所有比它小的数据放在它前面 所有比它大的数据放在它后面
 * 面对多个相等的元素 可能出现不稳定的情况
 * @author tiancha
 * @since 2019/10/12 9:32
 */
public class QuickSort {
    // 用于记录 partition 操作的次数
    private static int count = 0;

    public static void main(String[] args) {
        int[] array = {3, 9, 1, 4, 2, 7, 8, 6, 5};
        int length = array.length;
        sort(array, 0, length - 1);
    }

    private static void sort(int[] array, int left, int right) {
        if (left >= right) {
            return;
        }
        int p = partition(array, left, right);
        sort(array, left, p - 1);
        sort(array, p + 1, right);
    }

    /**
     * 对 array[left, right] 部分进行分区 (partition) 操作
     *
     * @param array
     * @param left  数组的起始索引
     * @param right 数组的结束索引
     * @return 返回值为 p, 使得 array[left, p - 1] < array[p] < array[p + 1, right]
     */
    private static int partition(int[] array, int left, int right) {
        count++;
        int v = array[left];
        int j = left;
        // array[left + 1, j] < v, 初始状态时 j = left, 那么 array[left + 1, left] 是不存在的，保证了边界有效性
        // array[j + 1, i) > v, 右侧取开区间, 初始状态时 i = left + 1, 那么 array[left + 1, left + 1) 是不存在的，也保证了边界有效性
        for (int i = left + 1; i <= right; i++) {
            if (array[i] < v) {
                swap(array, j + 1, i);
                System.out.println("第 " + count + " 次比较的结果: " + Arrays.toString(array));
                j++;
            }
        }
        swap(array, left, j);
//        System.out.println("第 " + count + " 次分区的结果: " + Arrays.toString(array));
        return j;
    }

    private static void swap(int[] array, int a, int b) {
        if (a == b) {
            return;
        }
        int temp = array[a];
        array[a] = array[b];
        array[b] = temp;
    }
}
