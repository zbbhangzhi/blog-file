package com.zbb.lizi.algorithm;

import java.util.HashMap;
import java.util.Map;

/**
 * Fibonacci=(n*(n+1))/2
 * 1+2+3+4+5+..+n
 *
 * @author tiancha
 * @since 2020/6/28 17:26
 */
public class Fibonacci {
    public static void main(String[] args) {
        System.out.println(fib(10));
        Map<Integer, Integer> map = new HashMap<Integer, Integer>(8);
        System.out.println(fibByMap(10, map));
        Map<Integer, Integer> map2 = new HashMap<Integer, Integer>(8);
        System.out.println(fibByUp(10, map2));
    }

    /**
     * 时间复杂度=子问题的时间 * 子问题个数=O(2^n) todo 我还没理解为什么是指数
     */
    private static int fib(int n) {
        if (n == 1 || n == 2) return 1;
        // 如果一直未到递归出口 那么会一直在方法栈上开辟空间 递归越深可能会导致栈溢出
        return fib(n - 1) + fib(n - 2);
    }

    /**
     * 优化1：减少递归冗余
     * 时间复杂度=子问题的时间 * 子问题个数=O(n)
     */
    private static Integer fibByMap(Integer n, Map<Integer, Integer> map) {
        if (n == 1 || n == 2) return 1;
        // 保存已经计算过的子问题，不至于每次都需要去递归计算
        if (map.get(n) != null) return map.get(n);
        map.put(n, fibByMap(n - 1, map) + fibByMap(n - 2, map));
        return fib(n - 1) + fib(n - 2);
    }

    /**
     * 优化2：减少递归冗余 + 递归改为单向循环迭代
     * 时间复杂度=子问题的时间 * 子问题个数=O(n)
     */
    private static Integer fibByUp(Integer n, Map<Integer, Integer> map) {
        if (n == 1 || n == 2) return 1;
        map.put(1, 1);
        map.put(2, 1);
        for (int i = 3; i <= n; i++) {
            map.put(i, map.get(i - 1) + map.get(i - 2));
        }

        return map.get(n);
    }

    /**
     * 优化3：只存储最后的值 + 递归改为单向循环迭代
     * 时间复杂度=子问题的时间 * 子问题个数=O(n)
     * 空间复杂度=O(1)
     */
    private static Integer fibByUp2(Integer n) {
        if (n == 1 || n == 2) return 1;
        int prev = 1;
        int cur = 1;
        for (int i = 3; i <= n; i++) {
            int sum = prev + cur;
            prev = cur;
            cur = sum;
        }
        return cur;
    }
}
