package com.project.farming.domain.diary.service;

import com.project.farming.domain.notification.service.NotificationService;
import com.project.farming.domain.plant.entity.UserPlant;
import com.project.farming.domain.plant.repository.UserPlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyNotificationScheduler {

    private final UserPlantRepository userPlantRepository;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 18 * * *") // 오후 6시
    public void sendEveningIncompleteNotifications() {
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
                notificationService.sendNotification(userPlant.getUser(), "[오늘 미완료 알림]", message);
            }
        }
    }

    @Scheduled(cron = "0 0 10 * * *") // 오전 10시
    public void sendMorningTasks() {
        List<UserPlant> userPlants = userPlantRepository.findAllWithUserAndPlant();

        for (UserPlant userPlant : userPlants) {
            String message = String.format("오늘 %s의 할 일: 물주기, 가지치기, 영양제 주기 체크를 잊지 마세요!", userPlant.getPlantName());
            notificationService.sendNotification(userPlant.getUser(), "[오늘의 할 일]", message);
        }
    }
}
