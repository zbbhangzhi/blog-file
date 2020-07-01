package com.zbb.lizi.design.strategy;

/**
 * 判断当前订单是否使用红包
 *
 * @author tiancha
 * @since 2020/6/30 16:08优惠券
 */
public class CouponUsedCheckRule extends BasicRule {
    @Override
    public boolean evaluate(Object context) {
        return false;
    }

    @Override
    public void execute() {

    }
}
