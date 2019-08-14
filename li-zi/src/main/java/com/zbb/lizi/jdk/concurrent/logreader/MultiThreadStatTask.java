package com.zbb.lizi.jdk.concurrent.logreader;

import java.io.InputStream;

/**
 * @author by tiancha
 * @Date 2019/8/12 16:26
 */
public class MultiThreadStatTask extends AbstractThreadStatTask{

    public MultiThreadStatTask(int inputBufferSize, int batchSize, InputStream in) {
        super(inputBufferSize, batchSize, in);
    }

    @Override
    public void doCalculate(){
        final AbstractLogReader logReader = new LogReaderThread(in, inputBufferSize, batchSize);
        logReader.start();
        String record;
        try {
            for (; ; ) {
                RecordSet recordSet = logReader.nextBatch();
                if (recordSet == null) {
                    break;
                }
                record = recordSet.nextRecord();
                //process
                recordProcessor.process(record);
            }
        }catch (Exception e){}
    }
}
