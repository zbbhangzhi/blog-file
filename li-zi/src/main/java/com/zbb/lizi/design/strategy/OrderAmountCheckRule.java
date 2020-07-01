package com.zbb.lizi.design.strategy;

/**
 * 当前订单金额是否满足条件
 *
 * @author tiancha
 * @since 2020/6/30 16:09
 */
public class OrderAmountCheckRule extends BasicRule {

    @Override
    public boolean evaluate(Object context) {
        return false;
    }

    @Override
    public void execute() {

    }
}

