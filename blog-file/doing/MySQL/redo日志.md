redo日志

作用

innoDB存储引擎中，需要在服务器故障重启后，能够准确的恢复所有已提交的数据，保证数据持久性；如某个事务在内存Buffer Pool中已被提交（脏页），但服务器突然故障，数据就丢失了；

为了解决这个问题，可以采用修改页面刷新到磁盘，但因为可能只修改了一条记录，没必要实时刷新浪费时间，而且修改的记录并不一定是连续的，随机IO刷新较慢。

可以将已提交事务修改的记录记录下来，即某个表空间中某页的某个偏移量的值更新为多少，这个记录的文件就称为redo log。相比刷新内存中的页面到磁盘，redo log刷新到磁盘的内容小了很多，而且是一个顺序写入磁盘的过程。



日志格式

- type：类型
  - MLOG_1BYTE：1 ：表示在页面的某个偏移量处写入1个字节的`redo`日志类型。
  - MLOG_2BYTE：2
  - MLOG_4BYTE：4
  - MLOG_8BYTE：8
  - MLOG_WRITE_STRING：30
  - MLOG_REC_INSERT：9：表示插入一条使用非紧凑行格式记录时的redo日志类型（如redundant）
  - MLOG_COMP_REC_INSERT：38：表示插入一条使用非紧凑行格式记录时的redo日志类型（如compact/dynamic/compressed）
  - MLOG_COMP_REC_DELETE：42：表示删除一条使用紧凑行格式记录的`redo`日志类型
  - 。。。

- space id：表空间

- page number：页号

- data：真实数据
  - n_fileds：当前记录的字段数
  - n_uniques：决定该记录的唯一字段数量，如有主键的是主键数，唯一二级索引是索引列数+主键列数，普通二级索引一样
  - 。。。。

- len：类型为MLOG_WRITE_STRING时才有的，表示具体数据占用的字节数

Mini-Transaction

redo日志写入

































