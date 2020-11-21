package com.zbb.lizi.dubbo;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @Author: tiancha
 * @Date: 2020/9/25 10:53 下午
 */
public class SPIDemoTest {

    public static void main(String[] args) {
        ServiceLoader<ZSPi> serviceLoader = ServiceLoader.load(ZSPi.class);
        Iterator<ZSPi> iterator = serviceLoader.iterator();
        while (iterator.hasNext()){
            ZSPi obj = iterator.next();
            obj.zz();
        }
    }
}
