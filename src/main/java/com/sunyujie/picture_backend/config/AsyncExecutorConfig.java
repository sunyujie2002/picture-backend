package com.sunyujie.picture_backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务线程池配置
 */
@Configuration
@EnableAsync
public class AsyncExecutorConfig {

    /**
     * COS 文件操作专用线程池（删除等 IO 密集型任务）
     */
    @Bean("cosFileExecutor")
    public Executor cosFileExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：平时常驻的线程，IO 密集型不需要太多
        executor.setCorePoolSize(2);
        // 最大线程数：队列堆满后才会扩容到这个数
        executor.setMaxPoolSize(8);
        // 等待队列：任务先排队，排满才开新线程
        executor.setQueueCapacity(100);
        // 空闲线程存活时间：超出核心数的线程空闲 60 秒后回收
        executor.setKeepAliveSeconds(60);
        // 线程名前缀：日志里一眼能看出是哪个池的线程
        executor.setThreadNamePrefix("async-file-");
        // 拒绝策略：队列和线程都满时，由提交任务的线程（Tomcat 请求线程）自己执行，
        // 不丢任务，同时给上游天然限流
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅停机：应用关闭时等待已提交任务执行完，最多等 30 秒，避免文件删一半
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
