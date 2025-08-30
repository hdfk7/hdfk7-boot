package cn.hdfk7.boot.starter.common.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
public class AsyncComponent {

    @Bean
    public TaskExecutor shortTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int core = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(core * 2 + 1);
        executor.setKeepAliveSeconds(60);
        executor.setAwaitTerminationMillis(1000L * 60);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("short-task-executor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskExecutor longTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int core = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(core * 10 + 1);
        executor.setKeepAliveSeconds(60);
        executor.setAwaitTerminationMillis(1000L * 60);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("long-task-executor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

}
