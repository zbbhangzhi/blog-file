ExecutorService

![1550714694740](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1550714694740.png)

##### 问题

1. 使用线程池怎么体现减少开销：重复利用线程

2. 怎么体现的重复利用线程：runWorker方法中，处理的任务可能时工作线程的任务 也可能时任务队列中的任务所以工作线程可以多处理任务

##### todo

```
RunnableFuture
FutureTask
poll：移除并返回头部元素，若队列为空，则返回null
take：移除并返回头部元素，若队列为空，则阻塞
COndition用处
```

##### 示例

```java
ExecutorService executorService = new ThreadPoolExecutor(SIZE_CORE_POOL, SIZE_MAX_POOL, TIME_KEEP_ALIVE, TimeUnit.SECONDS, new ArrayBlockingQueue<>(SIZE_WORK_QUEUE),
                new ThreadFactory(POOL_NAME), new DiscardPolicy());
executorService.submit(runnable);
executorService.shutdown();
```

##### Executor：executor(Runnable) 

##### ExecutorService：提供方法管理/终止多个异步任务的执行

```java
public interface ExecutorService extends Executor {
    void shutdown();
    List<Runnable> shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    //执行任务
    <T> Future<T> submit(Runnable task, T result);
    Future<?> submit(Runnable task);
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;
    //执行给定的任务集合，返回任意成功的结果；遇到报错等取消执行；执行中集合被修改，结果不确定；
    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;
}
```

##### AbstractExecutorService：ExecutorService的默认实现类

```java
public abstract class AbstractExecutorService implements ExecutorService {
    
    public Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException();
        //重新包装任务
        RunnableFuture<Void> ftask = newTaskFor(task, null);
        //异步执行任务：为任务创建新线程/加入到任务队列，最终调用的是实现了execute()的子类的方法
        execute(ftask);
        return ftask;
    }
    //value：未来返回的默认值
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value);
    }
    
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return doInvokeAny(tasks, true, unit.toNanos(timeout));
    }
    
    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks,
                              boolean timed, long nanos)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null)
            throw new NullPointerException();
        int ntasks = tasks.size();
        if (ntasks == 0)
            throw new IllegalArgumentException();
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
        //提供同时执行任务的类：内部维护一个已完成任务的队列 且take/poll后会移除首个 保证队列都是最新的
        ExecutorCompletionService<T> ecs =
            new ExecutorCompletionService<T>(this);

        // For efficiency, especially in executors with limited
        // parallelism, check to see if previously submitted tasks are
        // done before submitting more of them. This interleaving
        // plus the exception mechanics account for messiness of main
        // loop.

        try {
            // Record exceptions so that if we fail to obtain any
            // result, we can throw the last exception we got.
            ExecutionException ee = null;
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Iterator<? extends Callable<T>> it = tasks.iterator();

            // 一定要先执行一个任务
            futures.add(ecs.submit(it.next()));
            --ntasks;
            int active = 1;
			//自旋 直到所有执行完成或者执行好任意一个 返回结果
            for (;;) {
                //移除并返回下一个已完成的任务future 如果队列为空 直接返回null
                Future<T> f = ecs.poll();
                //如果第一个执行的为空 
                if (f == null) {
                    //没有其他异常情况下 f一直保持为null 一直执行下一个 保证任务全部执行
                    if (ntasks > 0) {
                        --ntasks;
                        futures.add(ecs.submit(it.next()));
                        ++active;
                    }
                    else if (active == 0)
                        break;
                    //可能时间没到 就等等再拿
                    else if (timed) {
                        f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                        if (f == null)
                            throw new TimeoutException();
                        nanos = deadline - System.nanoTime();
                    }
                    //移除并返回下一个已完成的任务future 如果队列内没有 就阻塞等待
                    else
                        f = ecs.take();
                }
                //任务集合中有任意一个成功了 结束循环 返回结果
                if (f != null) {
                    --active;
                    try {
                        return f.get();
                    } catch (ExecutionException eex) {
                        ee = eex;
                    } catch (RuntimeException rex) {
                        ee = new ExecutionException(rex);
                    }
                }
            }

            if (ee == null)
                ee = new ExecutionException();
            throw ee;

        } finally {
            //保证如果出错 未完成的任务全部取消
            for (int i = 0, size = futures.size(); i < size; i++)
                futures.get(i).cancel(true);
        }
    }
    
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout, TimeUnit unit)
        throws InterruptedException {
        if (tasks == null)
            throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks)
                futures.add(newTaskFor(t));

            final long deadline = System.nanoTime() + nanos;
            final int size = futures.size();

            // Interleave time checks and calls to execute in case
            // executor doesn't have any/much parallelism.
            for (int i = 0; i < size; i++) {
                execute((Runnable)futures.get(i));
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L)
                    return futures;
            }
            for (int i = 0; i < size; i++) {
                Future<T> f = futures.get(i);
                if (!f.isDone()) {
                    if (nanos <= 0L)
                        return futures;
                    try {
                        f.get(nanos, TimeUnit.NANOSECONDS);
                    } catch (CancellationException ignore) {
                    } catch (ExecutionException ignore) {
                    } catch (TimeoutException toe) {
                        return futures;
                    }
                    nanos = deadline - System.nanoTime();
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done)
                for (int i = 0, size = futures.size(); i < size; i++)
                    futures.get(i).cancel(true);
        }
    }
}
```



##### ThreadPoolExecutor：继承AbstractExecutorService，使用线程池执行每个提交的任务

###### 属性

AtomicInteger ctl：控制状态，表示两个概念，为保持这两个概念在一个int里，将workerCount限制2^29-1，剩下3位就是线程池状态

1. runState：指的是线程池状态

​	RUNNING：     -1接受新任务同时处理队列任务

​	SHUTDOWN： 0不接受新任务但处理队列任务

​	STOP：	     1不接受不处理，停止所有在执行任务

​	TIDYING：        2所有任务都结束了，都即将执行TERMINATED的钩子方法

​	TERMINATED：3 全部结束

2. workerCount：当前工作线程数

```java
public class ThreadPoolExecutor extends AbstractExecutorService {
    //表示两种概念：工作线程数workerCount，运行状态runState
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    //workcount所占位数
    private static final int COUNT_BITS = Integer.SIZE - 3;//32-3=29
    //容量：workerCount最大值
    private static final int CAPACITY = (1 << COUNT_BITS)-1;//1向左无符号移29位->低28位全是1

    // runState is stored in the high-order bits
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;

    // Packing and unpacking ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    //当前运行线程总数
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    private static int ctlOf(int rs, int wc) { return rs | wc; }
    
    //任务队列：存储任务并将任务交接给线程
    private final BlockingQueue<Runnable> workQueue;
    private final ReentrantLock mainLock = new ReentrantLock();
    //工作线程集合
    private final HashSet<Worker> workers = new HashSet<Worker>();
    //等待条件？？？？
    private final Condition termination = mainLock.newCondition();
    //最大池大小 只能mainLock下访问
    private int largestPoolSize;
    //线程完成数 工作线程终止时更新 只能mainLock下访问
    private long completedTaskCount;
    //线程池 创建线程 todo 怎么保证不会创建失败 内存溢出
    private volatile ThreadFactory threadFactory;
    //饱和或停止则拒绝的处理
    private volatile RejectedExecutionHandler handler;
    //空闲线程等待工作的时间 纳秒
    private volatile long keepAliveTime;
    //false：核心线程即使处于空闲也是保持活跃；true：核心线程使用keepAliveTime超时等待工作
    private volatile boolean allowCoreThreadTimeOut;
    //保持活动的最小工作线程数 除非设置allowCoreThreadTimeOut
    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    //默认的拒绝执行器
    private static final RejectedExecutionHandler defaultHandler =
        new AbortPolicy();
    private static final RuntimePermission shutdownPerm =
        new RuntimePermission("modifyThread");
    //执行终结finalizer/null时才会使用的上下文
    private final AccessControlContext acc;
    ......
}    
```

###### 内部类：Worker自身即线程

```java
private final class Worker extends AbstractQueuedSynchronizer implements Runnable{
    final Thread thread;
    Runnable firstTask;
    volatile long completedTasks;
    Worker(Runnable firstTask) {
        setState(-1); // inhibit interrupts until runWorker
        this.firstTask = firstTask;
        this.thread = getThreadFactory().newThread(this);
    }
    public void run() {runWorker(this);	}//ThreadPoolExecutor提供
    protected boolean tryAcquire(int unused) {
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }
    protected boolean tryRelease(int unused) {
        setExclusiveOwnerThread(null);
        setState(0);
        return true;
    }
    //AQS的acquire方法中调用了抽象方法tryAcquire 即自动调用Worker的tryAcquire方法
    public void lock()        { acquire(1); }//AQS::acquire -> Worker::tryAcquire
    public boolean tryLock()  { return tryAcquire(1); }
    public void unlock()      { release(1); }
    public boolean isLocked() { return isHeldExclusively(); }
    //终止线程
	void interruptIfStarted() {
        Thread t;
        if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
            try {
                t.interrupt();
            } catch (SecurityException ignore) {
            }
        }
    }
}    
```

###### 构造方法

```java
public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
         Executors.defaultThreadFactory(), handler);
}
public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
    if (corePoolSize < 0 ||
        maximumPoolSize <= 0 ||
        maximumPoolSize < corePoolSize ||
        keepAliveTime < 0)
        throw new IllegalArgumentException();
    if (workQueue == null || threadFactory == null || handler == null)
        throw new NullPointerException();
    this.acc = System.getSecurityManager() == null ?
        null :
    AccessController.getContext();
    this.corePoolSize = corePoolSize;
    this.maximumPoolSize = maximumPoolSize;
    this.workQueue = workQueue;
    this.keepAliveTime = unit.toNanos(keepAliveTime);
    this.threadFactory = threadFactory;
    this.handler = handler;
    }
```

###### execute执行工作线程任务

```java
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
    //拿出ctl存储的值 用于拆分
    int c = ctl.get();
    //1. 工作线程数 小于 核心线程数 为任务创建一个工作线程，
    //   创建时检查runstate&workcount，防止不应该添加时添加出现错误
    if (workerCountOf(c) < corePoolSize) {
        if (addWorker(command, true))
            return;
        c = ctl.get();
    }
    //2. 为任务创建线程失败：判断线程池正在运行 且 能将任务成功放入队列
    //   双重检测：检测是否需要为任务创建新线程 
    //   因为上次检测时 创建线程失败 检测线程池是否已经关闭 同时可以从任务队列移除任务：Y 拒绝任务 
    //   N 判断工作线程等于0 TODO
    if (isRunning(c) && workQueue.offer(command)) {
        int recheck = ctl.get();
        if (! isRunning(recheck) && remove(command))
            reject(command);
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    //3. 线程池停止 或 任务队列不能接受任务 强制为任务创建线程 如果失败 执行拒绝策略
    else if (!addWorker(command, false))
        reject(command);
}
//为任务创建工作线程
//core：true 以corePoolSize为界限 false 以maximumPoolSize
private boolean addWorker(Runnable firstTask, boolean core) {
    //TODO 为什么要自旋
    retry:
    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN &&       //true
            ! (rs == SHUTDOWN &&	//false
               firstTask == null && //false
               ! workQueue.isEmpty()))//false
            return false;

        for (;;) {
            int wc = workerCountOf(c);
            if (wc >= CAPACITY ||
                wc >= (core ? corePoolSize : maximumPoolSize))
                return false;
            //更新线程数量 结束retry
            if (compareAndIncrementWorkerCount(c))
                break retry;
            c = ctl.get();  // Re-read ctl
            //线程池状态改变 跳回retry
            if (runStateOf(c) != rs)
                continue retry;
            // else CAS failed due to workerCount change; retry inner loop
        }
    }
	//前置条件都满足 开始创建工作线程
    boolean workerStarted = false;
    boolean workerAdded = false;
    Worker w = null;
    try {
        w = new Worker(firstTask);
        final Thread t = w.thread;
        if (t != null) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // Recheck while holding lock.
                // Back out on ThreadFactory failure or if
                // shut down before lock acquired.
                int rs = runStateOf(ctl.get());
				//检测线程池状态
                if (rs < SHUTDOWN ||
                    (rs == SHUTDOWN && firstTask == null)) {
                    //检查线程是否已经开始
                    if (t.isAlive()) 
                        throw new IllegalThreadStateException();
                    //添加工作线程到集合中
                    workers.add(w);
                    int s = workers.size();
                    //更新线程池线程数量
                    if (s > largestPoolSize)
                        largestPoolSize = s;
                    workerAdded = true;
                }
            } finally {
                mainLock.unlock();
            }
            //检查是否添加成功 成功：启动线程任务 t内部run方法见下方解析
            if (workerAdded) {
                t.start();
                workerStarted = true;
            }
        }
    } finally {
        if (! workerStarted)
            addWorkerFailed(w);
    }
    return workerStarted;
}
```

```java
//解析工作线程执行过程：
//Worker的构造方法 
Worker(Runnable firstTask) {
    setState(-1); // inhibit interrupts until runWorker
    this.firstTask = firstTask;
    //1. 创建线程时将当前worker放入初始化 worker实现了Runnable的run方法
    this.thread = getThreadFactory().newThread(this);
}
//Thread的run方法：2. 线程执行任务时 其实执行的是worker的run方法
public class Thread{
    private Runnable target;
    public void run() {
        if (target != null) {
            target.run();
        }
    }
}
// 3.Worker的run方法
public void run() {
    runWorker(this);//ThreadPoolExecutor::runWorker
}
```

###### 工作线程执行任务runWorker

```java
final void runWorker(Worker w) {
    Thread wt = Thread.currentThread();
    Runnable task = w.firstTask;
    w.firstTask = null;
    w.unlock(); // allow interrupts
    boolean completedAbruptly = true;
    try {
        //1.直接执行工作线程的任务 2.从任务队列中取 注意！ 这是个一个while循环 task可能是1 也可能是2
        while (task != null || (task = getTask()) != null) {
            w.lock();
            // If pool is stopping, ensure thread is interrupted;
            // if not, ensure thread is not interrupted.  This
            // requires a recheck in second case to deal with
            // shutdownNow race while clearing interrupt
            if ((runStateAtLeast(ctl.get(), STOP) ||
                 (Thread.interrupted() &&
                  runStateAtLeast(ctl.get(), STOP))) &&
                !wt.isInterrupted())
                wt.interrupt();
            try {
                beforeExecute(wt, task);
                Throwable thrown = null;
                try {
                    task.run();
                } catch (RuntimeException x) {
                    thrown = x; throw x;
                } catch (Error x) {
                    thrown = x; throw x;
                } catch (Throwable x) {
                    thrown = x; throw new Error(x);
                } finally {
                    afterExecute(task, thrown);
                }
            } finally {
                task = null;
                w.completedTasks++;
                w.unlock();
            }
        }
        completedAbruptly = false;
    } finally {
        //todo 额外创建的线程 在等待时间内没有获取到任务 被释放销毁
        processWorkerExit(w, completedAbruptly);
    }
}
//从任务队列中取任务
private Runnable getTask() {
    boolean timedOut = false; // Did the last poll() time out?

    for (;;) {
        int c = ctl.get();
        int rs = runStateOf(c);

        // Check if queue empty only if necessary.
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
            decrementWorkerCount();
            return null;
        }

        int wc = workerCountOf(c);

        // Are workers subject to culling?
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

        if ((wc > maximumPoolSize || (timed && timedOut))
            && (wc > 1 || workQueue.isEmpty())) {
            if (compareAndDecrementWorkerCount(c))
                return null;
            continue;
        }

        try {
            Runnable r = timed ?
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
            workQueue.take();
            if (r != null)
                return r;
            timedOut = true;
        } catch (InterruptedException retry) {
            timedOut = false;
        }
    }
}
```

那么超出corePoolSize创建出的线程，一旦超过keepAliveTime指定的时间，还获取不到任务，比如keepAliveTime是60秒，那么假如超过60秒获取不到任务，他就会自动释放掉了，这个线程就销毁了。

阻塞队列有界可以保证线程要处理的任务数是有限的，如果任务数无限且任务执行时间长很容易内存飙升，导致oom。
