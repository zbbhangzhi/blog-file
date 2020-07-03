package com.zbb.lizi.algorithm.window;

import java.util.HashMap;
import java.util.Map;

/**
 * 先不断向右边扩大窗口，直到子串条件被满足
 * 停止扩大右边，开始左侧收缩，缩小窗口，直到窗口不满足子串条件
 * 重复右扩左缩，直到right达到字符串的尽头
 *
 * 这个算法的精髓就在于：它先从头遍历到结尾，然后再收缩到最短的，全靠维护的两个map
 * @author tiancha
 * @since 2020/7/1 16:02
 */
public class MiniWindowSubstring {

    public static void main(String[] args) {
        String s = "CEBBANCF";
        String t = "ABC";
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
            while (valid == needMap.size()) {
                // 开始左侧收缩 这个len todo 怎么确定的
                if (right - left < len){
                    start = left;
                    len = right - left;
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
