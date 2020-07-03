package com.zbb.lizi.algorithm.window;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tiancha
 * @since 2020/7/1 19:37
 */
public class FindAnagrams {
    public static void main(String[] args) {
        String s = "cbaebabacd";
        String t = "abc";
        Map<Character, Integer> windowMap = new HashMap<Character, Integer>();
        Map<Character, Integer> needMap = new HashMap<Character, Integer>() {
        };
        char[] cc = t.toCharArray();
        char[] dd = s.toCharArray();
        for (char c : cc) {
            needMap.put(c, 1);
        }

        // 左游标
        int left = 0;
        // 右游标
        int right = 0;
        // need中有效个数
        int valid = 0;
        // 最小覆盖子串的起始索引和长度
        int start = 0;
        int len = Integer.MAX_VALUE;

        // 右侧进入 直到right和字符串相等停止
        while (right < dd.length) {
            char c = dd[right];
            right++;
            if (needMap.containsKey(c)) {
                // 如果子串需要这个字符，加入到needMap中并判断是否达到子串要求
                windowMap.put(c, windowMap.get(c) == null ? 1 : windowMap.get(c) + 1);
                if (windowMap.get(c) == needMap.get(c)) {
                    valid++;
                }
            }

            // 已达到子串条件 左侧窗口开始收缩 更新最小覆盖
            // 这里就是每cc个每cc个的检查是否符合
            while (right - left >= cc.length) {
                // 开始左侧收缩 这个len
                if (valid == needMap.size()){
                    System.out.println(left);
                }
                char d = dd[left];
                left++;
                // 如果子串包含这个字符，判断是否满足子串条件
                if (needMap.containsKey(d)){
                    if (windowMap.get(d) == needMap.get(d)){
                        valid--;
                    }
                    windowMap.put(d, windowMap.get(d) - 1);
                }
            }
        }

        // 返回最小覆盖子串
        System.out.println(len == Integer.MAX_VALUE ? "" : s.substring(start, start + len));
    }
}
