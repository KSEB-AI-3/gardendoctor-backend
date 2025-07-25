// src/main/java/com/project/farming/domain/notification/scheduler/DailyNotificationScheduler.java
package com.project.farming.domain.notification.scheduler; // 패키지 변경 권장

import com.project.farming.domain.notification.service.NotificationService;
import com.project.farming.domain.plant.entity.UserPlant;
import com.project.farming.domain.plant.repository.UserPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component; // @Component로 변경
import org.springframework.transaction.annotation.Transactional; // @Transactional 임포트 추가

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component // @Service 대신 @Component 사용
@RequiredArgsConstructor
@Slf4j
public class DailyNotificationScheduler {

    private final UserPlantRepository userPlantRepository;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 18 * * *") // 오후 6시
    @Transactional // 트랜잭션 추가
    public void sendEveningIncompleteNotifications() {
        log.info("Executing sendEveningIncompleteNotifications at {}", LocalDateTime.now());
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        List<UserPlant> userPlants = userPlantRepository.findAllWithUserAndPlant();

        for (UserPlant userPlant : userPlants) {
            String key = "userplant:" + userPlant.getUserPlantId() + ":" + today;
            BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(key);

            boolean watered = "true".equals(hashOps.get("watered"));
            boolean pruned = "true".equals(hashOps.get("pruned"));
            boolean fertilized = "true".equals(hashOps.get("fertilized"));

            StringBuilder sb = new StringBuilder();
            if (!watered) sb.append("물주기, ");
            if (!pruned) sb.append("가지치기, ");
            if (!fertilized) sb.append("영양제 주기, ");

            if (!sb.isEmpty()) {
                sb.setLength(sb.length() - 2);
                String message = String.format("오늘 %s의 미완료 작업: %s", userPlant.getPlantName(), sb);
                notificationService.createAndSendNotification(userPlant.getUser(), "[오늘 미완료 알림]", message);
            }
        }
        log.info("Finished sendEveningIncompleteNotifications.");
    }

    @Scheduled(cron = "0 0 10 * * *") // 오전 10시
    @Transactional // 트랜잭션 추가
    public void sendMorningTasks() {
        log.info("Executing sendMorningTasks at {}", LocalDateTime.now());
        List<UserPlant> userPlants = userPlantRepository.findAllWithUserAndPlant();

        for (UserPlant userPlant : userPlants) {
            String message = String.format("오늘 %s의 할 일: 물주기, 가지치기, 영양제 주기 체크를 잊지 마세요!", userPlant.getPlantName());
            notificationService.createAndSendNotification(userPlant.getUser(), "[오늘의 할 일]", message);
        }
        log.info("Finished sendMorningTasks.");
    }
}