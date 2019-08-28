## Java异步编程

同步任务：任务发起和执行是同时的

异步任务：任务发起后不一定直接执行，可能是在其他时间点执行

### 异步任务批量执行

CompletionService

ExecutorCompletionService是接口CompletionService的默认实现，内部维护一个阻塞队列（默认LinkedBlockingQueue或自行指定）存储已执行并得到的执行结果，调用take()获取一次结果。

#### 属性

- executor：任务执行器
- completionQueue：存储已执行完毕的异步任务对应的future实例

#### 方法

- submit：提交异步任务（runnable/callable），内部为每个任务包装成一个FutureTask实例，并返回（在任务结束时，将任务放入completionQueue中）
- take：是个阻塞方法，从completionQueue中拿出并返回已执行结束异步任务对应的future实例
- poll：非阻塞方法，获取异步任务的处理结果，同上，提供超时时间制接口

### 可获取结果的异步执行

Callable，通过ThreadPoolExecutor.submit(Callable<T>)的返回值得到任务的处理结果，只能在线程池内执行，不能单独执行

### 可取消的异步计算

FutureTask，所继承的RunnableFutue是Callable和Future的子类，所以融合了他俩的优点：它的任务既可以交给线程池执行（Runnable为构造器参数），也可以单独给工作线程（Callable为构造器参数），同时能返回其执行结果，它的执行线程和获取结果线程是并发执行的。

#### 属性

（会专门记录它们在内存中的偏移量）

- state：任务执行状态，有7种状态（NEW,COMPLETING,NORMAL,EXCEPTION,CANCELED,INTERUPTING,INTERUPTED），初始化状态是NEW
- callable：任务
- outcome：执行结果，状态为NORMAL时可拿到正确结果，其余状态会抛错
- runner：当前执行线程
- waiters：等待线程，维护了一个简单的单向链表

#### 方法

- cancel：在任务执行还是NEW时可取消，更改状态，中断当前执行线程，结束处理流程（替换等待线程中下一个执行线程，调用done方法）
- get：获取执行结果，当状态还处于结束之前，阻塞等待，反之直接获取
- run：只在状态为NEW时执行，说明一个任务只能执行一次
- runAndSet：实例所代表的任务能被多次执行，但是它不会标记任务的执行结果，任务结束时重新标记状态

### 计划任务

ScheduledExecutorService



























