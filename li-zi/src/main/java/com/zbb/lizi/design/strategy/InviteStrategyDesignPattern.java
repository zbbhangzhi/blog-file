package com.zbb.lizi.design.strategy;

/**
 * @author tiancha
 * @since 2020/6/30 10:49
 */
public class InviteStrategyDesignPattern {

    /**
     * 外卖营销返奖流程：
     * 1. 责任链判断该用户是否满足返奖条件：是否使用红包，订单金额是否满足
     * 2. 满足条件：添加到延时队列，经过T天后处理该延时消息，判断用户是否退款
     * 3. 用户未退款：开始返奖流程
     * 4. 返奖金额计价：不同用户不同计价规则
     * 5. 结算用户奖励：统一的返奖结算
     * 6. 返奖失败：返奖补偿，再次进行返奖
     * 7. 流程结束
     *
     * 返奖流程是符合开闭原则+单一职责原则的，返奖计价是开，结算是闭；整个流程使用到：责任链模式+策略模式+工厂模式（感觉工厂模式可有可无）
     */
    public static void main(String[] args) {
        // 预先判断是否满足条件
        RuleEngine ruleEngine = new RuleEngine();
        ruleEngine.invokeAll(new Object());
        // 开始计价
        FactorRewardStrategyFactory strategyFactory = new FactorRewardStrategyFactory();
        Invitee invitee = new Invitee(1);
        // 新用户
        if (invitee.getUserType() == 1){
            NewUserRewardStrategy newUserRewardStrategy = (NewUserRewardStrategy)strategyFactory.createStrategy(NewUserRewardStrategy.class);
            RewardContext rewardContext = new RewardContext(newUserRewardStrategy);
            rewardContext.doStrategy(invitee.getUserId());
        }
        // 老用户
        if (invitee.getUserType() == 2){}
    }
}
