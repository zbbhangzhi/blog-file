package com.zbb.lizi.jdk.concurrent.multidownload;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * 下载任务
 * @author by tiancha
 * @Date 2019/8/1 17:07
 */
public class DownloadTask implements Runnable {

    private final long lowerBound;

    private final long upperBound;

    private final URL fileUrl;

    private final DownloadBuffer downloadBuffer;

    public DownloadTask(long lowerBound, long upperBound, URL fileUrl, FileStorage storage) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.fileUrl = fileUrl;
        this.downloadBuffer = new DownloadBuffer(0, storage);
    }

    public void run() {
        ReadableByteChannel channel = null;
        try {
            channel = Channels.newChannel(requestSegmentDownload(fileUrl, lowerBound, upperBound));
            ByteBuffer buf = ByteBuffer.allocate(1024);
            //将从网络读取的数据写入缓冲区
            while (channel.read(buf) > 0) {
                downloadBuffer.write(buf);
                buf.clear();
            }
        }catch (Exception e){

        }finally {
//            channel.close();
//            downloadBuffer.close();
        }
    }

    private InputStream requestSegmentDownload(URL requestURL, long lowerBound, long upperBound) throws IOException {
        Thread thisThread = Thread.currentThread();
        final HttpURLConnection conn;
        InputStream in = null;
        conn = (HttpURLConnection)requestURL.openConnection();
        String strConnTimeout = System.getProperty("");
        int connTimeout = Integer.valueOf(strConnTimeout);
        conn.setConnectTimeout(connTimeout);

        String strReadTimeout = System.getProperty("");
        int readTimeout = Integer.parseInt(strReadTimeout);
        conn.setReadTimeout(readTimeout);

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Connection", "Keep-alive");
        conn.setRequestProperty("Range", "bytes=" + lowerBound + "-" + upperBound);
        conn.setDoInput(true);
        conn.connect();

        int statusCode = conn.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_PARTIAL){
            conn.disconnect();
            //抛错
        }

        in = new BufferedInputStream(conn.getInputStream()){
            @Override
            public void close() throws IOException {
                try{
                    super.close();
                }finally {
                    conn.disconnect();
                }
            }
        };
        return in;
    }
}
