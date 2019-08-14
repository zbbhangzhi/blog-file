package com.zbb.lizi.jdk.concurrent.logreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 日志读取线程
 * @author by tiancha
 * @Date 2019/8/12 16:45
 */
public abstract class AbstractLogReader extends Thread {

    /**
     * 缓冲区
     */
    private final BufferedReader logReader;

    /**
     * 读取结束标志
     */
    private volatile boolean EOF = false;

    /**
     * 批量写入大小
     */
    private final int batchSize;

    public AbstractLogReader(InputStream in, int inputBufferSize, int batchSize) {
        this.logReader = new BufferedReader(new InputStreamReader(in), inputBufferSize);
        this.batchSize = batchSize;
    }

    /**
     * 存储/发布日志集
     */
    public abstract void publish(RecordSet recordSet)  throws InterruptedException ;

    /**
     * 提取下一个日志记录
     * @return
     */
    public abstract RecordSet nextBatch() throws InterruptedException ;

    /**
     * 填充日志集
     * 返回是否读取结束
     */
    private boolean doFill(RecordSet recordSet) throws IOException {
        int capacity = recordSet.getCapacity();
        String record;
        for (int i = 0; i < capacity; i++){
            record = logReader.readLine();
            if (record == null){
                return true;
            }
            recordSet.putRecord(record);
        }
        return false;
    }

    @Override
    public void run() {
        RecordSet recordSet;
        //判断是否读完
        boolean isEof = false;
        try {
            while (true) {
                //创建新的日志集
                recordSet = new RecordSet(batchSize);
                //填充日志集
                isEof = doFill(recordSet);
                //存储日志集
                publish(recordSet);
                //判断是否读完 是：填充空字符，作为结尾标志位
                if (isEof) {
                    publish(new RecordSet(1));
                    EOF = isEof;
                    break;
                }
            }
        }catch (IOException e){

        }catch (InterruptedException e){

        }finally {
            try {
                logReader.close();
            }catch (Exception e){}
        }
    }

}
