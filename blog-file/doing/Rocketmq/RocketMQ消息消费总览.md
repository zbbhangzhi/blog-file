RocketMQ消息消费总览

消息放入的时候会锁住当前位置，不然会被其他的覆盖

queueOffset：

logicOffset：queueOffset*size

physicalOffset：commitLog中的全局偏移量

todo：消息放入会异步dispatch到consumerQueue

todo：消息消费后会返回一个ACK状态



消费者启动，准备好订阅的topic路由信息，rebalance分发出去；构建客户端



消息消费过程：

1. 消费者注册监听器
2. 从broker拉取消息：

- 获取方式
- 获取流程
  1. 锁消息
  2. 根据logicOffset拿到physicalOffset
  3. 反序列化拿到的消息数据

1. 将消息缓存到TreeMap
2. 消费消息
3. 报告消费结果到broker



### 介绍

#### 监听器MessageListener

- MessageListenerConcurrently
- MessageListenerOrderly

#### 拉取消息服务

- MQPushConsumer

```java
//MQPushConsumer
public interface MQPushConsumer extends MQConsumer {
    //消费服务defaultMQPushConsumerImpl开启，有需要traceDispatcher消息追踪也会开启
    void start() throws MQClientException;
    //注册消息监听器
    void registerMessageListener(final MessageListenerConcurrently messageListener);
    //topic订阅
    void subscribe(final String topic, final String subExpression) throws MQClientException;
}
```

- MQPullConsumer

#### 客户端实例MQClientInstance

- start()：启动线程/服务
  - this.mQClientAPIImpl.fetchNameServerAddr()：获取namesrv地址并更新当前客户端维护的地址列表
  - mQClientAPIImpl：启动与broker/namesrv通信的客户端
  - startScheduledTask：开启定时任务
    - fetchNameServerAddr更新namesrv地址
    - updateTopicRouteInfoFromNameServer从namesrv更新topic路由信息
    - cleanOfflineBroker移除下线的broker，向所有broker发送心跳
    - persistAllConsumerOffset持久化消费进度
    - adjustThreadPool调整线程池
  - pullMessageService：启动拉消息线程服务
  - rebalanceService：启动负载均衡线程服务
  - this.defaultMQProducer.getDefaultMQProducerImpl()

#### 拉取消息线程pullMessageService

- run()：方法中循环拉取消息pullMessage()，从pullMessageService维护的拉取消息请求队列pullRequestQueue中取出一个请求，最终调用MQConsumerInner实现类的pullMessage()

#### 消费者内部接口MQConsumerInner

- DefaultMQPushConsumerImpl
  - executePullRequestImmediately消息怎么来的
  - pullMessage
- DefaultMQPullConsumerImpl





消息消费过程：

1. 从broker拉取消息：消费者拉消息是怎么拉的，pull/push

   - 获取方式：

     - MQPushConsumer----DefaultMQPushConsumerImpl

       - start()

         	1. 检查消费者配置
                 2. 复制消费者配置的订阅信息，并同步到负载均衡服务
                 3. 集群模式下 更改消费实例名为进程id
                 4. 构建消费者客户端 用于与broker，namesrv通信
                 5. 配置当前消费者负载均衡服务信息
                 6. 构建包装从broker拉取的消息数据服务
                 7. 根据消费模式 获取消费队列的消费进度存储器；并加载
                     集群：从broker获取远程存储器；广播：获取本地存储器
                 8. 初始化并启动消费监听器
                 9. 注册当前消费者 todo
                 10. 启动消费者客户端MQClientInstance

         ​        开启pullMessage线程
         ​        从NameSrv更新订阅的topic路由信息
         ​        检查目标地址的broker和订阅信息是否一致
         ​        向所有broker发送心跳消息
         ​        启动负载均衡线程

            

     - MQPullConsumer

   - 获取流程

     1. 根据logicOffset拿到physicalOffset
     2. 反序列化拿到的消息数据

   

2. 将消息缓存到TreeMap

3. 消费消息

4. 报告消费结果到broker







负载均衡过程：todo：rocketmq与kafka比较：kafka由leader决定。。。

将同一consumerGroup的消费者所订阅的所有topic 负载均衡到各个机器上





















