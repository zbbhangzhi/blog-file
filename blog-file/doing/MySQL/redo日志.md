## redo日志

### 作用

innoDB存储引擎中，需要在服务器故障重启后，能够准确的恢复所有已提交的数据，保证数据持久性；如某个事务在内存Buffer Pool中已被提交（脏页），但服务器突然故障，数据就丢失了；

为了解决这个问题，可以采用修改页面刷新到磁盘，但因为可能只修改了一条记录，没必要实时刷新浪费时间，而且修改的记录并不一定是连续的，随机IO刷新较慢。

可以将已提交事务修改的记录记录下来，即某个表空间中某页的某个偏移量的值更新为多少，这个记录的文件就称为redo log。相比刷新内存中的页面到磁盘，redo log刷新到磁盘的内容小了很多，而且是一个顺序写入磁盘的过程。

redo日志不止记录索引插入/更新记录等操作，还有执行这个操作影响到的其他动作，如页分裂新增目录项记录，修改页信息等对数据页做的任何修改等等。

### 日志格式

- type：类型
  - MLOG_1BYTE：1 ：表示在页面的某个偏移量处写入1个字节的`redo`日志类型。
  - MLOG_2BYTE：2
  - MLOG_4BYTE：4
  - MLOG_8BYTE：8
  - MLOG_WRITE_STRING：30
  - MLOG_REC_INSERT：9：表示插入一条使用非紧凑行格式记录时的redo日志类型（如redundant）
  - MLOG_COMP_REC_INSERT：38：表示插入一条使用非紧凑行格式记录时的redo日志类型（如compact/dynamic/compressed）
  - MLOG_COMP_REC_DELETE：42：表示删除一条使用紧凑行格式记录的`redo`日志类型
  - MLOG_COMP_LIST_START_DELETE和MLOG_COMP_LIST_END_DELETE：批量删除，可以很大程度上减少redo日志的条数

- space id：表空间

- page number：页号

- data：真实数据（以MLOG_COMP_REC_INSERT为例）
  - n_fileds：当前记录的字段数
  - n_uniques：决定该记录的唯一字段列数量，如有主键的是主键数，唯一二级索引是索引列数+主键列数，普通二级索引一样；插入时可根据这个字段进行排序
  - field1_len-fieldn_len：若干字段占用存储空间的大小
- offset：记录前一条记录在页面中的位置，方便修改页面中的记录链表，前一条记录维护的next_record属性
  - end_seg_len：通过这个字段可得知当前记录占用存储空间大小
  
- len：类型为MLOG_WRITE_STRING时才有的，表示具体数据占用的字节数

### Mini-Transaction

Mini-Transaction（mtr）是对底层页面中的一次原子访问的过程（如MAX_ROW_ID生成/索引记录插入）。一个事务可以包含多个mtr，一个mtr包含多条redo日志。

#### 以组的形式写入日志

针对要保证**原子性**的操作必须以**组**的形式来记录redo日志，以插入一条记录为例，当出现页分裂时，会涉及到申请数据页，改动系统页，改各种段/区的统计信息，free等链表的信息，新增目录项记录等等操作，要么全都执行完成要么全都没执行。

划分日志组：通过为这一系列动作的日志的最后一条日志后添加一个特别的日志，类型为`MLOG_MULTI_REC_END：31`作为结尾。

如果某个原子操作只有一条日志记录，那么给这条日志类型的第一个**比特位**设为1，不然就是产生了一系列日志。

#### redo日志写入过程

MySQL使用512字节的页来记录redo日志，这种页被称为`block`，`block`由`log block header`，`log block trailer`和`log block body`组成，header和trailer存储页的管理信息。

服务器启动时会先向系统申请一片连续的内存作为redo日志缓冲区log buffer，以两个事务为例，事务T1和T2是交替执行的，所以可能是交替存储到log buffer的。它们各自包含两个mtr，每个mtr在运行过程中，会先将redo log存在一个地方，等到这个mtr执行结束，就会将这个mtr产生的所有redo日志**全部复制**到log buffer，一定时间后才会冲刷到磁盘。

向`log buffer`中写入日志是顺序的，所以block不会出现碎片空间，InnoDB提供一个全局变量`buf_free`来指明后续写入的redo日志应该往block的哪个位置中写入，即从这个位置开始后面都是空闲的。

redo日志刷盘























