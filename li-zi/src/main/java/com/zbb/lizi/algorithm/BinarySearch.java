package com.zbb.lizi.algorithm;

/**
 * @author tiancha
 * @since 2020/6/29 20:08
 */
public class BinarySearch {
    public static void main(String[] args) {
        int target = 2;
        int[] nums = {1, 2, 4, 5, 7};
        int left = 0;
        int right = nums.length - 1;
        // right没有越界
        while (left <= right){
            // 防止数值过大溢出 mid已经计算过 就要+1-1
            int mid = (left + (right - left)) / 2;
            if (nums[mid] == target){
                right = mid - 1;
            }
            else if (nums[mid] > target){
                right = mid - 1;
            }
            else if (nums[mid] < target){
                left = mid + 1;
            }
        }
        System.out.println(left);
    }
}
