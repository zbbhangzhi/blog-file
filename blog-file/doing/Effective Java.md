1. 静态工厂方法替代构造器
   - 静态工厂方法有名字
   - 可用于返回相同对象的重复调用，不必每次都创建
   - 可以自定义返回对象为子类型

```java
private final static Person INSTANCE = new Person();
private Person(){}
public static Person newInstance(){
	return new Person();
}
public static Person getInstance(){
	return INSTANCE;
}
```

2. 多构造器参数时可以使用构建器
   - JavaBeans方式（setter参数）多个调用使得对象处于不一致的状态，出现线程安全问题
   - 重叠构造器参数过多不够清晰
   - Builder构建模式，结构清晰且安全，但它创建对象前需要先创建它的构建器，代码冗长。

5. 尽可能减少对象的创建，可以使用静态方法或对象池，尽量使用基本类型减少装箱基本类型带来的开销
6. ***清除过期对象的引用，因为可能造成内存泄漏：过期对象，数组中被丢弃的对象；缓存，可以使用WeakHashMap代表缓存；监听器和回调，客户端注册回调却没有显示的取消注册，需要确保回调立即被当作垃圾回收  TODO
7. 

































