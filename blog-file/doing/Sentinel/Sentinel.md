## Sentinel

### 使用

Spring：以@SentinelResource注解在service方法上实现方法粒度的限流

Apollo：加载Apollo的限流配置到FlowRuleManager

adminPhone

### 问题

1. 上下文context和资源resource的关系

### 组件

##### Entry

其实是个表面抽象

##### node

DefaultNode：保存着某个resource在某个context中的实时指标，每个DefaultNode都指向一个ClusterNode

ClusterNode：保存着某个resource在所有的context中实时指标的总和，同样的resource会共享同一个ClusterNode，不管他在哪个context中

##### Slot

意为插槽，一堆slot组成调用链（next）实现限流降级等功能，fireEntry触发下一个节点调用entry方法，以此类推下去。

NodeSelectorSlot 负责收集资源的路径，并将这些资源的调用路径，以树状结构存储起来，用于根据调用路径来限流降级；

ClusterBuilderSlot 则用于存储资源的统计信息以及调用者信息，例如该资源的 RT, QPS, thread count 等等，这些信息将用作为多维度限流，降级的依据；

StatistcSlot 则用于记录，统计不同纬度的 runtime 信息；

FlowSlot 则用于根据预设的限流规则，以及前面 slot 统计的状态，来进行限流；

AuthorizationSlot 则根据黑白名单，来做黑白名单控制；

DegradeSlot 则通过统计信息，以及预设的规则，来做熔断降级；

SystemSlot 则通过系统的状态，例如 load1 等，来控制总的入口流量；

![slot链路](E:\other\blog-file\doing\Sentinel\slot链路.png)

InitFunc



### 入门

1. 初始化限流规则

加载限流规则到内存中(重启丢失)

```java
//初始化规则
private void initRule(){
    FlowRule rule1 = new FlowRule()
                .setResource("test-another-sync-in-async")//接口方法的全限定名
                .setLimitApp("originB")//应用名
                .as(FlowRule.class)
                .setCount(4)//限流阈值计数
                .setGrade(RuleConstant.FLOW_GRADE_QPS);//限流类型 0线程数 1QPS
    List<FlowRule> ruleList = Arrays.asList(rule1);
    //添加规则到配置 （FlowRuleManager中维护的配置提供更新通知功能）
    FlowRuleManager.loadRules(ruleList);
}
```





#### slot

###### NodeSelectorSlot

为当前上下文收集资源，建立调用树，添加子节点DefaultNode

###### ClusterBuilderSlot

存储资源数据，包括rt，qps，线程数，异常等；怎么获取的？？？

###### StatisticSlot

先触发后面的slot再开始计算通过的请求线程，实时统计资源信息如clusterNode，调用/引用方的clusterNode，特殊上下文的特殊资源信息defaultNode，所有入口的统计和

###### 系统指标的计算：







----------------------------------------------------------block slot分割线--------------------------------------------------------

###### FlowSlot

结合前三个slot的统计数据，根据用户自定义的限流rule实现passCheck，passCheck分为cluster和local。

###### AuthoritySlot

黑白名单比较请求的origin（服务名/源IP）

###### DegradeSlot

降级依据：平均rt/报错（block？）频率

###### SystemSlot

仅检查入站请求，根据系统的qps，最大线程数，rt，最大系统负载限制；且启动定时线程任务记录系统运行状况到日志



#### rule

###### 限流等规则是动态更新的

XXXManager内部维护一个监听器实现PropertyListener接口，当dataResource调用loadAndUpdateRules()方法以更新rules，实际是调用Sentinel的updateValue方法触发所有系统维护的监听器的configUpdate方法以更新各自slot的RuleManager维护的rules