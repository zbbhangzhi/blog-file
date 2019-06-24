## RocketMQ文件恢复和过期文件删除

### 文件恢复

![1553516003784](E:\other\blog-file\doing\Rocketmq\文件恢复过程.png)

1. #### 什么时候需要恢复文件？

   1. CommitLog已经刷盘的数据，ConsumeQueue/Index还没有构建完成：恢复commitLog
   2. ConsumeQueue/Index已经构建完成并刷盘的数据，CommitLog没有完成刷盘
   3. broker异常crash

2. #### 需要恢复哪些文件？

   commitLog文件的recover

   consumerQueue文件的recover

   index文件的recover

### 删除文件

1. #### 什么时候需要删除文件

   1. 空间不足
   2. 如果非当前写文件在一定时间间隔内没有被再次被更新，则认为是过期文件，可以删除

2. #### 怎么删除？

   定时任务执行线程DefaultMessageStore#cleanFilesPeriodically

   - CleanCommitLogService#run
   - CleanConsumeQueueService#run
     - IndexService#deleteExpiredFiles

