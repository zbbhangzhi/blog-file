### Zookeeper和Paxos

Zookeeper是一个分布式协调服务，是Chubby的开源实现。它提供一个典型的分布式数据一致性的解决方案，它将复杂且容易出错的分布式一致性服务封装起来，构成一个高效可靠的原语集。没有直接采用Paxos算法，而是采用ZAB（zookeeper atomic broadcast）协议。

#### 功能特性

Zookeeper提供了一系列保证分布式一致性特性的功能，如数据发布/订阅服务，负载均衡，命名服务，分布式协调/通知，集群管理，Master选举，分布式锁和分布式队列等。这些功能包含如下特性：

- 顺序一致性（对于客户端发起的请求，服务端会严格按照发起顺序执行）
- 原子性（事务请求的结果集在所有机器上是一致的要么提交要么没有）
- 单一视图（所有服务器的数据模型一致）
- 可靠性（一旦服务端对客户端的请求响应并处理，那么它的最终状态会一直存在，除非下一个事务更改）
- 实时性（客户端在一定时间内读到的数据一定是最新的）

#### 架构设计

1. 集群cluster：zookeeper集群不同常见主备Master/Slave关系的集群而是采用Leader/Follower/Listener三角色
2. 会话session：客户端与服务端通过一个TCP长连接，开启会话的生命周期，并通过心跳检测保持有效的会话。
3. 数据节点znode：zookeeper的数据模型是一棵树ZNode Tree，斜杠分割路径（或称ZNode），每个ZNode除了保存数据内容还会带点属性信息。ZNode可分为临时节点（与会话的生命周期一致）和持久节点（需要主动删除）。
4. 版本：每个ZNode都有自己的存储数据，可能也有自己的子节点，所以zookeeper为每个数据节点都维护了一个数据结构Stat用于记录这个系欸但的三个数据版本：version（znode版本），cversion（子节点版本），aversion（当前znode的acl版本）
5. watcher监听事件：客户端在指定节点上注册多个watcher，某些目标事件触发时，服务端会将事件通知给客户端
6. ACL权限控制列表：有以下5种权限对节点的控制
   - CREATE：创建节点权限
   - READ：获取节点数据和子节点列表权限
   - WRITE：更新节点数据的权限
   - DELETE：删除子节点的权限
   - ADMIN：设置节点ACL的权限

#### 一致性协议ZAB

Zookeeper Atomic Broadcast支持崩溃可恢复的原子广播协议，基于这个协议，zookeeper实现了一种主备模式的系统架构来保持集群中各副本之间的数据的一致性。

##### 处理过程

- 所有客户端发送事务请求给服务端，由一个全局唯一的服务器（Leader）处理
  - 服务器使用一个单一的主进程来接收并处理客户端的所有事务请求
  - 用ZAB的原子广播协议将服务器数据的状态变更以事务proposal的形式广播到所有副本进程follower
  - leader服务器等待所有follower服务器的反馈
    - 超过半数follower正确反馈后，leader向所有follower发送commit消息要求follower将前一个proposal提交
    - 如果leader机器挂了或者少于半数机器和leader没有通信成功，所有进程进入崩溃恢复协议以达成一致

##### ZAB协议的三种模式

- 恢复模式：整个服务框架在启动或者leader服务器挂了，那么进入恢复模式选举产生新的leader服务器

  - 新leader服务器一定要拥有集群中最大ZXID的事务proposal，并让自己和集群知道leader是谁
    
    阶段-发现Discovery，可以保证：
    
    - 新leader要确保过半的follower已经提交了老leader周期的所有proposal
    - 如果之前的leader提出一个proposal就挂了，其他follower没有接收到，那么这个proposal就是一个丢弃的proposal，当这个挂了的leader重新加入到集群时，新leader会根据自己服务器上最后被提交的proposal和这个丢弃proposal比对，要求它丢弃这个proposal，回退到一个确实被集群中过半机器提交的最新proposal
    
    阶段-同步Synchronization，可以保证：
    
  - follower机器和leader进行数据同步，保证每个follower可以和leader正常处理proposal，过程：
    - leader给每个follower准备一个队列
    - leader将没有被各follower同步的事务以proposal的形式逐个发送给follower，并紧接着发送一个commit消息，表示该事务已提交
    - 全部未提交消息都成功被follower提交后，follower就可以加入到真正可用的follower列表
    
  - 过半数同步成功后，退出恢复模式

- 消息模式：恢复模式结束后进入消息模式

  - 有新的机器加入，这个机器先进入数据恢复模式，主动找leader与它进行数据同步
  
    阶段-广播Broadcast：
  
- leader收到客户端事务请求后，会生成对应的提案proposal并发起广播协议
  
- 如果时其他非leader机器收到客户端的请求，会先把这个请求转发给leader
  
- 如果leader机器挂了或者少于半数机器和leader没有通信成功，所有进程进入崩溃恢复协议以达成一致
  
  广播协议
  
  广播协议是一个原子性协议，类似于一个二阶段提交过程。
  
  - ZAB只要求过半数follower反馈ack即可开始提交事务。但是这会带来因leader挂了，集群数据不一致的问题。因此ZAB使用崩溃恢复模式来解决这个问题
  - 消息广播中消息的发送与接受有顺序性，因为采用了具有FIFO特性的TCP协议来进行网络特性，而且服务器是会为每个事务proposal分配一个全局唯一且单调递增的事务ID（ZXID，64位数字，高32位代表leader周期的epoch编号，低32位代表递增的计数器），因此处理proposal前需要将proposal先排序。

##### ZAB协议的三种状态

- LEADING：leader作为主进程的领导状态
- LOOKING：服务启动或leader挂了，leader选举
- FOLLOWING：已经选举出leader，和leader保持同步状态











