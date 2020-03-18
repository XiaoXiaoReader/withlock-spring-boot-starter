package anl.lock.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class RedisFactory {

    private StringRedisTemplate redisTemplate;

    private ThreadPoolTaskExecutor threadPool;

    public RedisFactory(StringRedisTemplate redisTemplate, ThreadPoolTaskExecutor threadPool) {
        this.redisTemplate = redisTemplate;
        this.threadPool = threadPool;
    }

    /**
     * @param lockKey
     * @return
     */
    public RedisLock getLock(String lockKey) {
        return new RedisLock(lockKey, redisTemplate, threadPool);
    }

    /**
     * @param lockKey
     * @param expireTime 锁过期时间
     * @return
     */
    public RedisLock getLock(String lockKey, long expireTime) {
        return new RedisLock(lockKey, expireTime, redisTemplate, threadPool);
    }

    /**
     * @param lockKey
     * @param expireTime 过期时间
     * @param outTime    请求超时阻塞时间（ 小于0会一直阻塞慎用）
     * @param retyTime   重试时间间隔
     * @return
     */
    public RedisLock getLock(String lockKey, long expireTime, long outTime, long retyTime) {
        return new RedisLock(lockKey, expireTime, outTime, retyTime, redisTemplate, threadPool);
    }
}
