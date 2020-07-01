package com.zbb.lizi.design.state;

/**
 * @author tiancha
 * @since 2020/6/30 15:17
 */
public abstract class RewardState {

    private boolean success;

    abstract void doReward(RewardStateContext context, RewardRequest request);

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
