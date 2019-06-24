# Zookeeper-客户端

### 例子：

```java
// org.apache.zookeeper.ZooKeeperMain
public class ZooKeeperMain {
    public static void main(String args[]) throws CliException, IOException, InterruptedException {
    	//1. 初始化zk配置，并建立连接
        ZooKeeperMain main = new ZooKeeperMain(args);
        //2. 一直等待控制台读入命令行 并执行
        main.run();
    }
	public ZooKeeperMain(String args[]) throws IOException, InterruptedException {
        //1.1 连接配置解析
        cl.parseOptions(args);
        System.out.println("Connecting to " + cl.getOption("server"));
        connectToZK(cl.getOption("server"));
    }
    //1.2 建立连接
    protected void connectToZK(String newHost) throws InterruptedException, IOException {
        //连接已经存在 关闭连接 重新创建
        if (zk != null && zk.getState().isAlive()) {
            zk.close();
        }
        host = newHost;
        boolean readOnly = cl.getOption("readonly") != null;
        if (cl.getOption("secure") != null) {
            System.setProperty(ZKClientConfig.SECURE_CLIENT, "true");
            System.out.println("Secure connection is enabled");
        }
        zk = new ZooKeeperAdmin(host, Integer.parseInt(cl.getOption("timeout")), new MyWatcher(), readOnly);
    }
    //1.3 自定义监听器
    private class MyWatcher implements Watcher {
        public void process(WatchedEvent event) {
            if (getPrintWatches()) {
                ZooKeeperMain.printMessage("WATCHER::");
                ZooKeeperMain.printMessage(event.toString());
            }
        }
    }
}
```



### 问题

1. zk怎么体现最终一致性：
2. zk的监控在客户端和服务端的连接过程中起到什么作用：节点更新，服务端通知客户端，客户端调用回调方法处理
3. zk对节点的原子操作是怎么体现的：版本控制，节点内部维护三种版本
4. 客户端与服务端连接会话中的各个状态下 客户端处理什么样的事情



### 基本功能

1. 以树形结构存储数据，叶子节点可以存储数据
   - 文件系统
   - 配置管理
   - 命名服务
2. 当某个节点的子节点变更，连接在这个节点的client可以实时监听到变化
   - 集群管理
3. client对节点操作时，是原子操作
   - 远程锁：分布式锁



### 基本术语

- States客户端状态：

```java
public enum States {
    CONNECTING, ASSOCIATING, CONNECTED, CONNECTEDREADONLY,
    CLOSED, AUTH_FAILED, NOT_CONNECTED;
    public boolean isAlive() {
        return this != CLOSED && this != AUTH_FAILED;
    }
    public boolean isConnected() {
        return this == CONNECTED || this == CONNECTEDREADONLY;
    }
}
```

- Packet传给服务端的数据包

```java
//ClientCnxn内部定义的一个堆协议层的封装，用作zk中请求和响应的载体；
static class Packet {
    
}
```

- ClientCnxnSocket底层与服务端通信类

```java
//真正与服务端连接的抽象类；有两个子类分别使用jdk.nio/netty.nio实现会话操作
abstract class ClientCnxnSocket {
    abstract void connect(InetSocketAddress addr) throws IOException;
    //会从outgoingQueue中取出一个可发送的Packet对象，
    //同时生成一个客户端请求序号XID并将其设置到Packet请求头中去，
    //然后序列化后再发送，请求发送完毕后，会立即将该Packet保存到pendingQueue中，
    //以便等待服务端响应返回后进行相应的处理。
    abstract void doTransport(int waitTimeOut, List<Packet> pendingQueue,
            ClientCnxn cnxn) throws IOException, InterruptedException;
}
```

- WatchedEvent监控事件

```java
//包含发生的事件，zookeeper当前状态信息，事件涉及的节点路径
public class WatchedEvent {
    final private KeeperState keeperState;
    final private EventType eventType;
    private String path;
}
```

- Watcher：事件处理类的基本父类
- KeeperState：Event事件中Zookeeper可能存在的所有状态
- EventType：Zookeeper中各种Event类型
- WatcherType：

```java
//内部包含两个类Event，WatchType
public interface Watcher {
    
}
```

#### 与服务端建立连接

##### ZooKeeperAdmin

```java
//主要用于集群的管理任务，如重配置集群成员；
@InterfaceAudience.Public
public class ZooKeeperAdmin extends ZooKeeper {
	//ZooKeeperAdmin构造器最终调用父类构造器
	public ZooKeeperAdmin(String connectString, int sessionTimeout, Watcher watcher, boolean canBeReadOnly) throws IOException {
        super(connectString, sessionTimeout, watcher, canBeReadOnly);
    }
}
```

##### Zookeeper：客户端

```java
//功能：1.初始化服务客户端连接服务端 1).创建客户端对象 2).启动客户端内部线程
//	   2.提供操作数据功能 	      1).向服务端发送请求
@InterfaceAudience.Public
public class ZooKeeper implements AutoCloseable {
	protected final ClientCnxn cnxn;
    public ZooKeeper(String connectString, int sessionTimeout, Watcher watcher,
            boolean canBeReadOnly, HostProvider aHostProvider)
            throws IOException {
        this(connectString, sessionTimeout, watcher, canBeReadOnly,
                aHostProvider, null);
    }
    public ZooKeeper(String connectString, int sessionTimeout, Watcher watcher,
            boolean canBeReadOnly, HostProvider aHostProvider,
            ZKClientConfig clientConfig) throws IOException {
        LOG.info("Initiating client connection, connectString=" + connectString
                + " sessionTimeout=" + sessionTimeout + " watcher=" + watcher);
        if (clientConfig == null) {
            clientConfig = new ZKClientConfig();
        }
        this.clientConfig = clientConfig;
        watchManager = defaultWatchManager();
        watchManager.defaultWatcher = watcher;
        //解析连接ip:port
        ConnectStringParser connectStringParser = new ConnectStringParser(
                connectString);
        hostProvider = aHostProvider;
        //1. 创建管理连接的客户端 ChrootPath为客户端自定义的路径头
        cnxn = createConnection(connectStringParser.getChrootPath(),
                hostProvider, sessionTimeout, this, watchManager,
                getClientCnxnSocket(), canBeReadOnly);
        //2. 启动客户端内部线程        
        cnxn.start();
    }
    protected ClientCnxn createConnection(String chrootPath,
            HostProvider hostProvider, int sessionTimeout, ZooKeeper zooKeeper,
            ClientWatchManager watcher, ClientCnxnSocket clientCnxnSocket,
            boolean canBeReadOnly) throws IOException {
        return new ClientCnxn(chrootPath, hostProvider, sessionTimeout, this,
                watchManager, clientCnxnSocket, canBeReadOnly);
    }
}
```

##### ClientCnxn

```java
//维护服务端和客户端之间的网络连接，并进行一系列的网络通信：维护一个可用服务器的列表，当某客户端需要时可透明的切换服务
public class ClientCnxn {
    final SendThread sendThread;
    final EventThread eventThread;
    //客户端可以连接的服务端地址集合
    private final HostProvider hostProvider;
    //需要发送给服务端的数据包：最终通过SendThread调用clientCnxnSocket.doTransport发送给服务端
    private final LinkedBlockingDeque<Packet> outgoingQueue = new LinkedBlockingDeque<Packet>();
    //已经发送给服务端但还未得到响应的数据包集合
    private final LinkedList<Packet> pendingQueue = new LinkedList<Packet>();
    
    public ClientCnxn(String chrootPath, HostProvider hostProvider, int sessionTimeout, ZooKeeper zooKeeper, ClientWatchManager watcher, ClientCnxnSocket clientCnxnSocket,
            long sessionId, byte[] sessionPasswd, boolean canBeReadOnly) {
        this.zooKeeper = zooKeeper;
        this.watcher = watcher;
        this.sessionId = sessionId;
        this.sessionPasswd = sessionPasswd;
        this.sessionTimeout = sessionTimeout;
        this.hostProvider = hostProvider;
        this.chrootPath = chrootPath;
        connectTimeout = sessionTimeout / hostProvider.size();
        readTimeout = sessionTimeout * 2 / 3;
        readOnly = canBeReadOnly;
        sendThread = new SendThread(clientCnxnSocket);                                           eventThread = new EventThread();
        this.clientConfig = zooKeeper.getClientConfig();
        initRequestTimeout();
    }
    public void start() {
        // 0.建立连接会话 1.sasl验证 startConnect
        // 2.创建监听事件到事件队列中 3.保持心跳
        sendThread.start();
        // 处理事件队列中的事件
        eventThread.start();
    }
}
```

###### SendThread

1. 维护了客户端与服务端之间的会话生命周期（通过一定周期频率内向服务端发送PING包检测心跳），如果会话周期内客户端与服务端出现TCP连接断开，那么就会自动且透明地完成重连操作。

　2. 管理了客户端所有的请求发送和响应接收操作，其将上层客户端API操作转换成相应的请求协议并发送到服务端，并完成对同步调用的返回和异步调用的回调。

　3. 将来自服务端的事件传递给EventThread去处理。

```java
 //ClientCnxn内部类：为传出请求队列服务并生成心跳
class SendThread extends ZooKeeperThread {
    private final ClientCnxnSocket clientCnxnSocket;
    private InetSocketAddress rwServerAddress = null;
    SendThread(ClientCnxnSocket clientCnxnSocket) {
        super(makeThreadName("-SendThread()"));
        state = States.CONNECTING;
        this.clientCnxnSocket = clientCnxnSocket;
        setDaemon(true);
    }
    @Override
    public void run() {
        //赋值
        clientCnxnSocket.introduce(this, sessionId, outgoingQueue);
        //更新时间
        clientCnxnSocket.updateNow();
        //更新上一次发送和接收的时间
        clientCnxnSocket.updateLastSendAndHeard();
        int to;
        long lastPingRwServer = Time.currentElapsedTime();
        //设置最大心跳ping间隔 10s
        final int MAX_SEND_PING_INTERVAL = 10000; //10 seconds
        InetSocketAddress serverAddress = null;
        while (state.isAlive()) {
            try {
                //一开始 客户端还没连接上服务端 尝试初始化sasl认证并且连接服务端
                if (!clientCnxnSocket.isConnected()) {
                    // 与服务端连接断开时 不再重建会话 直接跳出循环
                    if (closing) {
                        break;
                    }
                    if (rwServerAddress != null) {
                        serverAddress = rwServerAddress;
                        rwServerAddress = null;
                    } else {
                        //从服务端可连接集合中获取下一个地址 如果全部尝试过 则等待1s
                        serverAddress = hostProvider.next(1000);
                    }
                    //clientCnxnSocket作为底层与服务端通信的类
                    startConnect(serverAddress);
                    clientCnxnSocket.updateLastSendAndHeard();
                }
                //后来连接上了 说明认证也已经初始化好了 通过发送认证包给服务建立验证
                if (state.isConnected()) {
                    // 确认是否需要发送认证失败事件
                    if (zooKeeperSaslClient != null) {
                        boolean sendAuthEvent = false;
                        if (zooKeeperSaslClient.getSaslState() == ZooKeeperSaslClient.SaslState.INITIAL) {
                            try {
                                //向服务端发送当前客户端sasl认证初始化请求
                                zooKeeperSaslClient.initialize(ClientCnxn.this);
                            } catch (SaslException e) {
                                LOG.error("SASL authentication with Zookeeper Quorum member failed: " + e);
                                state = States.AUTH_FAILED;
                                sendAuthEvent = true;
                            }
                        }
                        //获得zk的sasl认证状态
                        KeeperState authState = zooKeeperSaslClient.getKeeperState();
                        if (authState != null) {
                            if (authState == KeeperState.AuthFailed) {
                                //与服务端进行身份验证时发生错误 状态更改且需要发送认证事件
                                state = States.AUTH_FAILED;
                                sendAuthEvent = true;
                            } else {
                                //验证通过
                                if (authState == KeeperState.SaslAuthenticated) {
                                    sendAuthEvent = true;
                                }
                            }
                        }
                        //是否需要发送认证事件
                        if (sendAuthEvent) {
                            // 生成相应的事件 并放入事件队列中
                            eventThread.queueEvent(new WatchedEvent(
                                Watcher.Event.EventType.None,
                                authState,null));
                            if (state == States.AUTH_FAILED) {
                                eventThread.queueEventOfDeath();
                            }
                        }
                    }
                    to = readTimeout - clientCnxnSocket.getIdleRecv();
                } else {
                    to = connectTimeout - clientCnxnSocket.getIdleRecv();
                }
                if (to <= 0) {
                    String warnInfo;
                    warnInfo = "Client session timed out, have not heard from server in "
                        + clientCnxnSocket.getIdleRecv()
                        + "ms"
                        + " for sessionid 0x"
                        + Long.toHexString(sessionId);
                    LOG.warn(warnInfo);
                    throw new SessionTimeoutException(warnInfo);
                }
                if (state.isConnected()) {
                    //1000(1 second) is to prevent race condition missing to send the second ping
                    //also make sure not to send too many pings when readTimeout is small
                    //防止丢失 所有有下一次ping
                    int timeToNextPing = readTimeout / 2 - clientCnxnSocket.getIdleSend() 			- ((clientCnxnSocket.getIdleSend() > 1000) ? 1000 : 0);
                    //send a ping request either time is due or no packet sent out within MAX_SEND_PING_INTERVAL
                    if (timeToNextPing <= 0 || clientCnxnSocket.getIdleSend() > MAX_SEND_PING_INTERVAL) {
                        //保持和服务端的心跳 发送ping
                        sendPing();
                        clientCnxnSocket.updateLastSend();
                    } else {
                        if (timeToNextPing < to) {
                            to = timeToNextPing;
                        }
                    }
                }
                // 如果当前是读写模式 则寻找读写服务器 todo
                if (state == States.CONNECTEDREADONLY) {
                    long now = Time.currentElapsedTime();
                    int idlePingRwServer = (int) (now - lastPingRwServer);
                    if (idlePingRwServer >= pingRwTimeout) {
                        lastPingRwServer = now;
                        idlePingRwServer = 0;
                        pingRwTimeout =
                            Math.min(2*pingRwTimeout, maxPingRwTimeout);
                        pingRwServer();
                    }
                    to = Math.min(to, pingRwTimeout - idlePingRwServer);
                }
				//确保所有前提都满足 
                //取出等待队列的头部发送给服务端并从队列中移除 并将其保存到pendingQueue中
                clientCnxnSocket.doTransport(to, pendingQueue, ClientCnxn.this);
            } catch (Throwable e) {
                if (closing) {
                    if (LOG.isDebugEnabled()) {
                        // closing so this is expected
                        LOG.debug("An exception was thrown while closing send thread for session 0x" + Long.toHexString(getSessionId())  + " : " + e.getMessage());
                    }
                    break;
                } else {
                   //。。。。一堆抛出错误
                   //根据连接状态处理当前仍旧往队列中投放的事件
                   cleanAndNotifyState();
                }
            }
        }
        synchronized (state) {
            //清除当前队列中所有等待的事件 不做处理
            cleanup();
        }
       	//当连接失效 主动关闭和服务端的连接
        clientCnxnSocket.close();
        if (state.isAlive()) {
            eventThread.queueEvent(new WatchedEvent(Event.EventType.None,
                          Event.KeeperState.Disconnected, null));
        }
        eventThread.queueEvent(new WatchedEvent(Event.EventType.None,
                                                Event.KeeperState.Closed, null));
        ZooTrace.logTraceMessage(LOG, ZooTrace.getTextTraceLevel(),
          "SendThread exited loop for session: 0x"  + Long.toHexString(getSessionId()));
    }
    //发送心跳
    private void sendPing() {
        lastPingSentNs = System.nanoTime();
        RequestHeader h = new RequestHeader(-2, OpCode.ping);
        queuePacket(h, null, null, null, null, null, null, null, null);
    }
    //和服务端创建连接会话
    private void startConnect(InetSocketAddress addr) throws IOException {
        saslLoginFailed = false;
        //如果之前连接过 则缓1s
        if(!isFirstConnect){
            try {
                Thread.sleep(r.nextInt(1000));
            } catch (InterruptedException e) {
                LOG.warn("Unexpected exception", e);
            }
        }
        //连接状态改为正在连接
        state = States.CONNECTING;
        String hostPort = addr.getHostString() + ":" + addr.getPort();
        MDC.put("myid", hostPort);
        //为当前线程设置线程名称
        setName(getName().replaceAll("\\(.*\\)", "(" + hostPort + ")"));
        //客户端连接是否需要认证 Y:新建认证 如果认证过 断开重新认证
        if (clientConfig.isSaslClientEnabled()) {
            try {
                if (zooKeeperSaslClient != null) {
                    zooKeeperSaslClient.shutdown();
                }
                //初始化客户端sasl验证 sasl状态为初始化initial
                zooKeeperSaslClient = new ZooKeeperSaslClient(SaslServerPrincipal.getServerPrincipal(addr, clientConfig),
                                                              clientConfig);
            } catch (LoginException e) {
                //在SASL客户端初始化的过程中认证失败了，与和zk服务端连接过程出现的认证失败不同
                LOG.warn("SASL configuration failed: " + e + " Will continue connection to Zookeeper server without " + "SASL authentication, if Zookeeper server allows it.");
                //为当前认证失败创建新的监控任务
                eventThread.queueEvent(new WatchedEvent(
                    Watcher.Event.EventType.None,
                    Watcher.Event.KeeperState.AuthFailed, null));
                saslLoginFailed = true;
            }
        }
        logStartConnect(addr);
        //与服务端通信
        clientCnxnSocket.connect(addr);
    }
}
```

###### EventThread

1. 负责客户端的事件处理，并触发客户端注册的Watcher监听。

2. EventThread中的watingEvents队列用于临时存放那些需要被触发的Object，包括客户端注册的Watcher和异步接口中注册的回调器AsyncCallback。

3. 同时，EventThread会不断地从watingEvents中取出Object，识别具体类型（Watcher或AsyncCallback），并分别调用process和processResult接口方法来实现对事件的触发和回调。

```java
//ClientCnxn内部类：无限处理等待队列中的监听事务
class EventThread extends ZooKeeperThread {
    //等待处理的事件队列
    private final LinkedBlockingQueue<Object> waitingEvents =
    new LinkedBlockingQueue<Object>();
    @Override
    @SuppressFBWarnings("JLM_JSR166_UTILCONCURRENT_MONITORENTER")
    public void run() {
        try {
              isRunning = true;
              while (true) {
                 Object event = waitingEvents.take();
                  //eventOfDeath代表出现了身份认证失败 
                 if (event == eventOfDeath) {
                    wasKilled = true;
                 } else {
                     //核心处理
                    processEvent(event);
                 }
                 if (wasKilled)
                    synchronized (waitingEvents) {
                       if (waitingEvents.isEmpty()) {
                          isRunning = false;
                          break;
                       }
                    }
              }
           }//省略日志代码
    }
}
```

由上述事件处理线程的run方法得出问题：

1. 添加事件：队列中的事件waitingEvents是从哪里添加的

   EventThread内部的queueEvent，queueCallback，queuePacket，queueEventOfDeath

```java
//客户端访问服务端时使用：如操作节点数据等
public void queueEvent(WatchedEvent event) {
    queueEvent(event, null);
}
private void queueEvent(WatchedEvent event, Set<Watcher> materializedWatchers) {
    if (event.getType() == EventType.None && sessionState == event.getState()) {
        return;
    }
    sessionState = event.getState();
    final Set<Watcher> watchers;
    if (materializedWatchers == null) {
        // 根据事件信息生成一系列的观察者：由zk实现
        watchers = watcher.materialize(event.getState(),
                event.getType(), event.getPath());
    } else {
        watchers = new HashSet<Watcher>();
        watchers.addAll(materializedWatchers);
    }
    //将watcher集合和对应的事件组装 执行处理时 循环watchers处理
    WatcherSetEventPair pair = new WatcherSetEventPair(watchers, event);
    waitingEvents.add(pair);
}
//添加异步回调事件：TODO 什么情况下会用到
public void queueCallback(AsyncCallback cb, int rc, String path, Object ctx) {
    waitingEvents.add(new LocalCallback(cb, rc, path, ctx));
}
//客户端连接出错等情况下使用 TODO
@SuppressFBWarnings("JLM_JSR166_UTILCONCURRENT_MONITORENTER")
public void queuePacket(Packet packet) {
    if (wasKilled) {
        synchronized (waitingEvents) {
            if (isRunning) waitingEvents.add(packet);
            else processEvent(packet);
        }
    } else {
        waitingEvents.add(packet);
    }
}
```

2. 处理事件：processEvent(event)

   事件类型有三种：WatcherSetEventPair，LocalCallback

   核心就是调用watcher处理

```java
WatcherSetEventPair pair = (WatcherSetEventPair) event;
for (Watcher watcher : pair.watchers) {
     watcher.process(pair.event);
}
```











