## Redis基本数据结构（二）

#### set集合

Redis的set相当于HashSet，无序且唯一，相当于所有value为null的字典。set中最后一个元素被删除时，整个数据结构删除，内存回收。（hash是子key-子value形式，set是子key）

- 操作
  - 添加：sadd keySet value1 value2
  - 查找是否存在：sismember keySet value1
  - 获取keySet的长度：scard keySet
  - 弹出：spop keySet

#### zset有序列表

Redis的有序列表zset，类似SortedSet和HashMap的结合体。它整体是个set，有set的特性，但不同于set，它可以为keySet的每个value设置一个score（任意数值），代表其排序权重，内部由 跳跃列表（分值排序） 和字典（o(1)查询key值）实现。zset中最后一个元素被删除时，整个数据结构删除，内存回收；

- 操作

  - 添加：zadd keyZset score value1
  - 按score排序列出：zrange keySet 0 -1
  - 按socre逆序列出：zrevrange keySet 0 -1
  - 个数：zcard keyZset
  - 获取指定value的score：zscore keyZset value1
  - 根据score分区列出：zrangebyscore keyZset startScore endScore
  - 删除value1：zrem keyZset value1

- 跳跃列表：实现内部score排序，保证随机的快速插入和删除；因为链表在二分查找时不支持，只有数组支持，但数组不支持快速添加和删除，所以不使用二分查找定位。其实和索引的目录思想是差不多的。适用于存储字符串长度较长，整数较大的的对象值

  - ```c
    //跳跃表节点：一个成员节点维护了在它之上的所有层，按照score升序排列
    typedef struct zskiplistNode {
        sds ele;					//成员对象 一个字符串对象，需唯一
        double score;		//节点保存的分值，在跳跃表中按照score升序排列，可重复，分						//值相同时按照对象大小排序
        struct zskiplistNode *backward;
        struct zskiplistLevel {
            struct zskiplistNode *forward;//前进指针：指向下个同层节点
            unsigned long span;		//前进指针指向节点距当前节点的跨度
        } level[];					//层节点数组（一个元素可能有很多层）
    } zskiplistNode;
    //跳跃表
    typedef struct zskiplist {
        struct zskiplistNode *header, *tail;//指向头尾节点
        unsigned long length;		//除表头节点外，目前包含的节点数量
        int level;					//除表头节点层数外，层数最大的那个节点的层数1-32
    } zskiplist;
    ```

  - 遍历过程：根据前进指针，跨度为1，一直往后走，就可以得到所有的节点

  - 应用：有序集合键，集群节点内部数据结构







#### 特性

##### 针对list/set/zset/hash

- 新增元素时如果容器不存在则新建一个
- 删除最后一个元素时，删除整个数据结构，回收内存空间

##### 针对所有结构

- 几秒后key值失效：setnx key seconds value 或 expire key seconds
- 过期是整个对象结构过期而不是内部单个元素
- 如果某字符串已设置过期时间，再次调用set方法修改这个元素的值时，过期时间会消失