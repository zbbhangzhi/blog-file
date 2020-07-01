package com.zbb.lizi.design.state;

/**
 * 开始返奖状态
 *
 * @author tiancha
 * @since 2020/6/30 15:19
 */
public class FailRewardState extends RewardState {

    @Override
    void doReward(RewardStateContext context, RewardRequest request) {
        // 预检查等某步骤失败，进入返奖失败流程
        failReward(context, request);
    }

    private void failReward(RewardStateContext context, RewardRequest request) {
        setSuccess(true);
    }
}
