package com.zbb.lizi.jdk.concurrent.logreader;

import java.io.InputStream;
import java.util.Map;

/**
 * @author by tiancha
 * @Date 2019/8/12 16:26
 */
public class MultiThreadStatTask extends AbstractThreadStatTask{

    public MultiThreadStatTask(int inputBufferSize, int batchSize, InputStream in) {
        super(inputBufferSize, batchSize, in);
    }

    @Override
    protected void report(Map<String, Object> result) {

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
