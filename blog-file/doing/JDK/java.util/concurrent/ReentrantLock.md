ReentrantLock



### 问题：

1.非公平与公平的区别：在获取资源的过程中是否有等待队列的判断

2.可重入锁在基于synchronized的基础上有什么改进：目前认为是是否可持多个资源 

3.独占模式解释：线程独占资源

4.getState是指资源被当前线程占用数量还是所有线程：目前看来是所有的被占资源数





三个内部类：普通锁Sync，公平锁FairSync，非公平锁NonfairSync

##### 1.Sync是其他两个的父类，是ReentrantLock的基本同步控制。继承AQS，使用其状态表示持锁数量。

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
    //unfaire tryLock
    final boolean nonfairTryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        //表示没有线程在竞争该锁
        if (c == 0) {
            //确认状态为0即没有锁并更新状态
            if (compareAndSetState(0, acquires)) {
                //设置当前线程拥有独家访问权限（独占）
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        //当前线程处于独占锁状态且已持有资源 则增加重入次数
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }
    //release 
    protected final boolean tryRelease(int releases) {
        int c = getState() - releases;
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();
        boolean free = false;
        //当前独占线程持有锁和将释放锁相等 释放并放弃独占状态；否则只释放
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }
    //返回锁资源的占用线程
    final Thread getOwner() {
        return getState() == 0 ? null : getExclusiveOwnerThread();
    }
    //反序列化
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        setState(0); // reset to unlocked state
    }
```

##### 2.NonfairSync：采用非公平策略获取锁

```Java
static final class NonfairSync extends Sync {
    //获取锁
    final void lock() {
        //锁未被占用：更新状态且设置当前线程为独占
        if (compareAndSetState(0, 1))
            setExclusiveOwnerThread(Thread.currentThread());
        else
            //锁被占用：以独占模式获取且忽略中断
            acquire(1);//来自AQS::acquire
    }
    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);//来自Sync::nonfairTryAcquire
    }
}
```

##### 3.FairSync：采用公平策略获取锁

```java
static final class FairSync extends Sync {
    final void lock() {
        acquire(1);//来自AQS::acquire 由线程等待获取资源队列体现公平
    }
    
    protected final boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();
        if (c == 0) {
            //hasQueuedPredecessors 前方是否有排队的线程（公平体现）
            if (!hasQueuedPredecessors() &&
                compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        //如果当前资源已被占用且线程也不是独占状态 则获取失败
        return false;
    }
```

解析源码发现大多数实现都是基于AQS

```java
public class ReentrantLock implements Lock, java.io.Serializable {   
	private final Sync sync;

    //构造函数：默认非公平策略
    public ReentrantLock() {
        sync = new NonfairSync();
    }
    //或自选
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }
	// sync.lock为抽象方法则调用其实现类FairSync/NonfairSync::lock
	public void lock() {
        sync.lock();
    }
	
	public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

	public void unlock() {
        sync.release(1);//AQS::release
    }
}
```

