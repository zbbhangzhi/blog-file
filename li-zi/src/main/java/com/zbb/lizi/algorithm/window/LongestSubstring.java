package com.zbb.lizi.algorithm.window;

import java.util.HashMap;
import java.util.Map;

/**
 * 最长的无重复子串
 *
 * @author tiancha
 * @since 2020/7/1 20:08
 */
public class LongestSubstring {
    public static void main(String[] args) {
        String s = "abcabcbb";
        Map<Character, Integer> windowMap = new HashMap<Character, Integer>();
        char[] dd = s.toCharArray();

        // 左游标
        int left = 0;
        // 右游标
        int right = 0;
        // need中有效个数
        int res = 0;

        // 右侧进入 直到right和字符串相等停止
        while (right < dd.length) {
            char c = dd[right];
            right++;
            windowMap.put(c, windowMap.get(c) == null ? 1 : windowMap.get(c) + 1);
            // 如果重复map里已存在这个就该移动left缩小窗口
            while (windowMap.get(c) > 1) {
                char d = dd[left];
                left++;
                windowMap.put(d, windowMap.get(d) == null ? 1 : windowMap.get(d) - 1);
            }
            // 比较妙的是max这里 保证之前的还能被记住
            res = Integer.max(res, right - left);
        }
        System.out.println(res);
    }
}
