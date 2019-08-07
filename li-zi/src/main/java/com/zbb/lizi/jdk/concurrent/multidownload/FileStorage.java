package com.zbb.lizi.jdk.concurrent.multidownload;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 存储器
 * 功能
 * 1.创建文件存储：缓存+刷盘
 * @author by tiancha
 * @Date 2019/8/1 16:48
 */
public class FileStorage {

    private String fileName;

    private long pageSize;

    private final RandomAccessFile storeFile;

    private final FileChannel fileChannel;

    private final AtomicLong totalWrites = new AtomicLong(0);

    public FileStorage(String fileName, long pageSize) throws IOException {
        this.fileName = fileName;
        this.pageSize = pageSize;
        storeFile = new RandomAccessFile(fileName,"rw");
        fileChannel = storeFile.getChannel();
    }

    public long store(long offset, ByteBuffer buf) throws IOException {
        int length;
        fileChannel.write(buf, offset);
        length = buf.limit();
        totalWrites.addAndGet(length);
        return offset;
    }
}
