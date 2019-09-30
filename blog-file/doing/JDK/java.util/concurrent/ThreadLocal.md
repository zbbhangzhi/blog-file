## ThreadLocal

线程持有对象：一个实例只能被一个线程访问。ThreadLocal可以被理解为当前线程访问其线程持有对象的代理对象。ThreadLocal实例也被称为**线程局部变量**，ThreadLocal的使用其实是**以空间换时间**（一个线程持有一小块工作内存），去掉了锁的使用，同时减少了线程不安全的类的创建次数。

ThreadLocal通常作为某个类的静态字段，防止同个线程每次创建类的对象都生成一个新的变量而造成浪费，这样就可以让多个线程各自持有自己的变量实例

```Java
class ThreadLocal{
    class ThreadLocalMap{
        Entry<ThreadLocal, Map> entry;
            //ThreadLocal key;
            //Map value;				//独享数据
    }
}
```



### 内部实现

一个ThreadLocal内部其实是多个ThreadLocalMap，类似HashMap，有多个Entry（k-v），key为一个ThreadLocal实例，value是一个线程持有对象。

### 问题

1. 线程会在不同时间段执行多个不同的任务，所以线程局部变量可能会在任务之间形成数据错乱，所以我们需要保证每个任务执行前相应的线程持有对象的状态不受前一个任务的影响。那么我们就需要在任务执行之前，将线程局部变量清空。

2. ThreadLocal可能造成内存泄漏/伪内存泄漏（对象占用内存空间长时间不被垃圾回收）

   Entry对ThreadLocal实例（key）的引用是一个弱引用，当一个ThreadlLocal实例没有对其可达的强引用时，这个实例就会被垃圾回收，Entry中的key会被置为null，这Entry对于ThreadlLocalMap就是一个无效条目；但Entry对线程持有对象（value）的引用是强引用，所以这个无效条目就对它有可达的强引用，会阻止它被垃圾回收。

   对ThreadlLocalMap中set/get方法调用时，会将无效条目给删除，这会打破无效条目对线程持有对象的强引用，从而使其可能被回收；但是可能出现这个线程在访问过这个线程持有对象后是可达的强引用，且线程长时间处于非运行状态，一直不结束，比如线程池，可能导致无效目录一直不会被清除，其他Entry所引用的线程持有对象不会被清除，这样就导致了内存泄漏/伪内存泄漏。

   这里的问题就是，线程对线程持有对象的强引用导致的无法垃圾回收，所以只需要主动把这个线程持有对象删除就可以了`ThreadLocal.remove()`;

   todo 6-11例子 servlet使用threadLocal而避免线程安全和内存泄漏

### 使用场景

1. 希望线程安全，且减少使用锁带来的开销
2. 线程下的单例模式，每个线程只持有各自的一个实例

![img](https://upload-images.jianshu.io/upload_images/4943997-cac8c7ca612012f7.png)