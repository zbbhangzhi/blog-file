Dubbo源码阅读之配置

API版本

例子

```xml
<!-- Provider -->
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:dubbo="http://dubbo.apache.org/schema/dubbo"
       xmlns="http://www.springframework.org/schema/beans"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://dubbo.apache.org/schema/dubbo http://dubbo.apache.org/schema/dubbo/dubbo.xsd">
    <dubbo:application name="demo-provider"/>
    <dubbo:registry address="zookeeper://127.0.0.1:2181"/>
    <dubbo:protocol name="dubbo" port="20890"/>
    <bean id="demoService" class="org.apache.dubbo.samples.basic.impl.DemoServiceImpl"/>
    <dubbo:service interface="org.apache.dubbo.samples.basic.api.DemoService" ref="demoService"/>
</beans>
```

注册中心配置

RegistryConfig，由ServiceConfig注入，



服务暴露配置

配置方式

- API声明ServiceConfig

- 注解@Service

- spring XML文件

  XML文件配置好ServiceBean，由源码可知ServiceBean实现了ApplicationListener的事件响应方法onApplicationEvent()，该方法会收到spring的上下文刷新事件后执行服务导出功能

服务引用配置

ReferenceConfig