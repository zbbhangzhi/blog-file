package com.zbb.lizi.algorithm.list;

import java.util.Random;

/**
 * 输入N个整数，按照输入的顺序建立单链表存储，并遍历所建立的单链表，输出这些数据。
 *
 * @author tiancha
 * @since 2020/3/30 19:30
 */
public class LinkedList<E> {
    Node<E> next;

    Node<E> first;

    Node<E> last;

    int size;

    public static class Node<E extends Object> {
        E item;

        Node<E> next;

        Node<E> prv;

        public Node(E item, Node<E> next, Node<E> prv) {
            this.item = item;
            this.next = next;
            this.prv = prv;
        }
    }

    public void add(E item) {
        // 新插入的元素按顺序放入队尾
        linkedLast(item);
    }

    public void tail(E item){
        // 新插入的元素按顺序放入队首
        linkedFirst(item);
    }

    private void linkedFirst(E item) {
        Node<E> f = first;
        Node<E> node = new Node<E>(item, f, null);
        first = node;
        if (f != null){
            f.prv = node;
        }
        size++;
    }

    private void linkedLast(E item) {
        // 直接拿到队尾 再插入
        Node<E> l = last;
        Node<E> node = new Node<E>(item, null, l);
        // 一定要初始化first和last
        last = node;
        // 如果last为空说明整条链路都是空的
        if (l != null) {
            l.next = node;
        } else {
            first = node;
        }
        size++;
    }

    public static void main(String[] args) {
        LinkedList<Integer> linkedList = new LinkedList<Integer>();
        for (int i = 0; i < 10; i++) {
            int a = new Random().nextInt(10);
            if (a == -1)break;
            System.out.println("a = " + a);
            linkedList.tail(a);
        }
         for (Node<Integer> node = linkedList.first; node != null; node = node.next){
            System.out.println("node = " + node.item);
         }
    }
}
