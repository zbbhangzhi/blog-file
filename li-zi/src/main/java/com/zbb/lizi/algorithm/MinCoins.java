package com.zbb.lizi.algorithm;

/**
 * 最少硬币凑零钱
 * @author tiancha
 * @since 2020/6/29 19:31
 */
public class MinCoins {
    public static void main(String[] args) {
        int[] coins = {};
        int price = 11;
        int nums = fib(coins, price);
    }

    private static int fib(int[] coins, int price) {
        int nums = 0;
        int sum = 0;
        // 逐步积累到price
        for (int i = 1; i <= price; i++){
            nums += min();
        }
    }

    private static int min(){

    }
}
