package com.zbb.lizi.algorithm.sort;

import java.util.Arrays;

/**
 * 冒泡排序
 * 前一个与后一个比较大小，大的后移动
 * 时间复杂度为O(n2) 空间复杂度为O(1)因为没有用到额外的空间
 * @author tiancha
 * @since 2019/10/12 9:01
 */
public class BubbleSort {

    public static void main(String[] args) throws Exception {
        int[] array = {3, 1, 4, 2, 7, 8, 6, 5, 9};
        sort(array);
    }

    private static void sort(int[] array) throws Exception {
        if (array == null || array.length == 0) {
            throw new Exception("the array is null or no element...");
        }
        System.out.println("冒泡排序优化前...");
        int n = array.length;
        for (int i = 0; i < n; i++) {
            // 设定一个排序完成的标记
            // 若为 true，则表示此次循环没有进行交换，即待排序列已经有序，排序已然完成
            boolean success = true;
            for (int j = 0; j < n - i - 1; j++) {
                if (array[j] > array[j + 1]) {
                    swap(array, j, j + 1);
                    success = false;
                }
            }
            if (success) {
                break;
            }
            System.out.println("第" + (i + 1) + "轮后: " + Arrays.toString(array));
        }
    }

    private static void swap(int[] nums, int i, int p) {
        if (i < 0 || p < 0 || nums == null || i >= nums.length || p >= nums.length) return;
        int tmp = nums[i];
        nums[i] = nums[p];
        nums[p] = tmp;
    }
}
