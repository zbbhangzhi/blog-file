package com.zbb.lizi.jdk.concurrent;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author by tiancha
 * @Date 2019/8/1 19:10
 */
public class DownloadBuffer implements Closeable {


    /**
     * 写入缓冲区
     * @param byteBuffer
     */
    public void write(ByteBuffer byteBuffer){
        int length = byteBuffer.position();

    }

    public void close() throws IOException {

    }
}
