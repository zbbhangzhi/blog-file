package com.zbb.lizi.algorithm.doublePoint;

/**
 * 数组有序，查找数组中两数之和为target
 *
 * @author tiancha
 * @since 2020/7/2 16:20
 */
public class TwoSum {
    public static void main(String[] args) {
        int[] nums = {2, 7, 11, 15, 16};
        int target = 23;

        int left = 0;
        // 不可重复
        int right = nums.length - 1;
        while (left < right) {
            int sum = nums[left] + nums[right];
            if (sum == target) {
                System.out.println(nums[left] + " " + nums[right]);
                break;
            } else if (sum < target) {
                left++;
            } else if (sum > target) {
                right--;
            }
        }
    }
}
