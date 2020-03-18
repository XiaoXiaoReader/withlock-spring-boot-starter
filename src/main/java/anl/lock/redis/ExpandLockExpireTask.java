package anl.lock.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 守护线程实现
 */
public class ExpandLockExpireTask implements Runnable {
    private static Logger log = LoggerFactory.getLogger(ExpandLockExpireTask.class);

    private String lockKey;
    private String lockValue;
    private long expireTime;
    private boolean isRunning;
    private RedisLock redisLock;

    public ExpandLockExpireTask(String lockKey, String lockValue, long expireTime, RedisLock redisLock) {
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.expireTime = expireTime;
        this.redisLock = redisLock;
        this.isRunning = true;
    }

    @Override
    public void run() {
        // 执行周期（过期时间三分之二）
        long waitTime = expireTime * 2 / 3;
        while (isRunning) {
            try {
                Thread.sleep(waitTime);
                if (redisLock.expandLockExpire(lockKey, lockValue, expireTime)) {
                    log.debug("锁续期key：{} ， value：{}", lockKey, lockValue);
                } else {
                    stopTask();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopTask() {
        isRunning = false;
    }

}
