## Redis基本数据结构

#### String字符串

Redis的字符串是动态字符串可修改，结构类似于ArrayList预先分配内存，当字符串长度小于1M，扩容是加倍现有空间，大于1M，一次扩容1M，字符串长度最大512M，创建时len和capacity一样，不会有多余的空间

- 操作：
  - （单个增加）`set key value` 或 （单个获取）`get key value` 
  - （批量增加）`mset key1 value1 key2 value2....`或（单个获取） `mget key1 key2...`
  
- 计数：value为整数，可进行自增incr操作，最大值为signed long

- 字符串内部结构实现SDS-Simple Dynamic String（

  ```c
  //Redis Object对象头结构体 16字节 + 3字节
  struct RedisObject {
      int4 type; // 4bits 对象类型
      int4 encoding; // 4bits 存储形式
      int24 lru; // 24bits LRU信息
      int32 refcount; // 4bytes 引用计数，为0时，对象销毁
      void *ptr; // 8bytes，64-bit system 指针指向对象内容body的具体存储位置
  } robj;
  //SDS结构体
  struct SDS<T> { 
      T capacity; // 数组容量 （字符串较短时可以使用byte/short表示，减少内存使用）
      T len; // 数组长度 （同上）
      byte flags; // 特殊标识位，不理睬它 
      byte[] content; // 数组内容 
  }
  ```

  - append操作

    redis的字符串是支持append操作的，如果当前没有可用空间，就会重新分配数组，并将旧内容复制过来，再进行append，重新设置追加后的长度值，最后再为字符串以`\0`结尾（很少有append操作）

  - 存储方式

    - emstr：长度较短时，使用malloc方法一次性分配RedisObject对象头和SDS对象连续存放
    - row：长度超过44时，使用malloc方法两次分配RedisObject对象头和SDS对象，两个对象头在内存上不连续

    > embstr只能是44是因为，为了容纳一个完整的emstr对象，jemalloc（内存分配器）最少分配32字节空间，再大一点是64字节，当内存是64字节时，这个字符串的最大长度就是64-19=45字节，而SDS结构体中的content为了便于直接使用glibc的字符串处理函数即方便调试打印而以字节\0结尾的字符串，即45-1=44字节


#### list列表

Redis的list是链表结构，类似于LinkedList，所以其插入和删除的速度很快，时间复杂度为o(1)，但查询的时间复杂度为o(n)，当list中最后一个元素被删除，整个数据结构删除，内存回收。

但实际上列表在元素较少时会使用一块连续的内存存储，这时结构是ziplist压缩列表

当元素较多时结构改为快速列表quicklist，即`ziplist+linkedlist`实现快速插入/删除，因此减少了碎片空间和前后指针空间

- 操作：
  - 队列（右进左出）：(添加)`rpush keyList value1 value2 value3` 或 (长度)`llen keyList` 或 (弹出)`lpop keyList`
  - 栈（右进右出）：(添加)rpush keyList value1 value2 value3 或 (长度)llen keyList 或 (弹出)rpop keyList
  - lindex：获取某索引位置的值，需要遍历列表
  - lrange：获取所有元素
  - lretain：获取start_index到end_index之间的值，其他值被清除，如lretain keyList 1 0为真~清空列表
- 异步队列：将任务结构序列化为字符串存入Redis列表

#### hash字典

Redis的hash结构类似于HashMap，即数组+链表，是一个**无序字典**，但它的key只能是字符串类型。最后一个元素被删除时，整个数据结构删除，内存回收。

- rehash：为了提高性能，与HashMap的rehash需要全部重新hash不同，Redis的hash在rehash时采用渐进式策略：rehash时保留新旧两个hash结构，查询时同时查询两个hash结构，在后续的定时任务和hash指令中，渐渐迁移老的hash内容到新的hash内容，这样可以减少全部元素hash时带来的服务堵塞。
- 操作
  - 单独存储某个key下的子内容：（为hash结构的keyHash存储子key和value）hset keyHash key1 value1 ,hset keyHash key2 value2 
  - 获取hash的entries内容：hgetall keyHash
  - 长度：hlen keyHash
  - 单独获取hash的某个key：hget keyHash key1
  - 批量增加hash内容：hmset keyHash key1 value1 key2 value2 
  - 对子key计数：hincrby
- 缺点：hash结构存储消耗高于单个字符

#### set集合

Redis的set相当于HashSet，无序且唯一，相当于所有value为null的字典。set中最后一个元素被删除时，整个数据结构删除，内存回收。（hash是子key-子value形式，set是子key）

- 操作
  - 添加：sadd keySet value1 value2
  - 查找是否存在：sismember keySet value1
  - 获取keySet的长度：scard keySet
  - 弹出：spop keySet

#### zset有序列表

Redis的有序列表zset，类似SortedSet和HashMap的结合体。它整体是个set，有set的特性，但不同于set，它可以为keySet的每个value设置一个score（任意数值），代表其排序权重，内部由 跳跃列表 数据结构实现。zset中最后一个元素被删除时，整个数据结构删除，内存回收

- 操作
  - 添加：zadd keyZset score value1
  - 按score排序列出：zrange keySet 0 -1
  - 按socre逆序列出：zrevrange keySet 0 -1
  - 个数：zcard keyZset
  - 获取指定value的score：zscore keyZset value1
  - 根据score分区列出：zrangebyscore keyZset startScore endScore
  - 删除value1：zrem keyZset value1
- 跳跃列表：实现内部score排序，保证随机的快速插入和删除；因为链表在二分查找时不支持，只有数组支持，但数组不支持快速添加和删除，所以不使用二分查找定位。

#### 特性

##### 针对list/set/zset/hash

- 新增元素时如果容器不存在则新建一个
- 删除最后一个元素时，删除整个数据结构，回收内存空间

##### 针对所有结构

- 几秒后key值失效：setnx key seconds value 或 expire key seconds
- 过期是整个对象结构过期而不是内部单个元素
- 如果某字符串已设置过期时间，再次调用set方法修改这个元素的值时，过期时间会消失
