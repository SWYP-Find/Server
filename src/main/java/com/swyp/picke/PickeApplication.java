package com.swyp.picke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableJpaAuditing
@SpringBootApplication
public class PickeApplication {
    public static void main(String[] args) {
        SpringApplication.run(PickeApplication.class, args); // 실행 클래스 수정완료
    }
}