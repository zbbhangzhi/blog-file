package com.zbb.lizi.serialize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

/**
 * 1。序列化目的：a）将对象吃持久化在内存中 b）将对象以二进制形式在网络中进行传输
 * 2。解析序列化原理
 * 3。分析序列化优化，减少内存占用/序列化效率
 * @Author: tiancha
 * @Date: 2020/10/29 2:14 下午
 */
public class SerializeRun {



    public static void main(String[] args) throws Exception {
        FileOutputStream fos = new FileOutputStream("");
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos);
        // 序列化
        objectOutputStream.writeObject(null);
    }
}
