## undo日志

### 作用

因一些原因（机器宕机/操作系统错误/用户主动rollback等)导致事务执行到一半，但这时事务的执行已经让很多信息修改了（提交前就会边执行边修改记录），但还有部分未执行，为了保证事务的一致性与原子性，要么全都执行成功，要么全都失败，所以就需要回滚，而rollback需要旧值依据，而这些旧值记录就存储在undo日志中。

### redo日志记录

#### 记录时机

InnoDB存储引擎在实际的进行增删改操作时，每操作一条记录都会先把对应的undo日志记录下来。

#### undo日志通用结构

undo日志存储在类型为FIL_PAGE_UNDO_LOGO的页面中，而每条记录添加到数据页中时，都会隐式的生成两个列trix_id和roll_pointer，trix_id就是事务id，roll_pointer是指向记录对应的undo日志的一个指针

- end of record：本条undo日志在页中结束地址
- undo type：undo日志类型
- undo no：undo日志编号，事务没提交时，每生成一条undo日志就递增1
- table id：本条undo 日志对应记录所在table id（information_schema库中表innodb_sys_tables查看）
- 主键各列信息列表：<len, value>关键列占用空间和真实值信息列表
- start of record：本条undo 日志在页中开始的地址

各操作所对应的undo日志结构不同

1. **INSERT**：回滚时，就是删除掉新增的这条记录。暂时只考虑聚簇索引插入情况，因为二级索引也包含主键，所以删除时也会根据主键信息把二级索引中的内容也删除

   | 名称      | 内容                |
   | --------- | ------------------- |
   | undo type | TRX_UNDO_INSERT_REC |

2. **DELETE**：记录被删除时，该记录头部信息的delete_mask会置为1，且会根据头部信息中的next_record属性组成一个垃圾链表，而这个链表中各记录占用的空间可以被重用，数据页PAGE HEADER中有个PAGE_FREE属性记录了这个链表的头节点。这时我们需要将头部信息delete_mask置为1的记录从正常记录列表中删除，会经历两个阶段

   阶段一：该记录头部信息的delete_mask会置为1（处于中间状态，还在正常链表中，不在垃圾链表中），这是要回滚的。

   阶段二：在删除语句所在的事务提交后执行，使用专门的线程把记录从正常链表中移除，加入到垃圾链表的头节点处，调整数据页的一些信息和垃圾链表头指针，页中可用字节数量，页目录信息等。（todo 涉及到被删除记录空间重用过程）

   | 名称                        | 内容                                       |
   | --------------------------- | ------------------------------------------ |
   | undo type                   | TRX_UNDO_DEL_MARK_REC                      |
   | old trix_id                 | delete mark前记录                          |
   | old roll_pointer            | 找到修改之前最近的undo日志，比如新增的日志 |
   | index_col_info len          | 索引各列信息占用空间包括主键索引           |
   | 索引各列信息<pos,len,value> | 被索引的各列的位置/占用空间/值             |

3. **UPDATE**：

   - 更新主键

     InnoDB需要对聚簇索引进行处理

     - 将旧记录delete mark，因为其他事务可能需要访问（事务隔离机制）

     | 名称      | 内容                  |
     | --------- | --------------------- |
     | undo type | TRX_UNDO_DEL_MARK_REC |

     - 重新定位，并新增一条记录插到聚簇索引中

     | 名称      | 内容                |
     | --------- | ------------------- |
     | undo type | TRX_UNDO_INSERT_REC |

     这里会有两条undo记录

   - 不更新主键

     - 被更新各列占用存储空间都不变：直接在原纪录基础上改
     - .......变：用户线程同步地先从聚簇索引中真实的删除旧记录，不是delete mark，并修改一系列信息，再插入新记录

     | 名称                        | 内容                             |
     | --------------------------- | -------------------------------- |
     | undo type                   | TRX_UNDO_UPD_EXIST_REC           |
     | old trix_id                 |                                  |
     | old roll_pointer            |                                  |
     | n_updated                   | 被更新列的数量                   |
     | <pos,old len,old value>     | 被更新列更新前信息               |
     | index_col_info len          | 索引各列信息占用空间包括主键索引 |
     | 索引各列信息<pos,len,value> | 被索引的各列的位置/占用空间/值   |

     
undo日志

undo页面写入

存储undo日志的页面类型为FIL_PAGE_UNDO_LOG，和其他类型的页相比，多了Undo Page Header/Undo Log Segment Header/Undo Log Header属性。

> 通常InnoDB使用链表来管理页面信息，所以InnoDB会根据一个事务产生的所有undo日志存放页面的Undo Page Header--TRX_UNDO_PAGE_NODE而形成的链表，链表由ListBaseNode（存储头尾节点信息和节点数量）和ListNode（各页面基本信息和前后节点偏移量）组成

链表类型

- 普通表的insert类型为insert undo链表
- 普通表的update类型为updat undo链表；

- 临时表的insert类型为insert undo链表
- 临时表的update类型为updat undo链表；

一个事务可能有2*2个链表（为了提高并发场景下多事务写入undo日志的性能），这些链表的分配是按需分配的；每个事务的同类型链表不会存储到一起。

对于还没有被重用的undo页面链表来说，undo页面链表的第一个页面称为`first undo page`，在生成时就被填充`Undo Page Header/Undo Log Segment Header/Undo Log Header`属性；其余页面称为`normal undo page`，在生成时只被填充`Undo Log Header`属性；填充之后才开始写入undo日志

1. Undo Page Header

- TRX_UNDO_PAGE_TYPE：undo日志类型，只有两大类，不同类型undo日志不存在一起，因为insert类型的undo日志在事务提交后可以直接被删掉，但是delete等日志还需要为其他事务服务
  - TRX_UNDO_INSERT：1：新增记录/更新记录主键语句产生
  - TRX_UNDO_UPDATE：2：一般由delete和update语句产生，如TRX_UNDO_DEL_MARK_REC

- TRX_UNDO_PAGE_START：第一条undo日志在页面中的偏移量
- TRX_UNDO_PAGE_FREE：页面中存储的最后一条undo日志结束时的偏移量
- TRX_UNDO_PAGE_NODE：一个页面就是一个链表节点

`first undo page`多了一个`Undo Log Segment Header`属性，它包含了该链表对应段的`segment header`信息及其他一些关于这个段的信息

2. Undo Log Segment Header

- TRX_UNDO_STATE
- TRX_UNDO_LAST_LOG
- TRX_UNDO_FSEG_HEADER
- TRX_UNDO_PAGE_LIST

3. Undo Log Header：存储这个undo日志的事务的属性信息



undo页面重用

在某个事务提交后，重用该事务的undo页面链表，

但这个链表只能包含一个undo页面，这个undo页面的已使用空间小于整个空间的3/4，因为如果这个链表中有很多页面，接下来重用的事务的页面却没有这么多，意味着InnoDB还是得维护这么多页面，即使很多页面并没有被用到；

在基于上述的要求下，insert undo日志链表可以直接被覆盖；

但是update undo日志链表在事务提交后不能马上被删除，MVCC，所以想要重用这个undo链表只能在之前的undo页面后继续写入。



undo页面回滚

InnoDB设计Rollback Segment Header页面来存储各个undo页面链表的frist undo page的页号（undo slot），这样就可以通过这个undo slot找到和它有关的normal undo page。

Rollback Segment Header页面对应着一个segment，称为Rollback Segment回滚段（只存储一个页面）

todo 回滚段









