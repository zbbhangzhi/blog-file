package com.zbb.lizi.jdk.concurrent.multidownload;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author by tiancha
 * @Date 2019/8/1 19:10
 */
public class DownloadBuffer implements Closeable {

    private final ByteBuffer byteBuffer;

    private final FileStorage storage;

    private int offset = 0;

    private long globalOffset;

    public DownloadBuffer(long globalOffset, FileStorage storage) {
        this.byteBuffer = ByteBuffer.allocate(1024 * 1024);
        this.storage = storage;
        this.globalOffset = globalOffset;
    }

    /**
     * 写入缓冲区
     * @param buf
     */
    public void write(ByteBuffer buf) throws IOException{
        int length = buf.position();
        final int capacity = byteBuffer.capacity();
        //容量不足时 刷盘
        if (offset + length > capacity || length == capacity){
            flush();
        }
        byteBuffer.position(offset);
        buf.flip();
        byteBuffer.put(buf);
        offset += length;
    }

    private void flush() throws IOException{
        byteBuffer.flip();
        globalOffset += storage.store(globalOffset, byteBuffer);
        byteBuffer.clear();
        offset = 0;
    }

    public void close() throws IOException {

    }
}
