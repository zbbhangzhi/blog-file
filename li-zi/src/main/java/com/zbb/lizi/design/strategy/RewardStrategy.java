package com.zbb.lizi.design.strategy;

/**
 * @author tiancha
 * @since 2020/6/30 14:18
 */
public abstract class RewardStrategy {
    /**
     * 计价规则
     * @param userId
     */
    public abstract int reward(long userId);

    /**
     * 更新用户信息及结算
     */
    public void insertAndSettleReward(long userId, int reward){
         insertReward(userId, reward);
         settlement(userId);
    }

    /**
     * 插入返奖记录
     */
    private void insertReward(long userId, int reward) {
    }

    /**
     * 用户结算
     */
    private void settlement(long userId) {
    }
}
