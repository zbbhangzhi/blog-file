package com.zbb.lizi.design.state;

/**
 * 待补偿状态
 *
 * @author tiancha
 * @since 2020/6/30 15:19
 */
public class CompensateRewardState extends RewardState {

    @Override
    void doReward(RewardStateContext context, RewardRequest request) {
        // 返奖失败，需要重新返奖
        compensateReward(context, request);
    }

    private void compensateReward(RewardStateContext context, RewardRequest request) {
        setSuccess(true);
    }
}
