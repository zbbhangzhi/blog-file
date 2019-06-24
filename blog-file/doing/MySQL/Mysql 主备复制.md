Mysql 主备复制

![1551150987140](E:\other\blog-file\doing\MySQL\IMG\mysql主备复制原理.png)

原理：

1. master的DML/DDL操作写入bin-log（二进制日志）
2. slave启动I/O线程去读取bin-log-events，并写入到自身的relay-log（中继日志）里
3. 启动SQL线程执行中继日志内容

