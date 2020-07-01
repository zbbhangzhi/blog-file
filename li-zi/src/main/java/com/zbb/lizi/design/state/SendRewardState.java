package com.zbb.lizi.design.state;

import com.zbb.lizi.design.strategy.FactorRewardStrategyFactory;
import com.zbb.lizi.design.strategy.NewUserRewardStrategy;
import com.zbb.lizi.design.strategy.RewardContext;

/**
 * 开始返奖状态
 *
 * @author tiancha
 * @since 2020/6/30 15:19
 */
public class SendRewardState extends RewardState {

    @Override
    void doReward(RewardStateContext context, RewardRequest request) {
        // 经过订单检查后，可以开始返奖了
        sendReward(context, request);
    }

    private void sendReward(RewardStateContext context, RewardRequest request) {
        // 涉及到具体的策略了
        FactorRewardStrategyFactory strategyFactory = new FactorRewardStrategyFactory();
        NewUserRewardStrategy newUserRewardStrategy = (NewUserRewardStrategy)strategyFactory.createStrategy(NewUserRewardStrategy.class);
        RewardContext rewardContext = new RewardContext(newUserRewardStrategy);
        rewardContext.doStrategy(request.getUserId());
        setSuccess(true);
    }
}
