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

消息发送有三种模式：同步，异步，发了不管成功与否。（todo 消息异步发送不使用future而是主动传入callable，怎么保证回调函数分区有序）

元数据信息：客户端发送过程中，只知道topic是不行的，需要Sender线程负责向`leastLoadedNode`发送请求，以定时更新其他broker的元数据信息，而主线程负责读取。

#### broker

##### 消息存储

Kafka的消息由主题topic来归类，每个topic都有若干个partition分区，分区的添加实现主题IO性能的水平扩展，分区本质是可追加的日志文件，消息是顺序写入分区的，每个分区又有若干副本，分为leader副本和follower副本；leader副本负责处理读写请求，而follower副本只负责数据同步，当然会存在一定的滞后性，只能达到最终一致。

- topic
  - partition
    - leader副本：负责读写请求，监控follower副本同步情况
    - follower副本：数据同步

##### 副本同步

为了保证消息可靠性，Kafka将所有分区副本统称为AR，所有与leader副本保持一定程度同步的副本称为ISR，而剩下严重滞后的副本称为OSR（`AR=ISR+OSR`）。leader副本会维护和跟踪所有follower副本的滞后状态，如果ISR中有严重滞后的就会被踢出，OSR中赶上了leader副本的就会被移到ISR。LEO（`log end offset`）标识当前日志分区中下一条待写入的消息的offset，HW（`high` `watermark`）是分区ISR副本中消息写入的最小LEO，消费者最多能拉到HW前的消息。

ISR保证leader副本宕机后能最可靠的恢复数据，而且副本的不同程度同步（异步同步）可以降低同步带来的性能问题。

Kafka消息端consumer采用pull模式拉取消息，并保存消费的具体位置，保证consumer宕机后能重新拉取，防止消息丢失。但是consumer只能拉取到分区HW之前的消息；

#### 消费者









