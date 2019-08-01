package com.zbb.lizi.jdk.concurrent;

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

    public FileStorage(String fileName, long pageSize) {
        this.fileName = fileName;
        this.pageSize = pageSize;
    }
}
