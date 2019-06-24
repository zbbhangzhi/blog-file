## Zookeeper 选举过程



### 问题

1. 选举过程 服务器之间是怎么通信的？

   答：QuorumCnxManager使用TCP-socket实现选举过程中的连接通信

2. Leader的选举过程在什么时候实现？

3. Leader的选举过程是如何实现的？

4. Zookeeper的角色都有哪些，且各自有什么作用

5. Zookeeper集群为什么需要奇数个服务器

   

### 组件

##### Election

实现选举过程必须实现的接口

```java
public interface Election {
    //开启新一轮的选举：当服务器状态改为looking时，
    public Vote lookForLeader() throws InterruptedException;
    public void shutdown();
}
```

##### QuorumCnxManager

服务器之间的连接管理器，对每对服务器都维护一个连接

##### FastLeaderElection

Election的一个实现

​	Notification：通知包含其他服务器它改变了自己的投票

 	ToSend：消息包装类，向其他服务器发送通知（notification/ack）

​	 Messenger：消息处理类，内部维护两个线程，发送/接受并处理

```java
public class FastLeaderElection implements Election {
    //参与选举的服务器的托管者
    QuorumPeer self;
    Messenger messenger;
    //发送消息的存储队列：由Messenger中的WorkerSend发送
    LinkedBlockingQueue<ToSend> sendqueue;
    //从其他服务器接收到的通知的存储队列：由Messenger的WorkerRecv接收
    LinkedBlockingQueue<Notification> recvqueue;
    
    //开启消息管理器：开启接收和发送线程
    public void start() {
        this.messenger.start();
    }
    //开启新一轮的选举
    public Vote lookForLeader() throws InterruptedException {
        //1. 向其他服务器发送投票消息
        //2. 循环接收其他服务器的通知：在没有选举成功前且服务器仍在运行
        //3. 处理投票：先判断是不是同一轮选举
        	//LOOKING：比较ZXID和myid，ZXID越大越优先，如果相同，比较myid
        //4. 统计投票：判断是否已经有过半机器接受到相同的投票信息，那么就认为选出了leader
        //5. 更改服务器状态：当确定leader时 相应服务器更改状态
    }
}
```

### 实现

###### QuorumCnxManager实现服务器之间的通信

1. QuorumConnectionReqThread::initiateConnection()->startConnection()：QuorumCnxManager内部维护一个线程QuorumConnectionReqThread专门用于发送连接请求到服务器，线程其实调用外部方法initiateConnection()->startConnection()初始化连接，连接成功后，将服务器的sid从存储待处理连接的集合中移除；并启动相应的发/收消息的线程
2. QuorumConnectionReceiverThread::receiveConnection()->handleConnection()：如果服务器已经有其他连接，就将这个新接收的连接关闭

###### Zookeeper的角色

1. Leader：
2. Follower：实现Learner
3. Observer：实现Learner



https://blog.csdn.net/tanga842428/article/details/52247756

https://blog.csdn.net/qq_21178933/article/details/82841679

https://blog.csdn.net/panxj856856/article/details/80403487

https://www.cnblogs.com/leesf456/p/6139266.html