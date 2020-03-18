package anl.lock.aop;

import anl.lock.annotion.WithLock;
import anl.lock.exception.LockException;
import anl.lock.redis.RedisFactory;
import anl.lock.redis.RedisLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 分布式锁AOP
 */
@Aspect
@Component
public class LockAspect {

    private static Logger log = LoggerFactory.getLogger(LockAspect.class);

    @Autowired
    RedisFactory redisFactory;

    @Pointcut("@annotation(anl.lock.annotion.WithLock)")
    public void lockPoint() {

    }

    @Around(value = "@annotation(withLock)", argNames = "pjp, withLock")
    public Object around(ProceedingJoinPoint pjp, WithLock withLock) throws Throwable {
        Class clazz = pjp.getTarget().getClass();
        String methodName = pjp.getSignature().getName();
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        // 请求的方法参数名称
        ParameterNameDiscoverer d = new DefaultParameterNameDiscoverer();
        // 得到参数名列表

        //分布式锁的key
        StringBuilder key = new StringBuilder("lock:");
        if (withLock.key().equals("")) {
            // key=类名+方法名+参数名
            key.append(clazz.getName() + ":").append(methodName + ":");
            String[] parameterNames = d.getParameterNames(ms.getMethod());
            if (parameterNames.length > 0) {
                key.append(handleParams(Arrays.asList(parameterNames)));
            }
        } else {
            key.append(withLock.key());
        }

        RedisLock redisLock = redisFactory.getLock(key.toString(), withLock.expireTime(), withLock.outTimet(),
                withLock.retryTime());
        Object result = null;
        boolean lockSuccess = false;
        try {
            // 获得锁
            lockSuccess = redisLock.tryLock();
            if (lockSuccess) {
                try {
                    //执行方法
                    result = pjp.proceed();
                } catch (Exception e) {
                    log.error("执行业务发生错误,class={} ,method={} ,args={}", clazz.getName(), methodName, pjp.getArgs());
                    throw e;
                }
            } else {
                if (!withLock.errorMessage().equals("")) {
                    log.debug(withLock.errorMessage());
                }
            }
        } catch (Exception e) {
            throw new LockException("分布式锁获取时异常", e);
        } finally {
            if (lockSuccess) {
                if (redisLock.unLock()) {
                    log.debug("释放锁成功,class : {} ,method : {} , key : {}", clazz.getName(), methodName, key);
                } else {
                    log.debug("释放锁失败,class : {} ,method : {} , key : {}", clazz.getName(), methodName, key);
                }
            }
        }
        return result;
    }

    /**
     * 参数名称拼接
     *
     * @param list
     * @return
     */
    private String handleParams(List<String> list) {
        final StringBuffer str = new StringBuffer();
        list.forEach(param -> str.append(param).append("_"));
        str.deleteCharAt(str.length() - 1);
        return str.toString();

    }
}
