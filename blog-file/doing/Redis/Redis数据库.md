## Redis数据库

Redis服务器将数据库存储在redisServer的db数组中

```c
struct redisServer{
    //数据库数组
	redisDb *db;
    //数据库数量：决定服务器启动时，创建数据库数量
    int dbnum；
}
```

Redis客户端一个时间只有一个目标数据库，使用`select 索引`切换

```c
typedef struct redisClient{
    //指向服务器中不同的数据库（select调用实现切换）
    redisDb *db;
}redisClient
```

### 数据库键空间

数据库结构redisDb，内部维护一个字典来存储这个数据库的所有键值对，对每个数据库键的操作，其实都是通过键空间字典进行操作实现的。

```c
typedef struct redisDb{
    //数据库键空间，保存数据库中所有的键值对
    dict *dict;
    //过期字典，维护键的过期时间
    dict *expirse;
}redisDb;
```

#### 过期字典

调用`expire 键 时间秒/时间毫秒/时间戳`来控制键的过期时间，redisDb使用expires字典来保存数据库中所有键的过期时间，它的键是指向键空间中的某个键的，值是一个long long类型的整数，存储键（指向的数据库键）的过期时间（毫秒精度的unix时间戳）；使用persit移除过期时间。

##### 过期键删除策略

Redis实际使用**惰性删除**和**定期删除**两种策略

- 定时删除：占用大量CPU，查找过期键的复杂度为o(n)，影响服务器响应时间和吞吐量，不采用。

- *惰性删除：访问时发现应该过期才删除，大量应该删除的没有被删除，浪费内存，导致内存泄漏。

  在查询/更新等命令执行前，调用`expireIfNeeded`函数，过滤/删除已经过期的键

- *定期删除：Redis服务器会周期性调用`serverCron`里的函数，`activeExpireCyle`函数也在其中；在规定时间，分多次遍历服务器中的各个数据库，从expires字典中，随机检查部分键的过期时间，通过全局变量current_db记录当前进度。

### 数据库通知

客户端通过订阅给定的频道/模式，来获得数据库中键的变化，及数据库中命令的执行情况



