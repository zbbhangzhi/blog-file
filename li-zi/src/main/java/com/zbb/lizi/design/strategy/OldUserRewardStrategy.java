package com.zbb.lizi.design.strategy;

/**
 * 老用户返奖计价策略
 * @author tiancha
 * @since 2020/6/30 14:20
 */
public class OldUserRewardStrategy extends RewardStrategy {

    @Override
    public int reward(long userId) {
        return 0;
    }
}
