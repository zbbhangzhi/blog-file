package com.zbb.lizi.algorithm.list;


/**
 * 归并有序列表
 * @author tiancha
 * @since 2020/3/31 20:26
 */
public class MergeLinkedList {
    public static void main(String[] args) {
        int[] n1 = new int[]{5, 8, 6, 3, 9, 2, 1, 7};
        int[] n2 = new int[]{4, 1, 3, 1, 3, 2, 1, 1};
        LinkedList linkedList = new LinkedList();
        int[] n3 = n1;
        for(int i = 0;i<n1.length + n2.length;i++){
            if (i == n1.length){
                i = 0;
                n3 = n2;
            }
//            merge(n3[i], 0, n1.length + n2.length);
        }
    }

    private static void merge(int n){

    }
}
