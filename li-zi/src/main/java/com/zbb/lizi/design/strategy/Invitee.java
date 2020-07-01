package com.zbb.lizi.design.strategy;

/**
 * @author tiancha
 * @since 2020/6/30 14:30
 */
public class Invitee {
    private long userId;

    private int userType;

    public Invitee(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public int getUserType() {
        return userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
