package com.zbb.lizi.jdk.concurrent.logreader;

import java.io.InputStream;
import java.util.Map;

/**
 * @author by tiancha
 * @Date 2019/8/12 16:40
 */
public abstract class AbstractThreadStatTask  implements Runnable {

    // 日志文件输入缓冲大小
    protected final int inputBufferSize;
    // 日志记录集大小
    protected final int batchSize;
    // 日志文件输入流
    protected final InputStream in;

    protected final RecordProcessor recordProcessor;

    public AbstractThreadStatTask(int inputBufferSize, int batchSize, InputStream in) {
        this.inputBufferSize = inputBufferSize;
        this.batchSize = batchSize;
        this.in = in;
        recordProcessor = new FinalRecordProcessor();
    }
    /**
     * 统计操作
     */
    public abstract void doCalculate();

    /**
     * 执行统计
     * 输出统计结果
     */
    public void run() {
        doCalculate();

        Map<String, Object> result = recordProcessor.summary();

        report(result);
    }

    protected abstract void report(Map<String, Object> result);
}
