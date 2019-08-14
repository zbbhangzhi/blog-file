package com.zbb.lizi.jdk.concurrent.logreader;

import java.util.HashMap;
import java.util.Map;

/**
 * @author by tiancha
 * @Date 2019/8/14 14:38
 */
public class FinalRecordProcessor implements RecordProcessor {

    public void process(String record) {
        //先过滤不需要的日志
        String[] records = filterRecord(record);
        if (records == null || records.length == 0){
            return;
        }
        //处理日志信息（如发送警告等）
        process(records);
    }

    public Map<String, Object> summary() {
        return new HashMap<String, Object>();
    }

    private void process(String[] records) {

    }

    private String[] filterRecord(String record) {
        return null;
    }

}
