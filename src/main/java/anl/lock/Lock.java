package anl.lock;

import java.util.concurrent.TimeUnit;

public interface Lock {

    /**
     * 获取锁
     *
     * @return
     */
    boolean lock();

    /**
     * 阻塞获取锁，阻塞指定时间后返回
     *
     * @param tryLockTime 阻塞时间（ 小于0会一直阻塞慎用）
     * @param timeUnit
     * @return
     */
    boolean tryLock(long tryLockTime, TimeUnit timeUnit);

    /**
     * 阻塞获取锁，默认阻塞5秒
     *
     * @return
     */
    boolean tryLock();

    /**
     * 释放锁
     */
    boolean unLock();
}
