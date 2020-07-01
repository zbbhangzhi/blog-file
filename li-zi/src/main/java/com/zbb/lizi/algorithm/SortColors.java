package com.zbb.lizi.algorithm;

/**
 * 给定一个包含红色、白色和蓝色，一共 n 个元素的数组，原地对它们进行排序，使得相同颜色的元素相邻，并按照红色、白色、蓝色顺序排列。
 * @author tiancha
 * @since 2020/3/19 19:36
 */
public class SortColors {
    public static void main(String[] args) {
        int[] nums = {2, 0, 1, 2, 0, 1};
        sortColors(nums);
    }

    private static void sortColors(int[] nums) {
        int left = -1;
        int right = nums.length;
        for (int i = 0; i < right; i++) {
            if (nums[i] == 1){
                i++;
            }else if(nums[i] == 0){
                left++;
                swap(nums[i], nums[left]);
                i++;
            }else {
                right--;
                swap(nums[i], nums[right]);
            }
        }
    }

    private static void swap(int a, int b){

    }
}
