Dubbo源码阅读之概览

【

模块里api是基本实现用于拓展的，

通过官方文档里的成熟度可了解现有feature

dubbo在生产环境中的应用有两种方式，直接XXXConfig配置API进入，或者Spring的xml文件/注解进入；所以主要从spring入手

服务引用提供延迟加载

api配置方式为什么不建议直接使用，ReferenceConfig。。。

集群容错，容错方案，

protocol处理消费请求的协议，定义线程池配置，定义分派策略

consumer异步执行CompletableFuture

】

模块组件

- configCenter动态配置中心

  - 职责：

    1. configCenter外部化配置：将配置存储外部化（disconf....，支持优先级

    2. 服务治理

配置加载

- 注册中心配置RegistryConfig
- 服务配置ServiceConfig，解析由ProxyFactory生成代理类
- 引用配置ReferenceConfig
- 本地配置dubbo.properties
- JVM system properties
- 扩展配置

服务暴露

- 服务注册(服务均已Node形式存在，以Zookeeper为注册中心为例)，李涛zookeeper起到的作用和作用过程
  - 多协议多注册中心导出服务
    - 解析注册中心地址List< RegistryConfig >
    - 将服务以当前协议Protocol注册到注册中心RegistryService
  - Transporter

- 服务订阅





服务引用



