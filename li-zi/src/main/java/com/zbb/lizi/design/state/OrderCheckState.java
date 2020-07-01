package com.zbb.lizi.design.state;

/**
 * 订单检查状态
 *
 * @author tiancha
 * @since 2020/6/30 15:19
 */
public class OrderCheckState extends RewardState {

    @Override
    void doReward(RewardStateContext context, RewardRequest request) {
        // 检查订单是否用券，订单金额是否满足
        orderCheck(context, request);
    }

    private void orderCheck(RewardStateContext context, RewardRequest request) {
        setSuccess(true);
    }
}
