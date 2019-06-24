Druid解析-连接

![1551062872450](E:\other\blog-file\doing\druid\IMG\DataSource-Diagram.png)

##### 使用示例：

1. 面向SpringBoot的自动配置：DruidDataSourceWrapper继承DruidDataSource，提供自动配置属性功能

   ```java
   @ConfigurationProperties("spring.datasource.druid")
   class DruidDataSourceWrapper extends DruidDataSource implements InitializingBean {
       @Autowired
       private DataSourceProperties basicProperties;//从属性文件读取
   
       @Override
       public void afterPropertiesSet() throws Exception {
           //属性赋值：调用DruidAbstractDataSource的方法
           if (super.getUsername() == null) {
               super.setUsername(basicProperties.determineUsername());
           }
       }
   }
   ```



创建数据库连接DruidAbstractDataSource::createPhysicalConnection



##### 数据库连接

DataSource：

```java
//用于连接物理数据库的工厂，是DriverManager的替代，其更倾向用于获得连接
public interface DataSource  extends CommonDataSource, Wrapper {
    //尝试与数据库建立一个连接
    Connection getConnection() throws SQLException;
}
```

DruidDataSource：

```java
public class DruidDataSource extends DruidAbstractDataSource implements DruidDataSourceMBean, ManagedDataSource, Referenceable, Closeable, Cloneable, ConnectionPoolDataSource, MBeanRegistration {
    //存储数据库连接
    private volatile DruidConnectionHolder[] connections;
    //断开的数据库连接
    private DruidConnectionHolder[] 	  	 evictConnections;
    //保持存活的连接
    private DruidConnectionHolder[]          keepAliveConnections;
    //连接创建线程
    private CreateConnectionThread           createConnectionThread;
    //实现DataSource的getConnection接口
    @Override
    public Connection getConnection(String username, String password) throws SQLException{
        if (this.username == null && this.password == null
                && username != null && password != null) {
            this.username = username;
            this.password = password;
            return getConnection();
        }
        if (!StringUtils.equals(username, this.username)) {
            throw new UnsupportedOperationException("Not supported by DruidDataSource");
        }
        if (!StringUtils.equals(password, this.password)) {
            throw new UnsupportedOperationException("Not supported by DruidDataSource");
        }
        return getConnection();
    }
    @Override
    public DruidPooledConnection getConnection() throws SQLException {
        return getConnection(maxWait);
    }
    public DruidPooledConnection getConnection(long maxWaitMillis) throws SQLException {
        init();
		//过滤链：内部最后还是调用了getConnectionDirect
        if (filters.size() > 0) {
            FilterChainImpl filterChain = new FilterChainImpl(this);
            return filterChain.dataSource_connect(this, maxWaitMillis);
        } else {
            return getConnectionDirect(maxWaitMillis);
        }
    }
    //初始化连接参数 创建连接数
    public void init() throws SQLException {
        if (inited) {
            return;
        }
        // bug fixed for dead lock, for issue #2980
        DruidDriver.getInstance();
        final ReentrantLock lock = this.lock;
        try {
            //优先响应中断而不是获取锁 TODO 比较其与lock区别
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new SQLException("interrupt", e);
        }
        boolean init = false;
        try {
            if (inited) {
                return;
            }
			//获取当前线程的堆栈跟踪元素信息
            initStackTrace = Utils.toString(Thread.currentThread().getStackTrace());
			//记录数据源标记id
            this.id = DruidDriver.createDataSourceId();
            if (this.id > 1) {
                long delta = (this.id - 1) * 100000;
                this.connectionIdSeedUpdater.addAndGet(this, delta);
                this.statementIdSeedUpdater.addAndGet(this, delta);
                this.resultSetIdSeedUpdater.addAndGet(this, delta);
                this.transactionIdSeedUpdater.addAndGet(this, delta);
            }
            //..... 省略一堆属性设置
            connections = new DruidConnectionHolder[maxActive];
            evictConnections = new DruidConnectionHolder[maxActive];
            keepAliveConnections = new DruidConnectionHolder[maxActive];
            SQLException connectError = null;
            //是否异步创建连接
            if (createScheduler != null && asyncInit) {
                for (int i = 0; i < initialSize; ++i) {
                    createTaskCount++;
                    CreateConnectionTask task = new CreateConnectionTask(true);
                    this.createSchedulerFuture = createScheduler.submit(task);
                }
            } else if (!asyncInit) {
                // 循环创建物理连接 并放入连接数组
                while (poolingCount < initialSize) {
                    try {
                        //调用父类DruidAbstractDataSource的方法
                        PhysicalConnectionInfo pyConnectInfo = createPhysicalConnection();
                        DruidConnectionHolder holder = new DruidConnectionHolder(this, pyConnectInfo);
                        connections[poolingCount++] = holder;
                    } catch (SQLException ex) {
                        LOG.error("init datasource error, url: " + this.getUrl(), ex);
                        if (initExceptionThrow) {
                            connectError = ex;
                            break;
                        } else {
                            Thread.sleep(3000);
                        }
                    }
                }
                if (poolingCount > 0) {
                    poolingPeak = poolingCount;
                    poolingPeakTime = System.currentTimeMillis();
                }
            }
            createAndLogThread();
            //创建并开启创建线程 并用CountDownLatch计数
            createAndStartCreatorThread();
            //创建并开启毁坏线程 并用CountDownLatch计数
            createAndStartDestroyThread();
			//等到创建线程和毁坏线程都启动好 才往下执行
            initedLatch.await();
            init = true;
            initedTime = new Date();
            registerMbean();
            if (connectError != null && poolingCount == 0) {
                throw connectError;
            }
			//如果需要保证一直有最小活跃数 则循环创建线程填充至最小空闲数
            if (keepAlive) {
                // async fill to minIdle
                if (createScheduler != null) {
                    for (int i = 0; i < minIdle; ++i) {
                        createTaskCount++;
                        CreateConnectionTask task = new CreateConnectionTask(true);
                        this.createSchedulerFuture = createScheduler.submit(task);
                    }
                } else {
                    //唤醒所有等待线程 TODO lock.newCondition()
                    this.emptySignal();//内部调用(Condition)empty.signal();
                }
            }
        //.... 省略一堆报错设置，解锁等
    }
}
```

DruidAbstractDataSource

```java
public abstract class DruidAbstractDataSource extends WrapperAdapter implements DruidAbstractDataSourceMBean, DataSource, DataSourceProxy, Serializable {
    protected volatile boolean defaultAutoCommit = true;
    protected volatile String username;
    protected volatile String password;
    protected volatile String jdbcUrl;
    protected volatile String driverClass;
    protected volatile ClassLoader driverClassLoader;
    protected volatile Properties connectProperties = new Properties();
    protected volatile int initialSize = DEFAULT_INITIAL_SIZE;
    protected volatile int maxActive = DEFAULT_MAX_ACTIVE_SIZE;
    protected volatile int minIdle = DEFAULT_MIN_IDLE;
    protected volatile int maxIdle = DEFAULT_MAX_IDLE;
    protected volatile long maxWait = DEFAULT_MAX_WAIT;
    protected int notFullTimeoutRetryCount = 0;
    protected Driver driver;
    //创建物理连接
    public PhysicalConnectionInfo createPhysicalConnection() throws SQLException {
        //..... 省略组装physicalConnectProperties
        Connection conn = null;
        long connectStartNanos = System.nanoTime();
        long connectedNanos, initedNanos, validatedNanos;
        Map<String, Object> variables = initVariants
                ? new HashMap<String, Object>()
                : null;
        Map<String, Object> globalVariables = initGlobalVariants
                ? new HashMap<String, Object>()
                : null;
        createStartNanosUpdater.set(this, connectStartNanos);
        creatingCountUpdater.incrementAndGet(this);
        try {
            //内部调用驱动类driver创建线程
            conn = createPhysicalConnection(url, physicalConnectProperties);
            connectedNanos = System.nanoTime();
            if (conn == null) {
                throw new SQLException("connect error, url " + url + ", driverClass " + this.driverClass);
            }
            initPhysicalConnection(conn, variables, globalVariables);
            initedNanos = System.nanoTime();

            validateConnection(conn);
            validatedNanos = System.nanoTime();

            setFailContinuous(false);
            setCreateError(null);
        } catch (SQLException ex) {
            setCreateError(ex);
            JdbcUtils.close(conn);
            throw ex;
        } catch (RuntimeException ex) {
            setCreateError(ex);
            JdbcUtils.close(conn);
            throw ex;
        } catch (Error ex) {
            createErrorCountUpdater.incrementAndGet(this);
            setCreateError(ex);
            JdbcUtils.close(conn);
            throw ex;
        } finally {
            long nano = System.nanoTime() - connectStartNanos;
            createTimespan += nano;
            creatingCountUpdater.decrementAndGet(this);
        }
        return new PhysicalConnectionInfo(conn, connectStartNanos, connectedNanos, initedNanos, validatedNanos, variables, globalVariables);
    }
     public Connection createPhysicalConnection(String url, Properties info) throws SQLException {
        Connection conn;
        if (getProxyFilters().size() == 0) {
            //通过驱动driver类创建连接
            conn = getDriver().connect(url, info);
        } else {
            conn = new FilterChainImpl(this).connection_connect(info);
        }
        createCountUpdater.incrementAndGet(this);
        return conn;
    }
}
```



##### SQL语句执行处理

DuidPooledConnection实现Connection接口

```java
//与某个特定数据库的连接会话，用于处理sql语句的执行和返回结果
public interface Connection  extends Wrapper, AutoCloseable {
    //为sql语句创建参数化对象
    PreparedStatement prepareStatement(String sql) throws SQLException;
    //根据给定的结果对象和结果同步类型 为sql生成参数化对象
    PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException;
    //为调用数据库存储过程创建可回执的对象
    CallableStatement prepareCall(String sql) throws SQLException;
    //为发送给数据库的sql语句创建声明对象
    Statement createStatement() throws SQLException;
    //将给定的sql语句转化为系统支持的语句
    String nativeSQL(String sql) throws SQLException;
    //设置自动提交模式：在这个模式下所有的sql作为单独的事务执行和提交；反之，sql语句将会被分组提交TODO
    void setAutoCommit(boolean autoCommit) throws SQLException;
    //使上一次的提交/回滚语句成功执行，同时释放被当前连接持有的数据库锁；禁用自动提交模式下使用
    void commit() throws SQLException;
    //回滚所有当前事务造成的改变，同时释放被当前连接持有的数据库锁；
    void rollback() throws SQLException;
    Savepoint setSavepoint(String name) throws SQLException;
    //更改当前连接对象的事务隔离级别为给定级别
    void setTransactionIsolation(int level) throws SQLException;
    //获取当前连接对象的事务隔离级别
	int getTransactionIsolation() throws SQLException;
    void close() throws SQLException;
```