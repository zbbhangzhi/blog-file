Disruptor

用于解决PCP问题（生产者消费者），尽量获得高的吞吐量TPS和低延迟。

低并发

为解决生产者消费者在并发下产生的安全问题，jdk的解决方案是BlockQueue，如ArrayBlockingQueue，LinkedBlockingQueue；以ArrayBlockingQueue为例，它内部维护一个可重入锁lock，当获取/添加线程要操作队列时，需要先获取到锁，保证原子性。如果已有线程获取到锁，那么其他线程进入阻塞状态，等待之前的线程释放锁。当读/写操作结束，根据队列个数，唤醒相应的等待线程。比如添加元素后，如果队列个数从0变为1，可唤醒消费线程消费。反之。

但是，阻塞队列在低并发的情况下可以支撑，但随着并发量增加，持有锁的线程来回切换，开销增大。锁的获取与释放频繁，性能出现瓶颈。且多线程下可能代码没有按照想要的顺序执行。

高并发

Disruptor，相比阻塞队列disruptor做了一些性能优化。

1. 无锁用指针代替，使用CAS移动写指针保证线程安全。Disruptor用RingBuffer存放生产者产生的对象，RingBuffer就是一个长度为n的环形缓冲区，生产者消费者各有一个指针，生产者指针指向下一个要写入的slot，消费者指针指向下一个要读取的slot。生产/消费后指针p=(p+1)%n。

   （此处完全摘抄）

   > **①**当生产者和消费者都只有一个时，由于两个线程分别操作不同的指针，所以不需要锁。
   >
   > **②**当有多个消费者时，（按Disruptor的设计）每个消费者各自控制自己的指针，依次读取每个Slot（也就是每个消费者都会读取到所有的产品），这时只需要保证生产者指针不会超过最慢的消费者（超过最后一个消费者“一圈”）即可，也不需要锁。
   >
   > **③**当有多个生产者时，多个线程共用一个写指针，此处需要考虑多线程问题，例如两个生产者线程同时写数据，当前写指针=0，运行后其中一个线程应获得缓冲区0号Slot，另一个应该获得1号，写指针=2。对于这种情况，Disruptor使用CAS来保证多线程安全。

   CAS即caompare and set，它是由硬件实现的极轻量级指令，由cpu保持操作的原子性。使用cas可以避免多线程带来的不安全。写指针伪代码（完全摘抄）

   > //写指针向后移动n
   > public long next(int n)
   > {
   >     //......
   >     long current,next;
   >     do
   >     {
   >         //此处先将写指针的当前值备份一下
   >         current = pointer.get();
   >         //预计写指针将要移动到的位置
   >         next = current + n;
   >         //......省略：确保从current到current+n的Slot已经被消费者读完......
   >         //*原子操作*如果当前写指针和刚才一样（说明9-12行的计算有效），那么移动写指针
   >         if ( pointer.comapreAndSet(current,next) )
   >             break;  
   >     }while ( true )//如果CAS失败或者还不能移动写指针，则不断尝试
   >     return next;
   > }

2. CPU缓存行填充。现在是多核操作系统，而且cpu缓存是以行的形式缓存。当核1某一缓存行同时存储了生产者指针p1和消费者指针c1，其他核也会存储相同的指针，当核1的指针p1更改时，其他核的p1也就失效了。更新时务必影响到c1。所以对于一个long型的缓冲区指针，用一个长度为8的long型数组代替，这样一个缓存行被这个数组填满，就不会影响到其他指针了。

3. 避免gc，提前分配。创建实例时，RingBuffer会首先将缓冲区填满Factory产生的实例，用完就还回去，要用就用之前已经new好的实例。（这和Kafka提高网络通信，用内存池分配内存块存储消息是一样的。）

4. 成批操作batch。RingBuffer中操作为生产和消费。（这和kafka的消息batch发布一样，batch打包发送，减少发送频率。）

   生产操作分两步：一，先是申请空间，生产者获得一个指针范围（n，m）(n < m)，然后对缓冲区（n，m）这段的所有对象进行setValue；二，发布ringBuffer.publish(n,m)。步骤一结束，其他生产者申请空间时会得到另一段缓冲区；步骤二结束，消费者将能读到（n，m）这段数据。如果将生产成批，发布成批，那么将减少生产同步带来的性能损失。

   消费同样分为两步，一，等待生产者的指针大于指定值r，表示有新的数据产生。二，当得到新指针n时读取数据。如果将消费成批，那么可以直接读取（r，n）范围内的数据

disruptor源于LMAX架构

> LMAX架构：（注：指的是LMAX公司在做他们的交易平台时使用的一些设计思想的集合，严格讲是LMAX架构包含Disruptor，并非其中的一部分，但是Disruptor的设计中或多或少体现了这些思想，所以在这还是要提一下，关于LMAX架构应该可以写很多，但限于个人水平，在这只能简单说说。另外，这个架构是以及极端追求性能的产物，不一定适合大众。）如下图所示LMAX架构分为三个部分，输入/输出Disruptor，和中间核心的业务逻辑处理器。所有的信息输入进入Input Disruptor，被业务逻辑处理器读取后送入Output Disruptor，最后输出到其他地方。

该架构有几个特点

1. 异步事件驱动：lmax最大的特点就是快速，高效，所以对于用时长需要等待的逻辑处理，它会抛到disruptor中异步处理。
2. 业务逻辑处理器是单线程的，因为多线程会带来业务复杂度，限制业务处理速度。
3. 如果想用多线程可以使用管道模式处理多级业务逻辑。

> 下图的3块结构可以以多种方式组合，一个BLP可以将输出送往多个Output Disruptor，而这些Disruptor可能是另一些3块结构的Input Disruptor，即有些BLP是起到分发作用的，另一些是进行具体业务逻辑计算的。每个BLP对应一个线程，整个架构可能比上图复杂很多。

![LMAXæ¶æ](http://ifeve.com/wp-content/uploads/2013/01/arch-summary.png)

例子对比

```java
/**
 * 分别用LinkedBlockingQueue和Disruptor实现监听模式
 */
public class TestForConcurrent {
    //要生产的对象数量
    final long objectCount = 1000000;
    //缓冲区大小
    final long bufferSize;

    {
        bufferSize = getRingBufferSize(objectCount);
    }

    static long getRingBufferSize(long num){
        long s = 2;
        while (s < num){
            s <<= 1;
        }
        System.out.println("s = [" + s + "]");
        return s;
    }

    public static void main(String[] args)throws Exception {
        TestForConcurrent test = new TestForConcurrent();
//        test.blockingQueue();
        test.disruptor();
    }

    public void disruptor()throws Exception{
        //创建单生产者的RingBuffer，EventFactory是填充缓冲区的对象工厂
        //YieldingWaitStrategy是等待策略 指出消费者等待数据变得可用前的等待策略
        RingBuffer<TestObj> ringBuffer = RingBuffer.createSingleProducer(new EventFactory<TestObj>() {
            @Override
            public TestObj newInstance() {
                return new TestObj(0);
            }
        }, (int)bufferSize, new YieldingWaitStrategy());
        //创建消费者指针
        final SequenceBarrier barrier = ringBuffer.newBarrier();
        //生产线程
        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= objectCount; i++){
                    //申请下一个缓冲区slot
                    long index = ringBuffer.next();
                    //在缓冲区中的slot赋值
                    ringBuffer.get(index).setValue(i);
                    //发布让消费者可以读到
                    ringBuffer.publish(index);
                    System.out.println("生产者在" + index + "生产了数据");
                }
            }
        });
        //消费线程
        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                TestObj readObj = null;
                int readCount = 0;
                long readIndex = Sequencer.INITIAL_CURSOR_VALUE;
                while (readCount < objectCount){
                    try {
                        //设置下一个该读的位置
                        long nextIndex = readIndex + 1;
                        //等待直到上面的位置可读
                        long availableIndex = barrier.waitFor(nextIndex);
                        while (nextIndex <= availableIndex){
                            //获取buffer中的对象
                            readObj = ringBuffer.get(nextIndex);
                            System.out.println("消费者读到第" + nextIndex + "数据" + readObj.toString());
                            readCount++;
                            nextIndex++;
                        }
                    } catch (AlertException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        long timeStart = System.currentTimeMillis();
        producer.start();
        consumer.start();
        consumer.join();
        producer.join();
        long timeEnd = System.currentTimeMillis();
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        System.out.println((timeEnd - timeStart) + "/" + df.format(objectCount) +
                " = " + df.format(objectCount/(timeEnd - timeStart)*1000) );//每秒执行 9013/1,000,000 = 110,000个
    }

    public void blockingQueue()throws Exception{
        final LinkedBlockingQueue<TestObj> queue = new LinkedBlockingQueue<>();
        //生产线程
        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= objectCount; i++){
                    try {
                        queue.put(new TestObj(i));
                        System.out.println("生产第" + i + "个，队里还有"+ queue.size() +"个");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        //消费线程
        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i <= objectCount; i++){
                    try {
                        queue.take();
                        System.out.println("消费第" + i + "个，队里还有"+ queue.size() +"个");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        long timeStart = System.currentTimeMillis();
        producer.start();
        consumer.start();
        consumer.join();
        producer.join();
        long timeEnd = System.currentTimeMillis();
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        System.out.println((timeEnd - timeStart) + "/" + df.format(objectCount) +
                " = " + df.format(objectCount/(timeEnd - timeStart)*1000) );//9647/1,000,000 = 103,000
    }
    //缓冲区中的元素
    private class TestObj{
        public long value;
        public TestObj(long value) { this.value = value; }
        public long getValue() { return value; }
        public void setValue(long value) { this.value = value; }
        @Override
        public String toString() {
            return "TestObj{" + "value=" + value + '}';
        }
    }
}
```





转自 <http://www.cnblogs.com/shenck/p/4002456.html#top>

待看剖析 <http://ifeve.com/disruptor/>























**WHERE** contract_no [=](http://zabbix.2dfire.net/mysql/url.php?url=http%3A%2F%2Fdev.mysql.com%2Fdoc%2Frefman%2F5.6%2Fen%2Fcomparison-operators.html%23operator_equal&server=5&token=48c331be7d752737905bc5f633d52abe) '375369587362971684'

375369587362971684                   00341867             

00358705

**WHERE** id [=](http://zabbix.2dfire.net/mysql/url.php?url=http%3A%2F%2Fdev.mysql.com%2Fdoc%2Frefman%2F5.6%2Fen%2Fcomparison-operators.html%23operator_equal&server=14&token=48c331be7d752737905bc5f633d52abe) '0034186766bfae360166bfcea68f000f'



cn367369619071059848---------------------si430770846659822078