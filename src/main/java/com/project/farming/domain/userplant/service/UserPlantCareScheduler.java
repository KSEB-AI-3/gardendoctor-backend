package com.project.farming.domain.userplant.service;

import com.project.farming.domain.notification.service.NotificationService;
import com.project.farming.domain.userplant.entity.UserPlant;
import com.project.farming.domain.userplant.repository.UserPlantRepository;
import com.project.farming.global.exception.UserPlantNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserPlantCareScheduler {

    private final UserPlantRepository userPlantRepository;
    private final NotificationService notificationService;

    /**
     * 매일 오전 12시에 모든 사용자의 식물 상태 초기화
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void initUserPlantStatus() {
        List<UserPlant> userPlantList = userPlantRepository.findAll();
        if (userPlantList.isEmpty()) {
            throw new UserPlantNotFoundException("등록된 사용자 식물이 없습니다.");
        }
        userPlantList.forEach(userPlant ->
                userPlant.updateUserPlantStatus(false, false, false));
        userPlantRepository.saveAll(userPlantList);
        log.info("userPlant 상태 초기화 완료");
    }

    /**
     * 매일 오후 6시에 미완료 작업 알림 발송(각 userPlant)
     */
    @Transactional
    @Scheduled(cron = "0 0 18 * * *")
    public void notifyIncompleteTasks() {
        List<UserPlant> userPlantList = userPlantRepository.findAll();
        if (userPlantList.isEmpty()) {
            throw new UserPlantNotFoundException("등록된 사용자 식물이 없습니다.");
        }
        userPlantList.forEach(userPlant -> {
            List<String> incompleteTasks = new ArrayList<>();
            if (!userPlant.isWatered()) incompleteTasks.add("물 주기");
            if (!userPlant.isPruned()) incompleteTasks.add("가지치기");
            if (!userPlant.isFertilized()) incompleteTasks.add("영양제 주기");
            if (!incompleteTasks.isEmpty()) {
                String tasks = String.join(", ", incompleteTasks);
                log.info("Incomplete tasks for ID({}): {}", userPlant.getUserPlantId(), tasks);

                String content = String.format("%s(%s)의 미완료 작업: %s. 잊지 말고 해주세요!",
                        userPlant.getPlantNickname(), userPlant.getPlantName(), tasks);
                notificationService.createAndSendNotification(
                        userPlant.getUser(), "오늘의 미완료 작업 알림", content);
            }
        });
    }
}
