## CountDownLatch

### 作用

CountDownLatch有点类似于Thread.join()，但又不同于Thread.join()暂停等待其他线程终止，而是暂停等待某些动作（先决操作）执行成功即可继续运行。

### 实现原理

内部维护一个**先决操作数量**的计数器，而不是先决操作需要被执行的数量（即countDown方法调用次数），countDown每执行一次递减1；而await方法相当于内部维护一个保护条件为计数器值为0的保护方法。当计数器值大于0时，CountDownLatch.await的执行线程会暂停，当countDown操作使计数器为0时，有一个通知的动作，唤醒这个实例上的所有等待线程。

### 特性

- 可见性和有序性

  对于同一个countDown实例，countDown方法执行线程在执行该方法之前所执行的任何内存操作对等待线程在await方法中调用返回的代码是**可见且有序**的。

- 一次性

  countDown直到计数器为0时，就不再继续做减法，即使有调用，也不会抛错，因此CountDownLatch是一次性的，只有**一次等待和一次唤醒**。

### 使用

因为countDown本身是无法判断它的操作结果是成功还是失败的，只能向CountDownLatch报告它完成了某个操作，所以为了避免CountDownLatch因为程序错误，其内部计数器永远无法为0而使等待线程永远等待，需要将countDown方法放在**finally**中，保证一定会被执行，或在await方法中声明一个**超时时间**，如果超时时间外，计数器仍然不为0，就直接唤醒全部等待线程。

（todo await和countDown方法调用的时候无需加锁 为什么）
