package com.swyp.picke.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 스케줄링 활성화.
 * 크레딧 주간 배치({@link com.swyp.picke.domain.user.service.batch.CreditWeeklyBatchScheduler})의 {@code @Scheduled} 어노테이션이 동작하려면 필요.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("credit-scheduler-");
        return scheduler;
    }
}
