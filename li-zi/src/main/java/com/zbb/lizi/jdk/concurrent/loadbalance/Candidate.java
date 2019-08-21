package com.zbb.lizi.jdk.concurrent.loadbalance;

import java.util.Iterator;

/**
 * @author by tiancha
 * @Date 2019/8/5 18:43
 */
public class Candidate implements Iterator<EndPoint>{

    public void remove() {

    }

    public boolean hasNext() {
        return false;
    }

    public EndPoint next() {
        return null;
    }
}
