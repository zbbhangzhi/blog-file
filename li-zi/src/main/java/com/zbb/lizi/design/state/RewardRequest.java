package com.zbb.lizi.design.state;

/**
 * @author tiancha
 * @since 2020/6/30 15:23
 */
public class RewardRequest {
    private long userId;

    private long orderId;

    public RewardRequest(long userId, long orderId) {
        this.userId = userId;
        this.orderId = orderId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }
}
