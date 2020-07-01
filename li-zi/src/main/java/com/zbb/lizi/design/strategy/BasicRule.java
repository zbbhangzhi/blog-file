package com.zbb.lizi.design.strategy;

/**
 * @author tiancha
 * @since 2020/6/30 15:54
 */
public abstract class BasicRule {
    /**
     * 判断是否经过规则执行
     */
    public abstract boolean evaluate(Object context);

    /**
     * 执行具体的规则内容
     */
    public abstract void execute();
}
