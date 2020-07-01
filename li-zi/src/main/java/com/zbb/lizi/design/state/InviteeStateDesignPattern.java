package com.zbb.lizi.design.state;

/**
 * @author tiancha
 * @since 2020/6/30 15:10
 */
public class InviteeStateDesignPattern {

    /**
     * 状态模式：根据不同状态执行不同动作，并流转向下个状态（帮助我们对系统状态及状态之间流转进行统一管理和扩展）
     *
     * 根据状态进行返奖逻辑流转
     * 好多if-else
     */

    public static void main(String[] args) {
        long userId = 1;
        long orderId = 2;
        RewardRequest request = new RewardRequest(userId, orderId);
        RewardStateContext context = new RewardStateContext();
        context.setRewardState(new OrderCheckState());
        // 从订单检查开始返奖状态流转
        context.echo(context, request);
        if (context.isResultFlag()){
            context.setRewardState(new PreSendRewardState());
            context.echo(context, request);
        }else {
            // 订单校验失败，进入返奖失败状态
            context.setRewardState(new FailRewardState());
            context.echo(context, request);
        }
        // 预返奖流程成功，进入返奖流程
        if (context.isResultFlag()){
            context.setRewardState(new SendRewardState());
            context.echo(context, request);
        }else {
            // 预返奖流程失败，进入返奖失败状态
            context.setRewardState(new FailRewardState());
            context.echo(context, request);
        }
        // ......
    }
}
