## Redis基本数据结构（一）

Redis对象可分为5种类型，字符串，链表，字典，集合，有序集合，Redis基于一些数据结构实现了这些对象。

Redis对象

```c
typedef struct redisObject{
    //类型
    unsigned type:4;
    //编码：决定对象实现的数据结构
    unsigned encoding:4;
    //指向对象的底层实现数据结构
    void *ptr;
    //引用计数：垃圾回收机制
    int refCount;
    //最后一次访问时间：可用于内存回收算法
    unsigned lru:22;
}robj;
```

编码

不同类型对象，可以有多种编码方式，使用`OBJECT ENCODING 对象键`查看

![](C:\Users\zbb\Desktop\redis-encoding.jpg)

垃圾回收

根据refCount跟踪对象引用计数，以方便垃圾回收，

共享内存

通过引用计数实现对象共享机制，使多个数据库键引用同一个对象来节约内存

#### REDIS_STRING字符串

Redis需要修改的字符串是动态字符串SDS，如果不需要修改只是显示的使用C语言自带的，结构类似于ArrayList预先分配内存，当字符串长度小于1M，扩容是加倍现有空间，大于1M，一次扩容1M，字符串长度最大512M，创建时len和capacity一样，不会有多余的空间

| type         | encoding              | ptr类型 | 值                           |
| ------------ | --------------------- | ------- | ---------------------------- |
| REDIS_STRING | REDIS_ENCODING_INT    | long    | 整数                         |
| REDIS_STRING | REDIS_ENCODING_RAW    | sdsdhr  | 字符串长度大于32/int值append |
| REDIS_STRING | REDIS_ENCODING_EMBSTR | sdshdr  | 字符串长度小等于32           |

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
      T free; // 未使用空间 （同上）
      byte flags; // 特殊标识位，不理睬它 
      byte[] content; // 字节数组内容 
  }
  ```

  - SDS用于键值对存储字符串的底层实现，AOF模块中的AOF缓冲区，客户端状态中输入输出缓冲区

  - SDS与C语言字符串的区别

    - SDS可以存储文本数据和任意格式的二进制数据（图片/音频等等），而C字符串只能存文本数据，空字符只能存在尾部，内容不能包含，因为C字符串以空字符判断结尾，而SDS以len来判断
    - SDS可以使用部分C字符串的API，这样就可以重用了，因为它遵循了C字符串结尾以空字符'\0'结束的惯例。
    - c语言字符串不记录本身长度，计数需要从头到空字符为止，复杂度为O(n)，而SDS为O(1)，如果需要反复获取字符串长度，也能很快而不给Redis造成性能瓶颈。
    - 在为字符串做拼接操作时，因C语言字符串不记录自身长度，如果忘了内存重分配，可能造成因内存不足而产生的缓冲区逸出；而SDS会在执行拼接操作之前，检查其空间是否满足，如果不满足，将会根据空间分配策略自动扩展，然后再执行修改操作。   
    - C的字符串在进行增加字符串操作时，如果忘了内存重分配来扩容，就会导致缓冲区逸出；在进行截断操作时，如果忘了内存重分配释放空间，就会导致内存泄漏。而内存重分配过程其实是一个很耗时的过程，如果频繁调用会降低系统性能。
    - SDS空间预分配：SDS拼接前检查未使用空间是否足够，如果够直接修改，如果不够需要扩展，则会进行内存重分配，会为其分配额外的未使用空间和需要的空间（len小于1 M时，两者相同，大于等于1 M时，未使用空间为1M），以减少内存重分配次数。
    - SDS惰性空间释放：SDS截断后，使用free属性将多出来的字节保存，以便下次使用，可以强制调用API进行真正的释放。

  - 存储方式

    embstr编码是一种优化的编码方式，专门用于存储短字符串，它将字符串的所有数据存储在一块连续的内存里，它和raw编码不同的是，raw编码分两次申请redisObject和sdshdr的内存空间，而embstr一次申请，redisObject和sdshdr是分配在一块连续的空间，这样embstr也只需要调用一次内存释放函数，而raw需要两次。

embstr编码的对象不具备修改功能，所以每次修改都需要将对象转换为raw编码，再执行操作，最终成为一个raw编码对象。

#### list列表

Redis的list是链表结构，类似于LinkedList，所以其插入和删除的速度很快，时间复杂度为o(1)，但查询的时间复杂度为o(n)，当list中最后一个元素被删除，整个数据结构删除，内存回收。

但实际上列表在元素较少时会使用一块连续的内存存储，这时结构是ziplist压缩列表

当元素较多时结构改为快速列表quicklist，即`ziplist+linkedlist`实现快速插入/删除，因此减少了碎片空间和前后指针空间

| type       | encoding                  | ptr类型    | 值                                          |
| ---------- | ------------------------- | ---------- | ------------------------------------------- |
| REDIS_LIST | REDIS_ENCODING_ZIPLIST    | zsl        | 所有字符串长度都小于64B&列表元素个数小于512 |
| REDIS_LIST | REDIS_ENCODING_LINKEDLIST | linkedlist | 上述任一不满足会被强转为linkedlist          |

- 操作：

  - 队列（右进左出）：(添加)`rpush keyList value1 value2 value3` 或 (长度)`llen keyList` 或 (弹出)`lpop keyList`
  - 栈（右进右出）：(添加)rpush keyList value1 value2 value3 或 (长度)llen keyList 或 (弹出)rpop keyList
  - lindex：获取某索引位置的值，需要遍历列表
  - lrange：获取所有元素
  - lretain：获取start_index到end_index之间的值，其他值被清除，如lretain keyList 1 0为真~清空列表

- 链表

  - 表示

    ```c
    typedef struct listNode {
        struct listNode *prev;//前置节点（头节点指向null）
        struct listNode *next;//后置节点（尾节点指向null）
        void *value;		  //节点值（可以保存各种类型的值）
    } listNode;
    typedef struct list {
        listNode *head;		  //头节点
        listNode *tail;		  //尾节点
        void *(*dup)(void *ptr); //节点值复制函数
        void (*free)(void *ptr); //节点值释放函数
        int (*match)(void *ptr, void *key);//节点值比较函数
        unsigned long len;	   //节点数量
    } list;
    ```

  - 应用：发布订阅，慢查询，监视器，保存多个客户端的状态信息，构建客户端输出缓冲区

#### hash字典

Redis的hash结构类似于HashMap，即数组+链表，是一个**无序字典**，但它的key只能是字符串类型。最后一个元素被删除时，整个数据结构删除，内存回收。

| type       | encoding               | ptr类型 | 值                                          |
| ---------- | ---------------------- | ------- | ------------------------------------------- |
| REDIS_HASH | REDIS_ENCODING_ZIPLIST | ziplist | 所有字符串长度都小于64B&列表元素个数小于512 |
| REDIS_HASH | REDIS_ENCODING_HT      | dict    | 上述任一不满足会被强转为hashtable           |

`REDIS_ENCODING_ZIPLIST`编码下，键值分别节点的形式紧靠的存在链表队尾，先添加的键值在链表头，后加入的在链尾。

- 操作

  - 单独存储某个key下的子内容：（为hash结构的keyHash存储子key和value）hset 哈希键1key1 value1 ,hset 哈希键1 key2 value2 
  - 获取hash的entries内容：hgetall keyHash
  - 长度：hlen keyHash
  - 单独获取hash的某个key：hget keyHash key1
  - 批量增加hash内容：hmset keyHash key1 value1 key2 value2 
  - 对子key计数：hincrby

- 缺点：hash结构存储消耗高于单个字符

- 应用：Redis底层数据库，对数据库的增删改查操作是基于对字典数据的操作

- 实现

  ```c
  //字典dict
  typedef struct dict {
      dictType *type;			//类型：为建立多态字典；dictType维护不同类型对应的函数
      void *privdata;			//私有数据
      dictht ht[2];			//两个hash表，hash[1]用于rehash，hash[0]为hash表
      long rehashidx; 		//-1时说明没有在rehash，0是开始，大于0是rehash的进度
      unsigned long iterators; /* number of iterators currently running */
  } dict;
  //哈希表dictht
  typedef struct dictht {
      dictEntry **table;		//hash表数组
      unsigned long size;		//hash表大小
      unsigned long sizemask; //hash表大小掩码，计算索引时使用
      unsigned long used;		//hash表已有节点数量
  } dictht;
  //节点dictEntry
  typedef struct dictEntry {
      void *key;				//键：唯一且多态
      union {					//值：可以是指针/整数
          void *val;
          uint64_t u64;
          int64_t s64;
          double d;
      } v;
      struct dictEntry *next;//同key下，指向下个节点，实现单向链表，解决key冲突
  } dictEntry;
  
  ```

- hash函数计算索引：MurmurHash算法计算hash值 & dictht的sizemask

- 解决键冲突：链表地址法，新节点插入到key下链表的头节点，复杂度为o(1)

- rehash：字典中键值对的增多或减少都会影响负载因子`load factor`（used/size），所以为了保持在合理范围，需要对字典进行扩容/缩容；但字典的空间重分配不会一次性做完，避免对服务器性能造成影响，会分批次做完。在rehash时才会为ht[1]分配空间，并设rehashidx为0，每做一次迁移，都会增1。

  rehash时保留新旧两个hash结构，查询时同时查询两个hash结构，在后续的定时任务和hash指令中，渐渐迁移老的hash内容到新的hash内容，这样可以减少全部元素hash时带来的服务堵塞。
