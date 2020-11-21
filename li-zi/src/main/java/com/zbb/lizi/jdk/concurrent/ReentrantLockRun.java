package com.zbb.lizi.jdk.concurrent;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: tiancha
 * @Date: 2020/9/27 10:42 下午
 */
public class ReentrantLockRun {
    /**
     * 目标：理解等待线程挂起和资源抢占涉及到安全问题
     *
     */

    private static int one_second = 1000;
    public static void main(String[] args) {
        ReentrantLock reentrantLock = new ReentrantLock(false);

        // 模拟两种情况
        // 1。 线程1一直持续占有 线程2自旋 自旋过程做了什么:自旋作为尾节点 有候选资格，加入成功后，自旋尝试并阻塞线程等待被前置线程释放资源时唤醒 核心就是头节点
        // 2。 线程1短时间占有，线程2尝试获取失败 线程1释放资源后 做了什么 60000
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                reentrantLock.lock();
                try {
                    Thread.sleep(one_second * 60 * 60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    reentrantLock.unlock();
                }
            }
        }, "first");
        thread1.start();
        try {
            Thread.sleep(one_second);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                reentrantLock.lock();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    reentrantLock.unlock();
                }
            }
        }, "after");
        thread2.start();

    }
}
