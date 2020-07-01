package com.zbb.lizi.design.strategy;

/**
 * 抽象工厂
 * @author tiancha
 * @since 2020/6/30 14:21
 */
public abstract class StrategyFactory<T> {

    public abstract RewardStrategy createStrategy(Class<T> clazz);
}
