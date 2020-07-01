package com.zbb.lizi.design.strategy;

/**
 * 维护一个唯一的策略作用
 * @author tiancha
 * @since 2020/6/30 14:25
 */
public class RewardContext {
    private RewardStrategy rewardStrategy;

    public RewardContext(RewardStrategy rewardStrategy) {
        this.rewardStrategy = rewardStrategy;
    }

    public void doStrategy(long userId){
        int reward = rewardStrategy.reward(userId);
        rewardStrategy.insertAndSettleReward(userId, reward);
    }
}
