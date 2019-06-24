## Zookeeper(六)服务器



zkServer.cmd中声明 首先启动QuorumPeerMain

```
set ZOOMAIN=org.apache.zookeeper.server.quorum.QuorumPeerMain
```



### 服务器启动

#### 预启动--QuorumPeerMain

1. 解析配置文件zoo.cfg：配置运行时的基本参数tickTime、dataDir、clientPort
2. 创建并启动历史文件清除器：对事务日志及快照数据文件定时清理
3. 如果是集群情况下，再次解析配置文件
4. 单机情况下，创建服务器实例

```java
public class QuorumPeerMain {
    //args：配置文件zoo.cfg的路径
    protected void initializeAndRun(String[] args)
        throws ConfigException, IOException, AdminServerException {
        QuorumPeerConfig config = new QuorumPeerConfig();
        if (args.length == 1) {
            //1. 解析配置文件：配置运行时的基本参数tickTime、dataDir、clientPort
            config.parse(args[0]);
        }
        //2.创建并启动历史文件清除器：对事务日志及快照数据文件定时清理
        DatadirCleanupManager purgeMgr = new DatadirCleanupManager(config
                .getDataDir(), config.getDataLogDir(), config
                .getSnapRetainCount(), config.getPurgeInterval());
        purgeMgr.start();
		//3. 如果是集群情况下 再次解析配置文件
        if (args.length == 1 && config.isDistributed()) {
            runFromConfig(config);
        } else {
            //3.2. 单机情况下 standalone 创建服务器实例
            LOG.warn("Either no config or no quorum defined in config, running "
                    + " in standalone mode");
            ZooKeeperServerMain.main(args);
        }
    }
}
```



#### 初始化--ZooKeeperServerMain

```java
public class ZooKeeperServerMain {
    protected void initializeAndRun(String[] args)
        throws ConfigException, IOException, AdminServerException{
        cnxnFactory = ServerCnxnFactory.createFactory();
        cnxnFactory.startup(zkServer);
    }
}
```



##### 单机模式

1. 初始化数据管理器FileTxnSnapLog：根据配置解析出快照文件路径和数据文件路径
2. 初始化服务器ZooKeeperServer：根据配置的tickTime，minSessionTimeout等
3. 通过系统属性确定ServerCnxnFactory的继承实现类是NIOServerCnxnFactory还是NettyServerCnxnFactory
4. 创建ServerCnxnFactory：配置客户端连接端口，最大连接数，最大挂起连接数
5. 启动
   - 我
   - 设置内部数据库，恢复会话和数据
   - 创建并开启会话管理sessionTracker
   - 初始化请求处理链，处理器以责任链方式链接，并启动顺序处理请求的线程
   - 注册JMX：Zookeeper会将服务器运行时的一些信息以JMX的方式暴露给外部。
   - 设置服务器状态为运行中

```java
public class NettyServerCnxnFactory {
    public void startup(ZooKeeperServer zks, boolean startServer)
            throws IOException, InterruptedException {
        start();
        setZooKeeperServer(zks);
        if (startServer) {
            zks.startdata();
            zks.startup();
        }
    }
}
```



ZooKeeperServer

```java
public class ZooKeeperServer implements SessionExpirer, ServerStats.Provider {
	//会话跟踪
    protected SessionTracker sessionTracker;
    //数据管理器：管理事务日志文件和快照数据文件
    private FileTxnSnapLog txnLogFactory = null;
    //zk的内部数据库
    private ZKDatabase zkDb;
    private ResponseCache readResponseCache;
    //对客户端请求的处理
    protected RequestProcessor firstProcessor;
    //服务器状态
    protected volatile State state = State.INITIAL;
    
    public synchronized void startup() {
        if (sessionTracker == null) {
            createSessionTracker();
        }
        startSessionTracker();
        setupRequestProcessors();

        registerJMX();

        setState(State.RUNNING);
        notifyAll();
    }
}
```



##### 集群模式

1. 创建ServerCnxnFactory。
  2. 初始化ServerCnxnFactory。
  3. 创建QuorumPeer实例。
  4. 初始化initialize
     - authServer
     - authLearner
5. 启动start
   - 设置内部数据库
   - 启动ServerCnxnFactory
   - 启动admin Server
   - 开启选举线程

```java
public class QuorumPeerMain {
    public void runFromConfig(QuorumPeerConfig config)
        throws IOException, AdminServerException {
        //。。。。省略quorumPeer属性赋值
        quorumPeer.initialize();
        quorumPeer.start();
        quorumPeer.join();
    }
}
```

quorumPeer相当于裁判，Quorum是集群模式下特有的对象，是Zookeeper服务器实例（ZooKeeperServer）的托管者，QuorumPeer代表了集群中的一台机器，在运行期间，QuorumPeer会不断检测当前服务器实例的运行状态，同时根据情况发起Leader选举。

```java
public class QuorumPeer{
    public void initialize() throws SaslException {
        // init quorum auth server & learner
        if (isQuorumSaslAuthEnabled()) {
            Set<String> authzHosts = new HashSet<String>();
            for (QuorumServer qs : getView().values()) {
                authzHosts.add(qs.hostname);
            }
            authServer = new SaslQuorumAuthServer(isQuorumServerSaslAuthRequired(),
                    quorumServerLoginContext, authzHosts);
            authLearner = new SaslQuorumAuthLearner(isQuorumLearnerSaslAuthRequired(),
                    quorumServicePrincipal, quorumLearnerLoginContext);
        } else {
            authServer = new NullQuorumAuthServer();
            authLearner = new NullQuorumAuthLearner();
        }
    }
    public synchronized void start() {
        if (!getView().containsKey(myid)) {
            throw new RuntimeException("My id " + myid + " not in the peer list");
         }
        loadDataBase();
        startServerCnxnFactory();
        try {
            adminServer.start();
        } catch (AdminServerException e) {
            LOG.warn("Problem starting AdminServer", e);
            System.out.println(e);
        }
        startLeaderElection();
        super.start();
    }
}
```
