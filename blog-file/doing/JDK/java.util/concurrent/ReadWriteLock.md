ReadWriteLock



问题：

1.读写锁在实现过程中的区别：即独占共享模式的区别

2.ThreadLocal在共享模式下的使用意义：因为读锁可以多个线程同时拥有 所以需要ThreadLocal

3.获取读写锁占有数量 exclusiveCount/sharedCount 是指当前还是总的 ：这得看AQS了

4.体现：读写互斥，读读不影响，写写互斥

5.锁降级：从写锁变成读锁；获得写锁后去获取读锁，支持

6.锁升级：从读锁变成写锁。获取读锁后去获取写锁，不支持，会死锁。



TODO：条件阻塞Condition



```java
public interface ReadWriteLock {
    // Returns the lock used for reading. 共享且写锁没有被持有
    // 没有其他线程的写锁；没有写请求，或者有写请求但调用线程和持有锁的线程是同一个线程
    Lock readLock();
	
    // Returns the lock used for writing. 独占 当有读锁的时候 写锁不可被持有
    // 没有其他线程的读锁；没有其他线程的写锁
    Lock writeLock();
}
```



其实现类ReentrantReadWriteLock

### 内部有五个内部类：

- Sync基本同步控制器

```java
//基于AQS 与其实现类NonfairSync/FairSync相比读写锁获取时是否阻止
    abstract static class Sync extends AbstractQueuedSynchronizer {
        //返回count中表示的共享持有数量
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }
        //返回count中表示的独占持有数量
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }
        static final class HoldCounter {
            int count = 0;
            // Use id, not reference, to avoid garbage retention
            final long tid = getThreadId(Thread.currentThread());
        }
        //当前线程持有的可重入读锁数量
        static final class ThreadLocalHoldCounter
            extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }
        //当前线程持有的可重入读锁数量
        private transient ThreadLocalHoldCounter readHolds;
        //成功获取读锁的最后一个线程的计数
        private transient HoldCounter cachedHoldCounter;
        //第一个获取读锁的线程
        private transient Thread firstReader = null;
        
        //译：除了写没有阻止，其作用和tryAcquire一样
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                //todo 为什么独占数量为0 就不行
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }
        
        //获取读锁：写锁不可被持有 可同时获取多个读锁
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            //自旋 直到获得
            for (;;) {
                int c = getState();
                //独占数量不为0（说明写锁被持有）且当前线程不为独占线程
                if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    //共享数量为0且当前线程为第一个获取读锁
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        //更新cachedHoldCounter
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }
        //获取写锁占有数量：一定是独占状态下
        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }
        //todo 除了第一步还要获取第一个/最后一个/总的的数量
        final int getReadHoldCount() {
            //共享数量为0则没有占有数量
            if (sharedCount(getState()) == 0)
                return 0;
            Thread current = Thread.currentThread();
            //第一个获取线程
            if (firstReader == current)
                return firstReaderHoldCount;
            HoldCounter rh = cachedHoldCounter;
            //最后一个获取线程
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;
            //当前线程占有数量
            int count = readHolds.get().count;
            //清除当前线程在ThreadLocal中的值防止ThreadLocal溢出
            if (count == 0) readHolds.remove();
            return count;
        }
        //只判断独占模式
        final Thread getOwner() {
            return ((exclusiveCount(getState()) == 0) ?null :
                    getExclusiveOwnerThread());
        }
    }     
```

- FairSync公平sync

```java
 static final class FairSync extends Sync {
     private static final long serialVersionUID = -2274990926593161451L;
     final boolean writerShouldBlock() {
     return hasQueuedPredecessors();
     }
     final boolean readerShouldBlock() {
     return hasQueuedPredecessors();
     }
}
```

- NonfairSync非公平sync

```java
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = -8159625535654395037L;
    final boolean writerShouldBlock() {
        return false; // writers can always barge
    }
    final boolean readerShouldBlock() {
        return apparentlyFirstQueuedIsExclusive();
    }
}
```

- ReadLock 读锁

```java
public static class ReadLock implements Lock, java.io.Serializable {
    private final Sync sync;
    public void lock() {
        sync.acquireShared(1);//AQS::acquireShared 共享模式获取 无视中断
    }
    public boolean tryLock() {
        return sync.tryReadLock();
    }
    public void unlock() {
        sync.releaseShared(1);
    }
}
```

- WriteLock 写锁

```java
public static class WriteLock implements Lock, java.io.Serializable {
    private final Sync sync;
    //独占模式
    public void lock() {
        sync.acquire(1);
    }
    public boolean tryLock( ) {
        return sync.tryWriteLock();
    }
    public void unlock() {
        sync.release(1);
    }
```





```java
public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {
    //读锁
    private final ReentrantReadWriteLock.ReadLock readerLock;
    //写锁
    private final ReentrantReadWriteLock.WriteLock writerLock;
    //同步控制器：采用公平/非公平策略
    final Sync sync;
    
}
```











