package anl.lock;

import anl.lock.aop.LockAspect;
import anl.lock.redis.RedisFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@Import({LockAspect.class})
public class WithLockAutoConfiguration {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Bean
    public RedisFactory redisFactory() {
        return new RedisFactory(redisTemplate, asyncServiceExecutor());
    }


    /**
     * redis 守护线程线程池
     *
     * @return
     */
    @Bean(name = "redisExecutor")
    public ThreadPoolTaskExecutor asyncServiceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(7);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-redis-service");
        executor.setKeepAliveSeconds(10);
        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();
        return executor;
    }

}
