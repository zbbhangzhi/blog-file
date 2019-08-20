## Kafka简介

### 定义

Kafka是一个分布式流式处理平台，它提供三种角色：消息系统，存储系统，流式处理平台

### 组成

Kafka架构体系为实现消息模块包括了若干producer，broker和consumer，还有zookeeper集群来负责元数据的管理和控制器选举等操作。

#### 生产者

##### 发送过程

- 主线程创建消息`ProducerRecord`
- 拦截器：对消息做一些定制化操作
- 序列化：序列化成字节数组，方便网络传输
- 分区器：如果未指定分区，可重新计算确定分区号
- 消息累加器：实现以分区为组缓存到`RecordAccumulator`（内部维护一个Dequeue），方便批量发送，提高吞吐量
- 提交给Send线程
- 从`RecordAccumulator`获取缓存的消息，将`分区-消息列表`转换为`broker-消息列表`
- 创建请求`ProduceRequest`，同时保存在`InFlightRequests`（发出但未响应，可用来计数限制发送给broker数量大小，负载最小的称为`leastLoadedNode`）
- 提交给selector准备发送

`KafkaProducer`提供发送功能（且是批量发送，有较高的吞吐量），它是线程安全的。消息发送只支持字节数组形式，producer需要构建消息，将序列化后的消息及一些基本信息包装成`ProducerRecord`类；如果没有指定发送的分区，需要producer调用分区器`Partition`接口的partition()方法为消息计算分区号（根据key进行哈希运算，没有就轮询）。

##### 消息发送模式

同步，异步，发了不管成功与否。（todo 消息异步发送不使用future而是主动传入callable，怎么保证回调函数分区有序）

##### 元数据信息

客户端发送过程中，只知道topic是不行的，需要Sender线程负责向`leastLoadedNode`发送请求，以定时更新其他broker的元数据信息，而主线程负责读取。

#### broker

##### 消息存储

Kafka的消息由主题topic来归类，每个topic都有若干个partition分区，分区的添加实现主题IO性能的水平扩展，分区本质是可追加的日志文件，消息是顺序写入分区的；

每个分区又有若干副本，分为leader副本和follower副本，（每个副本需要分布于不同的broker上）以提供数据冗余及提高数据可靠性；leader副本负责处理读写请求，而follower副本只负责数据同步，当然会存在一定的滞后性，只能达到最终一致。

每个副本对应一个日志文件，每个日志文件对应一至多个日志分段`LogSegment`，每个日志分段还可细分为索引文件，日志存储文件，快照文件等。

- topic主题
  - partition分区
    - leader副本：负责读写请求，监控follower副本同步情况
    - follower副本：数据同步

主题管理

主题管理包括：主题查看/描述/创建/删除，分区个数，分区副本分配情况

管理方式包括：API方式：通过KafkaAdminClient，脚本方式：TopicCommand类，物理操作：日志文件和Zookeeper节点。

分区管理

分区副本分配策略

分区数选择

##### 副本同步

为了保证消息可靠性，Kafka将所有分区副本统称为AR，所有与leader副本保持一定程度同步的副本称为ISR，而剩下严重滞后的副本称为OSR（`AR=ISR+OSR`）。leader副本会维护和跟踪所有follower副本的滞后状态，如果ISR中有严重滞后的就会被踢出，OSR中赶上了leader副本的就会被移到ISR。LEO（`log end offset`）标识当前日志分区中下一条待写入的消息的offset，HW（`high` `watermark`）是分区ISR副本中消息写入的最小LEO，消费者最多能拉到HW前的消息。

ISR保证leader副本宕机后能最可靠的恢复数据，而且副本的不同程度同步（异步同步）可以降低同步带来的性能问题。

Kafka消息端consumer采用poll模式拉取消息，并保存消费的具体位置，保证consumer宕机后能重新拉取，防止消息丢失。但是consumer只能拉取到分区HW之前的消息；

#### 消费者

Kafka是基于轮询的方式不断**拉取**poll其所订阅的主题/分区上的一组消息，如果没有消息可拉取，就返回一个空集合。如果想要控制消费速度，可以调用pause()暂停消费，resume()恢复消费。最后显式的关闭close()占用的资源

##### 订阅方式

一个消费者只能使用一种订阅方式，可同时订阅一个或多个主题。subscribe订阅方式具有**消费者自动再均衡**的功能，在多个消费者（增加/减少）情况下根据**分区分配策略**来自动分配消费者和分区之间的关系，以实现负载均衡和故障自动转移。

- AUTO_PATTERN：正则表达式订阅subscribe(Pattern)
- AUTO_TOPICS：集合订阅subscribe(Collection)
- USER_ASSIGNED：指定分区订阅assign(Collection)

再均衡：分区的所属权从一个消费者转移到另一个消费者，使得消费组具有高可用和强伸缩性。Kafka提供再均衡监听器ConsumerRebalanceListener，当消费者被删除或新增时，做一些善后操作，如消费位移的保存等。

##### 消费组与消费者

消费组的存在提高了整体的消费能力，但为了合理扩展消费能力，需要将消费组内消费者数量设置的和其所订阅的主题下的分区数量接近。消费者与订阅主题之间的分区分配策略 todo

##### 消息投递模式

- 点对点模式：基于队列实现生产和消费（只有一个消费组）
- 发布订阅模式：在消息的一对多广播时使用，主题topic相当于中介，使生产者和消费者无需接触就可以保证消息的传递（有多个消费组）

##### 消息模型

ConsumerRecord为单个消息格式，ConsumerRecords为多个消息组装成的格式，用于消息接收

```java
class ConsumerRecord<K, V>{
    String topic;
    int partition;
    long offset;					//消息在所属分区的偏移量
    long timestamp;
    TimeStampType timeStampType;
    int serializedKeySize;
    int serializedValueSize;
    Headers headers;
    K key;							//消息键
    V value;						//消息值
    Long checkSum;					//CRC32的校验值
}
class ConsumerRecords{
    
}
```

##### 消费过程

- 消费者订阅`subscribe`主题topic
- 消费者从topic上拉取`poll()`消息
  - 消费位移记录：消费位移持久化于主题`__consumer_offsets`，存储当前消费位移+1，下次消费开始的位置；消费位移提交方式不恰当可能会产生消费丢失或重复消费的问题；所以Kafka中消费位移默认提交的方式是定期自动提交每个分区中最大的消费位移，这个动作在poll方法中，每次拉取消息前提交。或者我们手动调用提交：同步`commitSync`（可以按照分区粒度同步提交），异步提交`commitAsync`
  - 消费者协调器
  - 消费者拦截器ConsumerInterceptor
    - onConsumer()：poll方法返回前，对消息做定制化操作
    - onCommit()：消费位移提交后，记录跟踪所提交的位移消息
- 消息`ConsumerRecords`投递给消费组中的一个消费者
- 收到消息的消费者转发给组内的其他消费者

##### 回溯消费




