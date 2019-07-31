CyclicBarrier

一种允许多个线程全部等待彼此都到达某个屏障的同步机制

##### 使用
多个线程并发执行同一个CyclicBarrier实例的await方法时，每个线程执行这个方法后，都会被暂停，只有当最后一个线程执行完await方法时，它自身不会暂停，且会唤醒所有等待线程。因为CyclicBarrier内部维护了一个显式锁，它可以识别最后一个执行线程。

CyclicBarrier内部维护一个trip变量来实现等待/通知，所有除最后一个线程的保护条件都是“当前generation中，仍未执行的线程数大于0”，这个仍未执行的线程数默认为总参与线程的数量，await方法每执行一次，这个数量递减1.

##### 使用场景
模拟高并发，或放在一个循环中，使得当前迭代操作的结果作为下一个迭代的基础输入；不然可以直接使用Thread.join()或CoutDwonLatch

##### 问题：

1.可以设置多个屏障吗：可以，以generation区分

##### 示例：

```java
class MyThread extends Thread {
    private CyclicBarrier cb;
    public MyThread(String name, CyclicBarrier cb) {
        super(name);
        this.cb = cb;
    }
    
    public void run() {
        System.out.println(Thread.currentThread().getName() + " going to await");
        try {
            cb.await();
            System.out.println(Thread.currentThread().getName() + " continue");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
public class CyclicBarrierDemo {
    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        CyclicBarrier cb = new CyclicBarrier(3, new Thread("barrierAction") {
            public void run() {
                System.out.println(Thread.currentThread().getName() + " barrier action");
                
            }
        });
        MyThread t1 = new MyThread("t1", cb);
        MyThread t2 = new MyThread("t2", cb);
        t1.start();
        t2.start();
        System.out.println(Thread.currentThread().getName() + " going to await");
        cb.await();
        System.out.println(Thread.currentThread().getName() + " continue");

    }
}
```



```java
public class CyclicBarrier {
    //屏障的每一次使用都代表一代示例
    private static class Generation {
        //屏障是否被破坏
        boolean broken = false;
    }
    //防止线程跳过屏障的锁 默认非公平锁
    private final ReentrantLock lock = new ReentrantLock();
    //等待线程跳过屏障的条件
    private final Condition trip = lock.newCondition();
    //参与线程数量
    private final int parties;
    //跳过屏障后执行的指令
    private final Runnable barrierCommand;
    //当前阶段？？？？
    private Generation generation = new Generation();
    //每一阶段仍在等待的线程
    private int count;
    
    //barrierAction：跳过屏障后执行的指令
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }
    //线程到达屏障 则等待：外部等待调用方法
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }
    /**
     * 执行过程：
     *  加锁 -> 判断屏障是否被破坏(抛异常) -> 判断线程是否被中断(毁坏屏障，抛异常) -> 
     *  判断是否仍有等待线程(N -> 执行屏障命令； Y -> 进入屏障等待) -> 解锁
     */
    private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        final ReentrantLock lock = this.lock;
        //锁住当前线程
        lock.lock();
        try {
            final Generation g = generation;
            if (g.broken)
                throw new BrokenBarrierException();
			//线程被中断 唤醒所有等待线程 损坏当前屏障 抛出异常
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }
			//获得并减少正在等待进入屏障的线程个数
            int index = --count;
            if (index == 0) {  // 全部进入屏障了 执行后续命令
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;
                    //唤醒所有等待线程 进入下一代
                    nextGeneration();
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }
            // 自旋直到跳过屏障/中断/超时 停止
            for (;;) {
                try {
                    //没有设置时间 直接等待
                    if (!timed)
                        trip.await();
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }
                if (g.broken)
                    throw new BrokenBarrierException();
                //不是当前代的 返回索引
                if (g != generation)
                    return index;
				//设置了等待时间且时间小于等于0
                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }
    //条件：所有线程处于等待状态 
    private void nextGeneration() {
        // 唤醒所有线程
        trip.signalAll();
        // 恢复等待线程数
        count = parties;
        generation = new Generation();
    }
}    
```























