package com.project.farming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing // ⭐ Jpa Auditing 활성화
@EnableScheduling // ⭐ 스케줄러 활성화 (DailyNotificationScheduler 동작 위함)
public class FarmingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmingApplication.class, args);
    }

}
