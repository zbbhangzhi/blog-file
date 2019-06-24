dubbo filter/listener注入

1. 先找到Filter接口，filter支持SPI扩展，filter可用在invoker/exporter
2. 既然知道了Filter有扩展特性，那么通过SPI的机制，直接从配置文件开始

![1557193710400](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1557193710400.png)

3. 找到ProtocolFilterWrapper的buildInvokerChain方法，通过阅读方法代码可得知invoker的执行过程实际是一条责任链的调用过程
4. 在protocol的invoker/exporter方法中找到buildInvokerChain的调用，可以确定filter的作用点

```java
@Override
public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
    if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
        return protocol.export(invoker);
    }
    return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
}

@Override
public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
    if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
        return protocol.refer(type, url);
    }
    return buildInvokerChain(protocol.refer(type, url), Constants.REFERENCE_FILTER_KEY, Constants.CONSUMER);
}
```

5. 但说实话我是不知道ProtocolFilterWrapper这个类什么时候被注入的。

   ProtocolFilterWrapper实现了Protocol，并通过暴力搜索，发现在ReferenceConfig的注释中包含ProtocolFilterWrapper。

   ![1557194663119](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1557194663119.png)

   ReferenceConfig意为引用配置，即消费端消费配置，那么refProtocol就是引用协议。这时候猜测一下ProtocolFilterWrapper在这里的含义，wrapper一般都代表包装类，而引用协议是根据url得出的，通过代码和注释可得知，refProtocol是ExtensionLoader根据Protocol的类型获取的。从代码入手，可以看到几个过程

   - ExtensionLoader对扩展类资源（META-INF 下）解析并添加到缓存

   

   