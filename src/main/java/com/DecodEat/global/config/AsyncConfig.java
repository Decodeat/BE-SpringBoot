package com.DecodEat.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {
    @Bean(name = "ocrTaskExecutor") // 이 Bean의 이름을 지정한다.
    public Executor ocrTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수: 풀에 항상 살아있는 스레드 수
        taskExecutor.setCorePoolSize(5);

        // 최대 스레드 수: CorePoolSize를 넘어 큐가 가득 찼을 때 확장될 수 있는 최대 스레드 수
        taskExecutor.setMaxPoolSize(10);

        // 큐 용량: CorePoolSize의 스레드가 모두 일하고 있을 때, 요청을 대기시킬 큐의 크기
        taskExecutor.setQueueCapacity(25);

        // 스레드 이름 접두사: 로그를 볼 때 어떤 스레드 풀에서 실행됐는지 쉽게 파악 가능
        taskExecutor.setThreadNamePrefix("OcrExecutor-");

        taskExecutor.initialize();
        return taskExecutor;
    }
}
