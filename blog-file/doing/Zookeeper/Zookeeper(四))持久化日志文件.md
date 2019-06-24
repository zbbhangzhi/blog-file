## Zookeeper(四))持久化日志文件

#### 持久化用途

存储两种文件

- snapshot：内存快照

- log：事务日志，类似MySQL的binlog，存储数据节点的操作日志



#### 问题

- 序列化的本质其实就是将原数据重新写入

- roll中的BufferedOutputStream.flush和commit中的FileChannel.force()都是强制刷新：有什么区别



#### 基本术语

- FileTxnSnapLog，封装了TxnLog和SnapShot。 是操作数据文件和快照文件的对外API

- TxnLog，接口类型，读取事务性日志的接口。 

- FileTxnLog，实现TxnLog接口，添加了访问该事务性日志的API。

- Snapshot，接口类型，持久层快照接口。 

- FileSnap，实现Snapshot接口，负责存储、序列化、反序列化、访问快照。 

- Util，工具类，提供持久化所需的API。

#### 实现

##### TxnLog

```java
public interface TxnLog {
    /**
     * Setter for ServerStats to monitor fsync threshold exceed
     * @param serverStats used to update fsyncThresholdExceedCount
     */
    void setServerStats(ServerStats serverStats);
    
    // 滚动日志，从当前日志滚到下一个日志，不是回滚
    void rollLog() throws IOException;
    // 添加一个请求至事务性日志
    boolean append(TxnHeader hdr, Record r) throws IOException;

    // 读取事务性日志
    TxnIterator read(long zxid) throws IOException;
    
    // 事务性操作的最新zxid
    long getLastLoggedZxid() throws IOException;
    
    // 清空zxid以后的日志
    boolean truncate(long zxid) throws IOException;
    
    // 获取数据库的id
    long getDbId() throws IOException;
    
    // 提交事务并进行确认
    void commit() throws IOException;

    long getTxnLogSyncElapsedTime();
   
    // 关闭事务性日志
    void close() throws IOException;
}
```

##### FileTxnLog

实现TxnLog接口，提供操作事务日志的api

```java
public class FileTxnLog implements TxnLog {
    //最新的日志zxid
    long lastZxidSeen;
    //日志文件
    volatile BufferedOutputStream logStream = null;
    volatile OutputArchive oa;
    //日志存储文件
    File logFileWrite = null;
    private FilePadding filePadding = new FilePadding();
    private LinkedList<FileOutputStream> streamsToFlush =
        new LinkedList<FileOutputStream>();
    //重置日志文件
    public synchronized void rollLog() throws IOException {
        if (logStream != null) {
            this.logStream.flush();
            this.logStream = null;
            oa = null;
        }
    }
    //添加事务日志 hdr：事务标题 txn：事务本身
    public synchronized boolean append(TxnHeader hdr, Record txn)
        throws IOException {
        if (hdr == null) {
            return false;
        }
        //判断并更新最新的zxid
        if (hdr.getZxid() <= lastZxidSeen) {
            LOG.warn("Current zxid " + hdr.getZxid()
                    + " is <= " + lastZxidSeen + " for " + hdr.getType());
        } else {
            lastZxidSeen = hdr.getZxid();
        }
        //构建事务日志文件
        if (logStream==null) {
           if(LOG.isInfoEnabled()){
                LOG.info("Creating new log file: " + Util.makeLogName(hdr.getZxid()));
           }
		   //1. 生成新的log文件
           logFileWrite = new File(logDir, Util.makeLogName(hdr.getZxid()));
           //2. 生成log文件输出流
           fos = new FileOutputStream(logFileWrite);
           //为写入给定的输出流而创建缓冲输出流
           logStream=new BufferedOutputStream(fos);
           //获取二进制序列化类 TODO
            //BinaryOutputArchive内部维护一个DataOutput 根据值传递特性 
            //oa序列化写入时其实就是写入log文件
           oa = BinaryOutputArchive.getArchive(logStream);
           //3. 用TXNLOG_MAGIC VERSION dbId来生成事务日志文件头
           FileHeader fhdr = new FileHeader(TXNLOG_MAGIC,VERSION, dbId);
           //4. 将事务日志文件头序列化到文件上
           fhdr.serialize(oa, "fileheader");
           //确保文件扩展之前 魔数已被写入
           logStream.flush();
           filePadding.setCurrentSize(fos.getChannel().position());
           streamsToFlush.add(fos);
        }
        //5. 剩余空间不足时 填充文件
        filePadding.padFile(fos.getChannel());
        //6. 将事务头部和本身序列化为字节数组
        byte[] buf = Util.marshallTxnEntry(hdr, txn);
        if (buf == null || buf.length == 0) {
            throw new IOException("Faulty serialization for header " + "and txn");
        }
        //生成验证算法 校验数据流
        Checksum crc = makeChecksumAlgorithm();
        crc.update(buf, 0, buf.length);
        oa.writeLong(crc.getValue(), "txnEntryCRC");
        //6. 将当前序列化的事务记录写入到oa
        Util.writeTxnBytes(oa, buf);
        return true;
    }
    //从给定的zxid开始读取日志文件
    public TxnIterator read(long zxid, boolean fastForward) throws IOException {
        return new FileTxnIterator(logDir, zxid, fastForward);
    }
    //提交日志 保存到磁盘
    public synchronized void commit() throws IOException {
        //刷到磁盘
        if (logStream != null) {
            logStream.flush();
        }
        //强刷到磁盘
        for (FileOutputStream log : streamsToFlush) {
            log.flush();
            if (forceSync) {
                long startSyncNS = System.nanoTime();
                FileChannel channel = log.getChannel();
                //会强制将所有未写入磁盘的数据都强制写入磁盘 比如还在缓冲区中的数据
                channel.force(false);
                syncElapsedMS = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startSyncNS);
                if (syncElapsedMS > fsyncWarningThresholdMS) {
                    if(serverStats != null) {
                        serverStats.incrementFsyncThresholdExceedCount();
                    }
                    LOG.warn("fsync-ing the write ahead log in "
                            + Thread.currentThread().getName()
                            + " took " + syncElapsedMS
                            + "ms which will adversely effect operation latency. "
                            + "File size is " + channel.size() + " bytes. "
                            + "See the ZooKeeper troubleshooting guide");
                }
                ServerMetrics.FSYNC_TIME.add(syncElapsedMS);
            }
        }
        //移除流并关闭
        while (streamsToFlush.size() > 1) {
            streamsToFlush.removeFirst().close();
        }
       //当日志文件大小超过限制 强刷到磁盘并重置
        if(txnLogSizeLimit > 0) {
            long logSize = getCurrentLogSize();
            if (logSize > txnLogSizeLimit) {
                LOG.debug("Log size limit reached: {}", logSize);
                rollLog();
            }
        }
    }
}
```



FileTxnIterator：用于读取事务日志

```java
public static class FileTxnIterator implements TxnLog.TxnIterator {
    public FileTxnIterator(File logDir, long zxid, boolean fastForward)
                throws IOException {
        this.logDir = logDir;
        this.zxid = zxid;
        init();
        if (fastForward && hdr != null) {
            while (hdr.getZxid() < zxid) {
                if (!next())
                    break;
            }
        }
    }
    void init() throws IOException {
        storedFiles = new ArrayList<File>();
        //找出大于等于snapshot中最大的zxid的logfile及后续logfile并升序
        List<File> files = Util.sortDataDir(FileTxnLog.getLogFiles(logDir.listFiles(), 0), LOG_FILE_PREFIX, false);
        for (File f: files) {
            if (Util.getZxidFromName(f.getName(), LOG_FILE_PREFIX) >= zxid) {
                storedFiles.add(f);
            }
            // add the last logfile that is less than the zxid
            else if (Util.getZxidFromName(f.getName(), LOG_FILE_PREFIX) < zxid) {
                storedFiles.add(f);
                break;
            }
        }
        goToNextLog();
        next();
    }
}
```