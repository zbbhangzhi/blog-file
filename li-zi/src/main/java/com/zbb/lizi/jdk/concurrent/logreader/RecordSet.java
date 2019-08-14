package com.zbb.lizi.jdk.concurrent.logreader;

/**
 * 结果集
 * @author by tiancha
 * @Date 2019/8/12 15:21
 */
public class RecordSet {
    private String[] contents;

    private final int capacity;

    private int readIndex;

    private int writeIndex;

    public RecordSet(int capacity) {
        this.capacity = capacity;
        contents = new String[capacity];
    }

    public boolean putRecord(String content){
        if (writeIndex == capacity){
            return true;
        }
        contents[writeIndex++] = content;
        return false;
    }

    public boolean reset(){
        readIndex = 0;
        writeIndex = 0;

        for (int i = 0; i < capacity; i++){
            contents[i] = null;
        }
        return true;
    }

    public boolean isEmpty(){
        return writeIndex == 0;
    }

    public int getCapacity() {
        return capacity;
    }

    public String nextRecord(){
        String record = null;
        if (readIndex < writeIndex){
            record = contents[readIndex++];
        }
        return record;
    }
}
