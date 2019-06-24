## Zookeeper(五)持久化快照

#### 用途	

快照文件是指定时间间隔对zookeeper服务器上的节点数据的序列化后备份到磁盘中，快照文件不一定是最新的

如果zk集群挂了，可能会用到它来复原

#### 基本术语

- DataTree：zookeeper中数据存储结构



http://www.cnblogs.com/leesf456/p/6179118.html

#### 问题（别人的问题，我暂时没想到）

- 什么时候生成快照，快照什么时候被删除,会不会被删除
- 如果zk集群挂了是从哪里恢复，FileSnap还是FileTxn，FileSnap都不一定是最新的，zxid怎么保证
- 什么时候进行快照记录



#### 实现

##### SnapShot

```java
public interface SnapShot {
    //从最新且有效的快照中反序列化一个dataTree
    long deserialize(DataTree dt, Map<Long, Integer> sessions)  throws IOException;
    //持久化dataTree和会话到一个持久化存储中
    void serialize(DataTree dt, Map<Long, Integer> sessions, File name, boolean fsync)
        throws IOException;
    //查询最近的快照文件
    File findMostRecentSnapshot() throws IOException;
    //立刻释放快照文件中的资源
    void close() throws IOException;
}
```

##### FileSnap

```java
public class FileSnap implements SnapShot {
    public long deserialize(DataTree dt, Map<Long, Integer> sessions)
            throws IOException {
        // we run through 100 snapshots (not all of them)
        // if we cannot get it running within 100 snapshots
        // we should  give up
        //100个有效且最新的快照文件
        List<File> snapList = findNValidSnapshots(100);
        if (snapList.size() == 0) {
            return -1L;
        }
        File snap = null;
        boolean foundValid = false;
        //找到一个有效的快照 就break
        for (int i = 0, snapListSize = snapList.size(); i < snapListSize; i++) {
            snap = snapList.get(i);
            LOG.info("Reading snapshot " + snap);
            try (InputStream snapIS = new BufferedInputStream(new FileInputStream(snap));
                 CheckedInputStream crcIn = new CheckedInputStream(snapIS, new Adler32())) {
                InputArchive ia = BinaryInputArchive.getArchive(crcIn);
                deserialize(dt, sessions, ia);
                long checkSum = crcIn.getChecksum().getValue();
                long val = ia.readLong("val");
                if (val != checkSum) {
                    throw new IOException("CRC corruption in snapshot :  " + snap);
                }
                foundValid = true;
                break;
            } catch (IOException e) {
                LOG.warn("problem reading snap file " + snap, e);
            }
        }
        if (!foundValid) {
            throw new IOException("Not able to find valid snapshots in " + snapDir);
        }
        //获取该有效快照的zxid
        dt.lastProcessedZxid = Util.getZxidFromName(snap.getName(), SNAPSHOT_FILE_PREFIX);
        return dt.lastProcessedZxid;
    }
 
}
```

