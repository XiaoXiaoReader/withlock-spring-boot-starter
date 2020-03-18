package anl.lock.redis;

import anl.lock.Lock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * redis分布式锁，不支持重入和redis集群
 */
public class RedisLock implements Lock {

    private StringRedisTemplate redisTemplate;
    private ThreadPoolTaskExecutor threadPool;
    private String lockKey;
    private String lockValue;
    //默认锁过期时间 30s
    private long expireTime = 30000L;
    //默认获取锁超时时间 5s
    private long outTime = 5000L;
    //重试时间0.5s,(单位毫秒)
    private long retyTime = 500L;
    private static final String LOCK_SUCCESS = "1";
    //加锁lua
    private static final String TRY_LOCK_SCRIPT = "if redis.call('setNx',KEYS[1],ARGV[1]) then if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('pexpire',KEYS[1],ARGV[2]) else return 0 end end";
    //解锁lua
    private static final String RELEASE_LOCK_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    //续期lua
    private static final String EXPAND_LOCK_SCRIPT = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('pexpire',KEYS[1],ARGV[2]) else return 0 end";

    /**
     * @param lockKey       key
     * @param redisTemplate
     */
    public RedisLock(String lockKey, StringRedisTemplate redisTemplate, ThreadPoolTaskExecutor threadPool) {
        this.lockKey = lockKey;
        this.redisTemplate = redisTemplate;
        this.threadPool = threadPool;
    }

    /**
     * @param lockKey       key
     * @param expireTime    锁过期时间
     * @param redisTemplate
     */
    public RedisLock(String lockKey, long expireTime, StringRedisTemplate redisTemplate, ThreadPoolTaskExecutor threadPool) {
        this(lockKey, redisTemplate, threadPool);
        this.expireTime = expireTime;
    }

    /**
     * @param lockKey       key
     * @param expireTime    过期时间
     * @param outTime       请求超时时间
     * @param retyTime      重试时间间隔
     * @param redisTemplate
     */
    public RedisLock(String lockKey, long expireTime, long outTime, long retyTime, StringRedisTemplate redisTemplate, ThreadPoolTaskExecutor threadPool) {
        this(lockKey, expireTime, redisTemplate, threadPool);
        this.outTime = outTime;
        this.retyTime = retyTime;

    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean lock() {
        //加锁客户端唯一标识,自己实现
        this.lockValue = UUID.randomUUID().toString().replace("-", "");
        Object result = redisTemplate.execute(RedisScript.of(TRY_LOCK_SCRIPT, Long.class), Collections.singletonList(lockKey),
                lockValue, String.valueOf(this.expireTime));

        boolean flag = LOCK_SUCCESS.equals(String.valueOf(result));
        if (flag) {
            //开启守护线程定期检测续锁
            ExpandLockExpireTask expandLockExpireTask = new ExpandLockExpireTask(lockKey, this.lockValue, this.expireTime, this);
            Thread thread = new Thread(expandLockExpireTask);
            thread.setDaemon(true);
            threadPool.execute(thread);
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public boolean tryLock(long tryLockTime, TimeUnit timeUnit) {
        // 是否死循环获取锁
        boolean forever = tryLockTime < 0;
        // 开始获取锁的时间
        final long startTime = System.currentTimeMillis();
        tryLockTime = (tryLockTime < 0) ? 0 : tryLockTime;
        // 统一转为毫秒，阻塞毫秒数
        final Long tryTime = (timeUnit != null) ? timeUnit.toMillis(tryLockTime) : 0;
        // 如果没有加锁成功，循环尝试获取锁
        while (true) {
            // 获取成功 退出
            if (lock()) {
                return Boolean.TRUE;
            }
            //超过了获取锁的最长时间，退出
            if (!forever && System.currentTimeMillis() - startTime - this.retyTime > tryTime) {
                return Boolean.FALSE;
            }
            //重试间隔时间
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(this.retyTime));
        }
    }

    @Override
    public boolean tryLock() {
        final long startTime = System.currentTimeMillis();
        //是否死循环获取锁
        boolean forever = this.outTime < 0;
        this.outTime = (this.outTime < 0) ? 0 : this.outTime;

        //如果没有加锁成功，循环尝试获取锁
        while (true) {
            //获取成功 退出
            if (lock()) {
                return Boolean.TRUE;
            }
            //超过了获取锁的最长时间，退出
            if (!forever && System.currentTimeMillis() - startTime - this.retyTime > this.outTime) {
                return Boolean.FALSE;
            }
            //重试间隔时间
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(this.retyTime));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean unLock() {
        Long result = (Long) redisTemplate.execute(RedisScript.of(RELEASE_LOCK_SCRIPT, Long.class),
                Collections.singletonList(lockKey), lockValue);
        return result != null && result > 0;
    }

    /**
     * 执行lua脚本(锁续期)
     *
     * @param lockKey
     * @param lockValue
     * @param expireTime
     * @return
     */
    public boolean expandLockExpire(String lockKey, String lockValue, Long expireTime) {
        Object result = redisTemplate.execute(RedisScript.of(EXPAND_LOCK_SCRIPT, Long.class), Collections.singletonList(lockKey),
                lockValue, String.valueOf(expireTime));
        return LOCK_SUCCESS.equals(String.valueOf(result));
    }
}
