## JAVA_OPT理解及调优理论

### CMS

以RocketMQ中runserver.cmd为例，这是启动NameSrv的命令行文件

```cmd
set "JAVA_OPT=%JAVA_OPT% -server -Xms2g -Xmx2g -Xmn1g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=320m"
set "JAVA_OPT=%JAVA_OPT% -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=70 -XX:+CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8 -XX:-UseParNewGC"
set "JAVA_OPT=%JAVA_OPT% -verbose:gc -Xloggc:"%USERPROFILE%\rmq_srv_gc.log" -XX:+PrintGCDetails"
set "JAVA_OPT=%JAVA_OPT% -XX:-OmitStackTraceInFastThrow"
set "JAVA_OPT=%JAVA_OPT% -XX:-UseLargePages"
set "JAVA_OPT=%JAVA_OPT% -Djava.ext.dirs=%BASE_DIR%lib"
set "JAVA_OPT=%JAVA_OPT% -cp "%CLASSPATH%""

"%JAVA%" %JAVA_OPT% %*
```

#### -server 

-sever：在多个CPU时可优化性能，启动慢运行快

-client：默认模式，启动快不适合长时间运行

-Xms和-Xmx设置为 FullGC之后的老年代内存占用的3-4倍

-Xms：初始heap大小，使用的最小内存，CPU性能高时此值应设大一些；一般和Xmx一样，避免每次gc后JVM重新分配内存

-Xmx：heap的最大值

-Xmn：为年轻代配置一个大堆（可以并行收集），再次利用大内存系统。它有助于防止短期对象过早地被提升到旧一代，垃圾收集更加昂贵。

#### -XX:MetaspaceSize  -XX:MaxMetaspaceSize

前言：Metaspace即元数据空间，1.8之后用来替代Perm永久代的。perm是用来存储klass（class文件在jvm里的运行时数据）等信息的；jvm在启动时会根据配置的-XX:MaxPermSize及-XX:PermSize来分配一块连续的内存块，但随之动态类加载增加，这块内存如果小了就会出现内存溢出，多了又会浪费内存。所以出现了元数据区Metaspace希望能解决内存管理的问题。

Metaspace由Klass Metaspace和NoKlass Metaspace组成。这两块均是由类加载共享的。当类加载时，其类加载器拥有的SpaceManager管理属于自己内存空间，这样很容易将Klass Metaspace使用完，即出现OOM，不过一般情况下不会，NoKlass Mestaspace是由一块块内存慢慢组合起来的，在没有达到限制条件的情况下，会不断加长这条链，让它可以持续工作。

MetaspaceSize：用于控制metaspaceGC发生的初始阈值，默认是20.8M左右

MaxMetaspaceSize：默认基本是无限大，但如果不设置metaspace是会被无限使用的，如果设置了数值，与MaxPermSize不同，在jvm启动时，不会创建很大的Metaspace，但Permspace会。

#### -XX:+UseConcMarkSweepGC

使用CMS收集器，通过多线程并发进行垃圾回收，尽量减少垃圾回收带来的停顿。

##### -XX:+UseCMSCompactAtFullCollection

CMS收集器的一个开关，默认为true；用于在Full GC（不是CMS并发GC）之后增加一个碎片的管理过程；因为CMS是基于“标记-清除”算法实现的收集器，这种算法会导致大量的碎片，如果遇到给大对象分配内存空间时，可能会出现找不到连续的空间来分配而不得不提前触发Full GC

##### -XX:CMSInitiatingOccupancyFraction=70

指设定CMS在对内存占用率达到70%时开始GC，默认是68%，因为CMS的垃圾算法会产生碎片所以一般提早GC

##### -XX:+CMSParallelRemarkEnabled

开启并行标记，减少停顿的时间

##### -XX:+CMSClassUnloadingEnabled

只有在使用CMS垃圾收集器的情况下，表示在使用CMS垃圾回收时是否启动类卸载功能，默认不启用，如果启用，垃圾回收就会清除持久代，移除不再使用的class

#### -XX:SoftRefLRUPolicyMSPerMB

因为soft reference软引用对象只有在垃圾回收的时候才被清除，但垃圾回收不总是执行。所以设置对象存货的毫秒数，这里为0即不被引用就清除

#### -XX:SurvivorRatio=8 

表示新生代的eden区:from区:to区= 8:1:1

#### -XX:-UseParNewGC

关闭年轻代的并行GC，因为当前使用-XX:+UseConcMarkSweepGC，所以-XX:UseParNewGC默认是开启的。

#### -verbose:gc

用于垃圾收集时的信息打印，和-XX:+PrintGC功能相同

#### -XX:-OmitStackTraceInFastThrow

关闭JVM优化抛出堆栈异常：对于一些频繁抛出的异常，jdk会为了性能而做出优化，jit重编译后会抛出没有堆栈的异常；在-sever模式下，该优化是默认开启的。也就是说JIT重新编译前，log中是能看见旧有堆栈的异常。

#### -XX:-UseLargePages

不使用内存分页；因为现代CPU是通过寻址来访问内存的。现引入了MMU（内存管理单元）用来解决比如32位的CPU的物理内存为4G，一般可用小于4G，当程序需要使用4G内存时，程序不得不降低内存占用。

而内存分页就是在MMU基础上提出的一种内存管理机制。它将虚拟地址和物理地址按照固定的大小分割成页和页帧，并保证页与页帧大小相同，这种机制保证访问内存的高效性，并使系统支持使用非连续的内存空间。这时候物理地址与虚拟地址需要同过映射来寻址，而映射关系存储在页表中，而页表存在于内存中。因为CPU通过总线访问存在于内存中页表效率仍旧不高，所以引入TLB页表缓冲寄存器，用来缓冲部分经常访问的页表内容。而我们设置的内存分页就是页表缓冲寄存器是否能支持大内存分页。如果启用，则通过设置-XX:LargePageSizeInBytes=10m设置大小

### G1

以RocketMQ中runbroker.cmd为例，这是启动Broker的命令行文件

```cmd
set "JAVA_OPT=%JAVA_OPT% -server -Xms2g -Xmx2g -Xmn1g"
set "JAVA_OPT=%JAVA_OPT% -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:G1ReservePercent=25 -XX:InitiatingHeapOccupancyPercent=30 -XX:SoftRefLRUPolicyMSPerMB=0 -XX:SurvivorRatio=8"
set "JAVA_OPT=%JAVA_OPT% -verbose:gc -Xloggc:%USERPROFILE%\mq_gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCApplicationStoppedTime -XX:+PrintAdaptiveSizePolicy"
set "JAVA_OPT=%JAVA_OPT% -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=30m"
set "JAVA_OPT=%JAVA_OPT% -XX:-OmitStackTraceInFastThrow"
set "JAVA_OPT=%JAVA_OPT% -XX:+AlwaysPreTouch"
set "JAVA_OPT=%JAVA_OPT% -XX:MaxDirectMemorySize=15g"
set "JAVA_OPT=%JAVA_OPT% -XX:-UseLargePages -XX:-UseBiasedLocking"
set "JAVA_OPT=%JAVA_OPT% -Djava.ext.dirs=%BASE_DIR%lib"
set "JAVA_OPT=%JAVA_OPT% -cp %CLASSPATH%"
```

#### -XX:+UseG1GC

G1垃圾收集器，jdk1.7后出现，为了代替CMS收集器。G1收集器是一个并行的，并发的，增量式压缩停顿短暂的垃圾收集器。G1与其他收集器不一样，不区分年轻代和老年代空间。被用于多处理器和大容量内存的环境，且整理空闲空间更快。

相比较于CMS收集器，G1垃圾收集的同时整理内存，不会产生很多内存碎片。G1的停顿时间可预测，用户可以在指定期望时间停顿。

##### -Xmn

设置年轻代大小会干预G1，无法根据需要增大或缩小年轻代的大小。

##### -XX:G1HeapRegionSize=16m

Region大小。其他GC将连续的内存空间划分为新生代，老年代和永久代（元空间），但G1根据其垃圾收集机制会将每一代用多个不连续相同大小的区域称为region存储，除了为三代划分空间，G1还新提供存储巨大对象的功能-H-objs，为减少H-objs分配对GC的影响，需要把大对象变为普通对象建议增大Region size。G1HeapRegionSize的取值范围2^0--2^6M，如果不设定G1将根据heap大小自动决定。

##### -XX:G1ReservePercent=25

增大堆内存大小，默认是10，因为JVM在回收存活或晋升对象的时候，栈区溢出就会发生失败，因为堆的使用已经到达极限。

##### -XX:MaxGCPauseMillis=200

设置目标最大暂停时间，JVM会尽可能完成它，但不一定实现；如果设置了-Xmn说明禁用目标暂停时间

##### -XX:InitiatingHeapOccupancyPercent=30

整个堆栈使用达到百分之30的时候，启动GC周期，0表示一直GC

##### -XX:+PrintGCDetails

堆栈溢出异常，可以用于打印to-space overflow日志

#### -XX:+UseGCLogFileRotation 

打开GC日志滚动记录功能，默认为0（即不限制）

##### -XX:NumberOfGCLogFiles=5 

日志文件数量

##### -XX:GCLogFileSize=30m

日志文件大小

#### -XX:+AlwaysPreTouch

在调用main函数之前，使用所有可用的内存分页，这个选项可用于测试长时间运行的系统

#### -XX:-UseBiasedLocking

JVM默认启用偏向锁，但竞争激烈时，偏向锁会增加系统负担（锁会偏向当前已经占用锁的线程）



<https://www.cnblogs.com/zuoxiaolong/p/jvm8.html>



<https://www.cnblogs.com/zuoxiaolong/p/jvm9.html>