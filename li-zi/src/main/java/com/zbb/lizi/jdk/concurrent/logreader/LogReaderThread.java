package com.zbb.lizi.jdk.concurrent.logreader;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author by tiancha
 * @Date 2019/8/12 16:57
 */
public class LogReaderThread extends AbstractLogReader {

    private final BlockingQueue<RecordSet> channel = new ArrayBlockingQueue<RecordSet>(2);

    public LogReaderThread(InputStream in, int inputBufferSize, int batchSize) {
        super(in, inputBufferSize, batchSize);
    }

    @Override
    public RecordSet nextBatch() throws InterruptedException {
        RecordSet recordSet = channel.take();
        if (recordSet.isEmpty()){
            return null;
        }
        return recordSet;
    }

    @Override
    public void publish(RecordSet recordSet) throws InterruptedException {
        channel.put(recordSet);
    }
}
