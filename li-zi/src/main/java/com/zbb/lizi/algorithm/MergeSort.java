package com.zbb.lizi.algorithm;

import java.util.Arrays;

/**
 * 归并排序
 * 1. 递归的核心：将一个集合拆分到不能再拆分；
 * 2. 将拆分出来的左右小组，依次比较大小，将比较结果放回集合（每次比较结果都无需保存，所以空间复杂度为O(1)
 * 时间复杂度：每层归并排序计算次数 n * 分层层级数 logn = O(nlogn)
 * @author tiancha
 * @since 2019/10/14 21:58
 */
public class MergeSort {
    public static void main(String[] args) {
        int[] array = {5, 8, 6, 3, 9, 2, 1, 7};
        mergeSort(array, 0, array.length - 1);
        System.out.println(Arrays.toString(array));
    }

    private static void mergeSort(int[] array, int start, int end) {
        if (start < end) {
            //递归拆分成若干小组，直到每组只有一个
            int mid = (start + end) / 2;
            mergeSort(array, start, mid);
            mergeSort(array, mid + 1, end);
            //将拆分的两个组 先比较大小后组合
            merge(array, start, mid, end);
        }
    }

    private static void merge(int[] array, int start, int mid, int end) {
        int[] tempArray = new int[end - start + 1];
        //p1/p2是左右小组的游标
        int p1 = start, p2 = mid + 1, p = 0;
        //比较组内元素大小 依次放入大集合
        while (p1 <= mid && p2 <= end) {
            if (array[p1] <= array[p2]) {
                tempArray[p++] = array[p1++];
            } else {
                tempArray[p++] = array[p2++];
            }
        }
        //左/右组内还有元素 依次放入大集合尾部
        while (p1 <= mid) {
            tempArray[p++] = array[p1++];
        }
        while (p2 <= end) {
            tempArray[p++] = array[p2++];
        }
        //复制大集合的元素到原数组
        for (int i = 0; i < tempArray.length; i++) {
            array[i + start] = tempArray[i];
        }
    }
}
