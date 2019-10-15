package com.zbb.lizi.algorithm;

import java.util.Arrays;

/**
 * 希尔排序
 * 1. 确认增量d：将array上的两个相隔d个元素的数据相互比较并互换位置；（d可以是数组长度逐步折半或精度更高的互质数）
 * 2. 希尔排序可能出现最恶劣的情况：每组内部元素没有任何交换，只能进行插入排序
 * 它先粗略排序，再使用插入排序；是不稳定的排序，相同的两个元素之间会互换位置
 * 时间复杂度为O(n2)
 * @author tiancha
 * @since 2019/10/15 15:04
 */
public class ShellSort {
    public static void main(String[] args) {
        int[] array = {5, 3, 9, 12, 6, 1, 7, 2, 4, 11, 8, 10};
        System.out.println(Arrays.toString(array));
        sort(array);
        System.out.println(Arrays.toString(array));
    }

    private static void sort(int[] array) {
        int d = array.length;
        while (d > 1) {
            //增量d：将array上的两个相隔d个元素的数据相互比较并互换位置
            //多次粗略排序后就是精确排序
            d = d / 2;
            for (int x = 0; x < d; x++) {
                for (int i = x + d; i < array.length; i = i + d) {
                    int temp = array[i];
                    int j;
                    for (j = i - d; j >= 0 && array[j] > temp; j = j - d) {
                        array[j + d] = array[j];
                    }
                    //插入排序优化：暂存比较元素，先复制比比较元素大的，最后再插入到适当位置
                    array[j + d] = temp;
                }
            }
        }
    }

}
