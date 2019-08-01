package com.zbb.lizi.jdk.concurrent;

import java.net.URL;

/**
 * 大文件下载
 * 功能：
 * 1.下载
 * 2.定时报告
 * 3.取消下载
 * 需要线程隔离吗
 */
public class BigFileMultiDownload {

    private URL fileUrl;

    private long fileSize;

    private FileStorage storage;

    public BigFileMultiDownload(String fileUrl) throws Exception{
        this.fileUrl = new URL(fileUrl);
        this.fileSize = getFileSize(fileUrl);
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        storage = new FileStorage(fileName, fileSize);
    }

    private long getFileSize(String fileUrl) {
        return fileSize;
    }

    /**
     * 分配下载任务
     * @param taskCount
     * @param reportInterval
     */
    public void download(int taskCount, long reportInterval){
        //均分下载段
        long chunkSizePerThread = fileSize / taskCount;

        //下载数据段的起始字节
        long lowerBound = 0;

        //下载数据段的结束字节
        long upperBound = 0;

        for (int i = taskCount - 1; i >= 0; i--){
            lowerBound = chunkSizePerThread * i;
            if (i == 0) {
                upperBound = chunkSizePerThread;
            }else {
                upperBound = lowerBound + chunkSizePerThread - 1;
            }
            DownloadTask dt = new DownloadTask(lowerBound, upperBound, fileUrl, storage);
            dispatchWork(dt, i);
        }
    }

    private void dispatchWork(DownloadTask dt, int threadIndex) {
        Thread workerThread = new Thread(dt);
        workerThread.setName("downloader-" + threadIndex);
        workerThread.start();
    }

}
