package com.zbb.lizi.algorithm.window;

import java.util.HashMap;
import java.util.Map;

/**
 * 先不断向右边扩大窗口，直到排列子串的大小和移动游标间隔一样
 * 停止扩大右边，开始左侧收缩，缩小窗口，找出满足条件的
 * 重复右扩左缩，直到right达到字符串的尽头
 * <p>
 * 这个算法的精髓就在于：它先从头遍历到结尾，然后再收缩到最短的，全靠维护的两个map
 *
 * @author tiancha
 * @since 2020/7/1 16:02
 */
public class CheckInclusion {

    public static void main(String[] args) {
        String s = "eidbaooo";
        String t = "abo";
        Map<Character, Integer> windowMap = new HashMap<Character, Integer>();
        Map<Character, Integer> needMap = new HashMap<Character, Integer>() {
        };
        char[] cc = t.toCharArray();
        char[] dd = s.toCharArray();
        for (char c : cc) {
            needMap.put(c, needMap.get(c) == null ? 1 : needMap.get(c) + 1);
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

            // 判断左侧窗口是否需要收缩：移动left的时机要和排列子串的长度一样
            while (right - left >= cc.length) {
                // 开始左侧收缩 这个len
                if (valid == needMap.size()){
                    break;
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
        System.out.println(valid == needMap.size());
    }

    // 没用滑动窗口的方法 我的会更优 不需要来来回回
//    while (right < dd.length) {
//        char c = dd[right];
//        right++;
//        if (needMap.containsKey(c)) {
//            // 如果子串需要这个字符，加入到needMap中并判断是否达到子串要求
//            windowMap.put(c, windowMap.get(c) == null ? 1 : windowMap.get(c) + 1);
//            if (windowMap.get(c) == needMap.get(c)) {
//                valid++;
//            }
//        }else if (valid == needMap.size()){
//            break;
//        } else if (valid > 0){
//            valid--;
//        }
//    }
}
