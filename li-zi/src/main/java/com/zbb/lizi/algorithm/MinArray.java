package com.zbb.lizi.algorithm;

/**
 * @author tiancha
 * @since 2020/6/22 19:42
 */
public class MinArray {
    public static void main(String[] args) {
        /**
         * 题目：给到一个旋转数组类似于56789123，找出数组内最小的元素
         * 思路：根据二分法找出旋转数组的旋转点
         * 时间复杂度：O(log2N)
         * 空间复杂度：O(1)
         */
        int[] arr = new int[]{5,6,7,8,9,1,2,3};
        int start = 0;
        int end = arr.length - 1;
        int mid = (start + end)/2;
        int result;
        // 保证一直在这个递增区间内
        while(start < end){
            // 如果mid比end大，说明旋转点在mid，end中
            // {5,6,7,8,9,1,2,3}
            if (arr[mid] > arr[end]){
                start = mid + 1;
            }
            // 如果mid比end大，说明旋转点在start，mid中
            // {1,2,3,5,6,7,8,9}
            else if (arr[mid] < arr[end]){
                end = mid;
            }
            // 如果mid比end一样，只能循环遍历
            // 比如 [1, 0, 1, 1, 1]
            // 时间复杂度：O(n)
            else {
                result = arr[start];
                for(int i = start;i <= end;i++){
                    if (arr[i] < result) {
                        result = arr[i];
                    }
                }
            }
        }
        // result就是最小的值，因为旋转数组本身就是递增的
        result = arr[start];
    }
}
