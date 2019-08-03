## redo日志

### 作用

innoDB存储引擎中，需要在服务器故障重启后，能够准确的恢复所有已提交的数据，保证数据持久性；如某个事务在内存Buffer Pool中已被提交（脏页），但服务器突然故障，数据就丢失了；

为了解决这个问题，可以采用修改页面刷新到磁盘，但因为可能只修改了一条记录，没必要实时刷新浪费时间，而且修改的记录并不一定是连续的，随机IO刷新较慢。

可以将已提交事务修改的记录记录下来，即某个表空间中某页的某个偏移量的值更新为多少，这个记录的文件就称为redo log。相比刷新内存中的页面到磁盘，redo log刷新到磁盘的内容小了很多，而且是一个顺序写入磁盘的过程。

redo日志不止记录索引插入/更新记录等操作，还有执行这个操作影响到的其他动作，如页分裂新增目录项记录，修改页信息等对数据页做的任何修改等等。

和binlog区别：binlog记录的是页已经正式落盘的操作且是包含所有存储引擎，redo日志记录InnoDB引擎下仍然在buffer pool中的操作，用于系统奔溃时恢复脏页。

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

#### redo日志内存内操作

##### Mini-Transaction

Mini-Transaction（mtr）是对底层页面中的一次**原子访问**的过程（如`MAX_ROW_ID`生成/索引记录插入）。一个事务可以包含多个mtr，一个mtr包含多条redo日志。

##### 以组的形式写入日志

针对要保证**原子性**的操作必须以**组**的形式来记录redo日志，以插入一条记录为例，当出现页分裂时，会涉及到申请数据页，改动系统页，改各种段/区的统计信息，free等链表的信息，新增目录项记录等等操作，要么全都执行完成要么全都没执行。

划分日志组：通过为这一系列动作的日志的最后一条日志后添加一个特别的日志，类型为`MLOG_MULTI_REC_END：31`作为结尾。

如果某个原子操作只有一条日志记录，那么给这条日志类型的第一个**比特位**设为1，不然就是产生了一系列日志。

##### redo日志写入过程

MySQL使用512字节的页来记录redo日志，这种页被称为`block`，`block`由`log block header`，`log block trailer`和`log block body`组成，header和trailer存储页的管理信息。

服务器启动时会先向系统申请一片连续的内存作为redo日志缓冲区log buffer，以两个事务为例，事务T1和T2是交替执行的，所以可能是交替存储到log buffer的。它们各自包含两个mtr，每个mtr在运行过程中，会先将redo log存在一个地方，等到这个mtr执行结束，就会将这个mtr产生的所有redo日志**全部复制**到log buffer，一定时间后才会冲刷到磁盘。

向`log buffer`中写入日志是顺序的，所以block不会出现碎片空间，InnoDB提供一个全局变量`buf_free`来指明后续写入的redo日志应该往block的哪个位置中写入，即从这个位置开始后面都是空闲的。

#### redo日志刷盘

##### 刷盘准备

当修改`buffer pool`中的页时，会将这个脏页的控制块插入到flush链表中，控制块存储了两个变量：`oldest_modification`被加载到`buffer pool`中第一次修改mtr**开始时**对应的lsn值，`newest_modification`每次mtr修改**结束时**对应的lsn值；控制块按照`oldest_modification`从大到小排序存储。

一个mtr可能修改多个页，所以多个控制块的`oldest_modification`/`newest_modification`可能一样。

##### 刷盘时机

- log buffer空间不足，空闲空间小于一半时
- 事务提交时，buffer pool中的脏页可以先不刷盘，但其中的log buffer需要刷盘，防止丢失
- 后台线程定时刷盘
- 正常关闭服务器时
- checkpoint时：批量从flush链表中刷出脏页：如果系统修改页面频繁，且不能将脏页刷出，则不能及时checkpoint，可能会直接使用用户线程同步的从flush链表中最早修改的脏页刷盘，这样这些脏页对应的redo日志就没用了，就可以checkpoint了

##### redo日志文件存储

在MySQL的数据目录下，（由`innodb_log_group_home_dir`确定存储位置，由名称可知存储形式是一个日志文件组）名为`ib_logfile0`...n的文件，文件个数决定文件名称后缀，由系统参数`innodb_log_files_in_group`确定文件个数，每个文件的大小由`innodb_log_file_size`指定。

每个`ib_logfile`顺序循环写入`log buffer`中的block，会出现文件被覆盖的现象。

###### 日志文件格式

- 前2048个字节（4个block）：存储管理信息
  - log file header：存储当前文件的redo日志版本，文件开始的LSN值等等
  - checkpoint1：标记日志文件可覆盖信息
    - LOG_CHECKPOINT_NO：checkpoint次数，递增记录
    - LOG_CHECKPOINT_LSN：redo日志文件可被覆盖的最大lsn值
    - LOG_CHECKPOINT_OFFSET：lsn对应日志文件的偏移量
    - LOG_CHECKPOINT_LOG_BUF_SIZE
  - 没用
  - checkpoint2

- 后面的字节：存储block内容，会被循环使用

###### 几个全局变量

- Log Sequeue Number（lsn）：InnoDB记录已写入的redo log量的全局变量，包括log buffer写入的日志，初始值为8704，此时日志文件的偏移量为2048字节。
  - 第一次一个mtr生成一组日志并加入到log buffer时，lsn=8704+日志写入量+log block header
  - 再次生产一组日志时，在同一个block 内，且能容纳，就只需要加上日志写入量；
  - 又生成一组日志，但占用量较大，当前block中剩余空间不可容纳，需要占用到下下个block时，lsn=lsn+日志写入量+2 *log block header+2 *log block trailer；
- flushed_to_disk_lsn：刷新到**磁盘**中的redo日志量的全局变量

- buf_next_to_write：标记已有哪些**log buffer**中的日志被刷盘的全局变量

- innodb_flush_log_at_trx_commit=？：表示用户线程提交时需要将该事务执行过程中产生的所有redo日志刷盘到磁盘（1）还是交给后台线程操作（0），或者先写入到缓冲区中（2）。

##### checkpoint

判断某些redo日志占用的磁盘空间是否可以被覆盖，即它对应的脏页是否已经刷新到磁盘

`checkpoint_lsn`：代表当前系统中可以被覆盖（脏页已经被刷盘）的redo日志总量，初始值为8074。

checkpoint步骤：

- 计算可以被覆盖的redo日志对应的lsn最大值（flush链表最小`oldest_modification`值对应的lsn之前的日志都是可以被覆盖掉的，因为flush链表不存储已经被刷盘的脏页），赋值给`checkpoint_lsn`；
- 将`checkpoint_lsn`对应的redo日志文件组偏移量`checkpoint_offset`及此次`checkpoint`的编号（总共做了多少次checkpoint，递增）`checkpoint_no`写到日志文件头部的`checkpoint1/2`中

##### 奔溃恢复

恢复奔溃发生时，flush链表中还未写入磁盘的脏页更改。

- 确定恢复的起点：比较日志中文件中`checkpoint1`和`checkpoint2`中最大的checkpoint_no，然后从对应的`checkpoint_lsn`（之前的都是可覆盖的，说明已经被刷盘了）开始恢复
- 确定恢复的终点：比较每个日志文件`log block header`的`LOG_BLOCK_HDR_DATA_LEN`属性，如果不为512则说明是最后一个填充的日志文件
- 恢复方法
  - 哈希表：计算redo日志的`hash(表空间id+页号)`，相同值的放在一个slot中，并根据生成时间排序链表形式连接，这样可以一次修复一个页面，避免多余的随机IO
  - 跳过奔溃时已被恢复的页：flush链表中可能存在已经被刷盘的脏页，所以会根据脏页的file header中FIL_PAGE_LSN属性即控制块中的`newest_modification`是否大于`checkpoint_lsn`，如果是那么就不需要执行小于`newest_modification`的`FIL_PAGE_LSN`的redo日志了
