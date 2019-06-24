Zookeeper数据模型ZNode

#### 问题

- ZK的数据模型ZNodes是什么样的：

  ​	树形结构，每个叶子节点都存储着数据，且可挂载子节点；

  ​	路径作为叶子节点名称，数据作为叶子节点内的数据；

- Znode可以存储什么类型的数据

#### 特性

- watcher数据变更通知：客户端在节点上设置监控，当节点发生变化时，会触发监控，zk向客户端发送通知

- 数据访问：对存储在命名空间的节点以原子方式读取和写入，每个节点都有一个访问控制列表ACL

  ​	ACL(sechema​ ​:id :​permision)：

  ​		权限模式schema(IP,Digest,World,Super)，

  ​		授权对象ID，

  ​		权限permission(CREATE,DELETE,READ,WRITE,ADMIN)

- 节点类型：

  ​	持久节点PERSISTENT，

  ​	持久顺序节点PERSISTENT_SEQUENTIAL：按照创建先后顺序添加数字后缀

  ​	临时节点EPEMERAL：其生命周期与客户端会话绑定，客户端失效，节点被清除，且不能作为父节点

  ​	临时顺序节点EPEMERAL_SEQUENTIAL

- 版本：保证分布式数据原子性操作

  ​	每个节点都维护三种版本：

  ​		version数据内容版本号

  ​		cversion子节点版本号

  ​		aversionACL变更版本号

#### 描述

- 路径：以斜线分割
- 存储空间：分层的命名空间，每个节点包含与之关联的数据及子节点
- stat：每个znode维护一个stat结构，内包含数据更改的版本号（具体用于更新操作，类似悲观锁），acl更改，时间戳

ZKDatabase：类似数据库

​	DataTree：数据库内的结构-节点树

​		ConcurrentHashMap<String, DataNode> nodes：节点树上的节点集合



###### DataNode

DataNode是数据存储的最小单元，其内部除了保存了结点的数据内容、ACL列表、节点状态之外，还记录了父节点的引用和子节点列表两个属性，其也提供了对子节点列表进行操作的接口。

```java
public class DataNode implements Record {
	//节点数据
    byte data[];
    //节点的acl的映射；dataTree上另存map
    Long acl;
    //节点持久化在磁盘的状态
    public StatPersisted stat;
    //该节点的子节点集合
    private Set<String> children = null;
}
```

###### DataTree

```java
public class DataTree {
    //节点集合：k-路径，v-节点
    private final ConcurrentHashMap<String, DataNode> nodes =
        new ConcurrentHashMap<String, DataNode>();
    //数据监控：内部维护监控集合
    private IWatchManager dataWatches;
	//子节点监控
    private IWatchManager childWatches;
    //一个会话中短命的节点？？？？
    private final Map<Long, HashSet<String>> ephemerals =
        new ConcurrentHashMap<Long, HashSet<String>>();
}
```

###### ZKDatabase

zookeeper的内存数据库，管理zookeeper的所有会话，dataTree存储和事务日志，它会定时向磁盘dump快照数据，同时在zk启动时，会通过磁盘的事务日志和快照文件恢复成一个完整的数据库

```java
public class ZKDatabase {
    protected DataTree dataTree;
    protected ConcurrentHashMap<Long, Integer> sessionsWithTimeouts;
    //快照日志：一个database一个snaplog
    protected FileTxnSnapLog snapLog;
    protected LinkedList<Proposal> committedLog = new LinkedList<Proposal>();
    protected ReentrantReadWriteLock logLock = new ReentrantReadWriteLock();
}
```



#### 基本操作

1. 创建节点：声明节点存储路径，节点存储模式（持久化/临时）
2. 获取节点
3. 获取子节点
4. 更改节点并触发watcher监控：会向客户端发送通知，客户端线程从WatcherManager中取出对应的Watcher对象来执行回调逻辑

```java
public class ZKTest{
    //声明zk客户端
	private ZooKeeper zookeeper;
    //数据存储根路径
    private final String dir;
    //数据访问权限列表
    private List<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;
    //节点存储模式：默认持久
    private CreateMode createMode = CreateMode.PERSISTENT_SEQUENTIAL;
    public ZKTest(ZooKeeper zookeeper, String dir, List<ACL> acl){
        this.dir = dir;
        if(acl != null){
            this.acl = acl;
        }
        this.zookeeper = zookeeper;
    }
    // 1.创建节点
    public boolean addNode(byte[] data){
        try{
            zookeeper.create(dir+"/"+prefix, data, acl, createMode);
            return true;
        }catch(KeeperException.NoNodeException e){
            zookeeper.create(dir, new byte[0], acl, CreateMode.PERSISTENT);
        }
    }
    // 2.获取节点
    public byte[] getData(){
        return zookeeper.getData(dir, false, null);
    }
    // 3.获取路径下的子节点
    public List<String> getChildren(){
       return childNames = zookeeper.getChildren(dir, watcher);
    }
}
```



```java
public class Zookeeper{
    //根据给定的路径path，访问权限acl，存储模式createMode等创建节点
    public String create(final String path, byte data[], List<ACL> acl,
            CreateMode createMode)
        throws KeeperException, InterruptedException{
        final String clientPath = path;
        //校验路径 且是否允许节点已存在 如果已存在路径名称+1 否就覆盖
        PathUtils.validatePath(clientPath, createMode.isSequential());
        //根据createMode辨别如何创建节点
        EphemeralType.validateTTL(createMode, -1);
        //校验acl列表是否为空
        validateACL(acl);
		//将chroot前置到clientPath
        final String serverPath = prependChroot(clientPath);
		//声明请求头 为请求服务端创建节点做准备
        RequestHeader h = new RequestHeader();
        h.setType(createMode.isContainer() ? ZooDefs.OpCode.createContainer : ZooDefs.OpCode.create);
        CreateRequest request = new CreateRequest();
        CreateResponse response = new CreateResponse();
        request.setData(data);
        request.setFlags(createMode.toFlag());
        request.setPath(serverPath);
        request.setAcl(acl);
        //调用客户端ClientCnxn提交请求：TODO
        ReplyHeader r = cnxn.submitRequest(h, request, response, null);
        if (r.getErr() != 0) {
            throw KeeperException.create(KeeperException.Code.get(r.getErr()),
                    clientPath);
        }
        if (cnxn.chrootPath == null) {
            return response.getPath();
        } else {
            return response.getPath().substring(cnxn.chrootPath.length());
        }
    }
    // 2.获取节点
    public byte[] getData(String path, boolean watch, Stat stat)
            throws KeeperException, InterruptedException {
        //watch true:将watcher留在节点上（没有报错的情况下）
        return getData(path, watch ? watchManager.defaultWatcher : null, stat);
    }
    public byte[] getData(final String path, Watcher watcher, Stat stat)
        throws KeeperException, InterruptedException
     {
        final String clientPath = path;
        PathUtils.validatePath(clientPath);
        // the watch contains the un-chroot path
        WatchRegistration wcb = null;
        if (watcher != null) {
            wcb = new DataWatchRegistration(watcher, clientPath);
        }
        final String serverPath = prependChroot(clientPath);
        RequestHeader h = new RequestHeader();
        //设置请求类型
        h.setType(ZooDefs.OpCode.getData);
        GetDataRequest request = new GetDataRequest();
        request.setPath(serverPath);
        request.setWatch(watcher != null);
        GetDataResponse response = new GetDataResponse();
        ReplyHeader r = cnxn.submitRequest(h, request, response, wcb);
        if (r.getErr() != 0) {
            throw KeeperException.create(KeeperException.Code.get(r.getErr()),
                    clientPath);
        }
        //将服务端返回的状态信息赋值到stat上 ？？？？有什么用
        if (stat != null) {
            DataTree.copyStat(response.getStat(), stat);
        }
        return response.getData();
    }
    //将watcher注册到某节点路径上
    public abstract class WatchRegistration {
        private Watcher watcher;
        private String clientPath;
    }
    // 3.获取路径下的子节点 和getData大致相同
    ....
}
```

