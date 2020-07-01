package com.zbb.lizi.design.strategy;

/**
 * 具体工厂创建具体的策略
 * @author tiancha
 * @since 2020/6/30 14:22
 */
public class FactorRewardStrategyFactory extends StrategyFactory {

    @Override
    public RewardStrategy createStrategy(Class clazz) {
        RewardStrategy product = null;
        try {
            product = (RewardStrategy)Class.forName(clazz.getName()).newInstance();
        }catch (Exception e){

        }
        return product;
    }
}
