package com.zbb.lizi.design.state;

/**
 * 预返奖状态
 *
 * @author tiancha
 * @since 2020/6/30 15:19
 */
public class PreSendRewardState extends RewardState {

    @Override
    void doReward(RewardStateContext context, RewardRequest request) {
        // 经过订单检查后，进入预返奖状态：加入延时队列
        preSendReward(context, request);
    }

    private void preSendReward(RewardStateContext context, RewardRequest request) {
        setSuccess(true);
    }
}
