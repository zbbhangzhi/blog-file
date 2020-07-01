package com.zbb.lizi.design.state;

/**
 * 组织多个state形成一个状态转换图来实现业务逻辑
 * 返回状态执行的上下文
 *
 * @author tiancha
 * @since 2020/6/30 15:13
 */
public class RewardStateContext {
    private RewardState rewardState;

    public RewardState getRewardState() {
        return rewardState;
    }

    public void setRewardState(RewardState rewardState) {
        this.rewardState = rewardState;
    }

    public void echo(RewardStateContext context, RewardRequest request) {
        rewardState.doReward(context, request);
    }

    /**
     * 流程是否成功
     */
    public boolean isResultFlag() {
        return rewardState.isSuccess();
    }
}
