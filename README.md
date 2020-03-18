# withlock--spring-boot-starter
redis分布式锁
1. 支持手写代码和方法级别AOP注解使用
2. 支持阻塞获取锁，超时提示（注解方式）
3. 支持锁自动续期
4. 不支持redis集群，有可能会出问题
5. 不支持重入

# 版本
支持springboot2+

# 使用
这里使用定时任务模拟用法
## 下载到本地install引入
```
 <dependency>
      <groupId>alun.cn</groupId>
      <artifactId>withlock--spring-boot-starter</artifactId>
      <version>1.1.0</version>
 </dependency>
```
## 用法
### 手写方式
```
    @Autowired
    RedisFactory redisFactory;
    
    @Scheduled(cron = "0/5 * * * * ?")
    public void locTest1() throws InterruptedException {
        //锁默认时间30秒，这里使用10秒测试续期功能
        Lock lock = redisFactory.getLock("test.key", 10000L);
        if (!lock.tryLock()) {
            return;
        }
        try {
            log.info("locTest1获取锁成功", serverPort);
            //超过三分之二，这里续期一次
            TimeUnit.SECONDS.sleep(7);
        } finally {
            log.info("locTest1释放锁成功");
            lock.unLock();
        }
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void locTest2() throws InterruptedException {
        Lock lock = redisFactory.getLock("test.key", 10000L);
        if (!lock.tryLock()) {
            return;
        }
        try {
            log.info("locTest2获取锁成功");
            //不续期
            TimeUnit.SECONDS.sleep(5);
        } finally {
            log.info("locTest2释放锁成功");
            lock.unLock();
        }
    }
```
### 注解方式
设置yml日志级别查看运行日志

```
logging:
  level:
    anl.lock: debug
```

```
  @Scheduled(cron = "0/5 * * * * ?")
  @WithLock(key = "test.key", expireTime = 10000)
  public void locTest1() throws InterruptedException {
      log.info("locTest1获取锁成功");
      //续期一次
      TimeUnit.SECONDS.sleep(7);

  }

  @Scheduled(cron = "0/5 * * * * ?")
  @WithLock(key = "test.key", expireTime = 10000, outTimet = 5, errorMessage = "locTest2请求超时")
  public void locTest2() throws InterruptedException {
      log.info("locTest2获取锁成功");
      TimeUnit.SECONDS.sleep(5);
```

# 最后
欢迎指出错误（‐＾▽＾‐）

