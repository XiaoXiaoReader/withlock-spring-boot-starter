package anl.lock.annotion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithLock {

    /**
     * redis锁的key前缀 如果为空，则默认为类名+方法名+参数
     *
     * @return
     */
    String key() default "";

    /**
     * 锁过期时间默认30秒（单位毫秒）
     *
     * @return
     */
    long expireTime() default 30000L;

    /**
     * 请求锁的超时时间，默认0秒(单位：毫秒)
     *
     * @return
     */
    long outTimet() default 0L;

    /**
     * 重试间隔时间，默认0.5秒(单位：毫秒)
     *
     * @return
     */
    long retryTime() default 500L;

    /**
     * 获取锁失败时候的失败提示
     *
     * @return
     */
    String errorMessage() default "";

}
