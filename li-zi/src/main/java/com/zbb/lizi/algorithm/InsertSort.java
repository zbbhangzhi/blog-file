package com.zbb.lizi.algorithm;

import java.util.Arrays;

/**
 * 插入排序
 * 1. 将首元素作为有序区，其他元素依次与有序区内元素比较，比有序区中任一元素小，就插在它前面
 * 2. 因为插入的动作是依次和有序区内的元素互相交换，所以可以先将插入元素保存，其他需要交换的元素依次向后复制，最后插入元素
 * 时间复杂度为O(n2) 空间复杂度为O(1)
 * @author tiancha
 * @since 2019/10/15 16:48
 */
public class InsertSort {
    public static void main(String[] args) {
        int[] array = {5, 3, 9, 12, 6, 1, 7, 2, 4, 11, 8, 10};
        System.out.println(Arrays.toString(array));
        sort(array);
        System.out.println(Arrays.toString(array));
    }

    private static void sort(int[] array) {
        for (int i = 1; i < array.length; i++) {
            int insertValue = array[i];
            int j = i - 1;
            //从左向右依次比较并复制
            for (; j >= 0 && insertValue < array[j]; j--) {
                array[j + 1] = array[j];
            }
            //插入元素
            array[j + 1] = insertValue;
        }
    }
}
