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









