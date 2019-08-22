Watcher

1. 客户端向服务器注册watcher，同时存储watcher到本地WatcherManager

   - 客户端API传入watcher对象，
- 标记request是否包含watcher对象，封装watcher对象到watchRegistration
   - 封装watchRegistration和request等信息到packet，并放入发送队列等待发送给服务端
  - 虽然watchRegistration也被封装到packet中，但是packet序列化时只序列化了requestHeader和request，即watcher其实没有被传给服务端
   - 客户端SendThread线程的readResponse方法等待服务端响应
     - 反序列化并还原为WatchedEvent，放入轮询处理队列
  - 根据watchedEvent的事件类型/通知状态/节点路径解析，找出所有watcherEvent有关的watcher，并移除
   - 服务端接收请求，并处理watcher（本质上是借助当前客户端连接的ServerCnxn对象来实现对客户端WatchedEvent传递）
     - FinalRequestProcessor.processRequest()存储标记了watcher注册请求的ServerCnxn到watcherManager
     - 触发事件（根据watcher监听的触发条件调用对应实现方法）
       - WatcherManager.triggerWatcher触发
       - 封装WatchedEvent对象
       - 从watchTable查询并移除watcher。没有就退出
       - 调用watcher的process对象触发，本质是调用ServerCnxn：在请求头中标记-1表明这是一个通知，封装watchedEvent对象为WatcherEvent方便网络传输TODO，借助当前客户端连接的ServerCnxn来实现WatchedEvent传递给客户端发送通知
   - 响应成功：finishPacket方法从packet中取出watchRegistration对象封装的watcher，并注册到ZkWatchManager.defaultManager中
- 响应失败：请求返回
   
2. zookeeper服务器触发Watcher事件，向客户端发送通知

3. 客户端线程从WatcherManager中取出对应Watcher对象执行回调逻辑

Watcher与两个枚举类有关KeeperState和EventType，process是Watcher的一个回调方法