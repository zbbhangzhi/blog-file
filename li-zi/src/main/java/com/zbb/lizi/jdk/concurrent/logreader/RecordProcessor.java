package com.zbb.lizi.jdk.concurrent.logreader;

import java.util.Map;

/**
 * 日志处理
 * @author by tiancha
 * @Date 2019/8/14 14:32
 */
public interface RecordProcessor {

    void process(String recordSet);

    Map<String, Object> summary();
}
