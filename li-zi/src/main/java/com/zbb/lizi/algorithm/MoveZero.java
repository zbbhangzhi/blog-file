package com.zbb.lizi.algorithm;

/**
 * @author tiancha
 * @since 2020/3/16 20:20
 */
public class MoveZero {
    public static void main(String[] args) {
        int[] nums = {1, 2, 6, 0, 8, 3};

        moveZeroToTail1(nums);
    }

    private static void moveZeroToTail1(int[] nums) {
        int[] newNums = new int[nums.length];
        int j = 0;
        int k = 0;
        for (int i = 0; i < nums.length;i++){
            if (i != 0){
                newNums[j] = nums[i];
                k++;
            }
            j++;
        }

    }

    private static void moveZeroToTail2(int[] nums) {
        nums[nums.length] = 0;
        for (int i = 0; i < nums.length;i++){
            if (nums[i] != 0){

            }

        }
    }

    private static void moveZeroToTail3(int[] nums) {

    }
}
