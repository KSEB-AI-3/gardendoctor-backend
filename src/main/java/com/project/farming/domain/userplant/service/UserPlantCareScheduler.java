package com.project.farming.domain.userplant.service;

import com.project.farming.domain.notification.service.NotificationService;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.userplant.entity.UserPlant;
import com.project.farming.domain.userplant.repository.UserPlantRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            log.info("등록된 사용자 식물이 없습니다.");
            return;
        }
        userPlantList.forEach(userPlant ->
                userPlant.updateUserPlantStatus(false, false, false));
        userPlantRepository.saveAll(userPlantList);
        log.info("userPlant 상태 초기화 완료");
    }

    /**
     * 매일 오전 9시에 오늘의 작업 알림 발송(각 userPlant)
     * - FCM 토큰이 유효하지 않는 사용자의 userPlant, 알림 수신 안 하는 userPlant 제외
     */
    @Transactional
    @Scheduled(cron = "0 0 9  * * *")
    public void sendDailyTasksNotification() {
        Map<UserPlant, List<String>> tasksByUserPlant = new HashMap<>();

        // computeIfAbsent는 키(userPlant)에 대한 값(작업 목록)이 존재하면 해당 값 반환
        // k는 map에 존재하지 않는 키: 이 경우 새 리스트 생성 후 저장
        // 즉, 이미 존재하는 리스트 또는 새로 생성된 리스트에 add 수행
        List<UserPlant> needWateringToday = userPlantRepository.findUserPlantsNeedWateringToday();
        needWateringToday.forEach(up ->
                tasksByUserPlant.computeIfAbsent(up, k -> new ArrayList<>()).add("\uD83D\uDCA7물 주기"));

        List<UserPlant> needPruningToday = userPlantRepository.findUserPlantsNeedPruningToday();
        needPruningToday.forEach(up ->
                tasksByUserPlant.computeIfAbsent(up, k -> new ArrayList<>()).add("✂️가지치기"));

        List<UserPlant> needFertilizingToday = userPlantRepository.findUserPlantsNeedFertilizingToday();
        needFertilizingToday.forEach(up ->
                tasksByUserPlant.computeIfAbsent(up, k -> new ArrayList<>()).add("\uD83D\uDC8A영양제 주기"));

        tasksByUserPlant.forEach((key, value) -> {
            User user = key.getUser();
            Long userPlantId = key.getUserPlantId();
            String plantName = key.getPlantName();
            String plantNickname = key.getPlantNickname();
            String tasks = String.join(", ", value);
            log.info("Daily tasks for UserPlant ID({}): {}", userPlantId, tasks);
            String message = String.format("%s(%s)의 오늘 해야 할 작업: %s. 잊지 말고 꼭 챙겨주세요!",
                    plantNickname, plantName, tasks);
            notificationService.createAndSendNotification(user, "\uD83E\uDEB4오늘의 작업 알림", message);
        });
    }

    /**
     * 매일 오후 5시에 미완료 작업 알림 발송(각 userPlant)
     * - FCM 토큰이 유효하지 않는 사용자의 userPlant, 알림 수신 안 하는 userPlant 제외
     */
    @Transactional
    @Scheduled(cron = "0 0 17 * * *")
    public void notifyIncompleteTasks() {
        Map<UserPlant, List<String>> incompleteTasksByUserPlant = new HashMap<>();

        List<UserPlant> incompleteWateringToday = userPlantRepository.findUserPlantsIncompleteWateringToday();
        incompleteWateringToday.forEach(up ->
                incompleteTasksByUserPlant.computeIfAbsent(up, k -> new ArrayList<>()).add("\uD83D\uDCA7물 주기"));

        List<UserPlant> incompletePruningToday = userPlantRepository.findUserPlantsIncompletePruningToday();
        incompletePruningToday.forEach(up ->
                incompleteTasksByUserPlant.computeIfAbsent(up, k -> new ArrayList<>()).add("✂️가지치기"));

        List<UserPlant> incompleteFertilizingToday = userPlantRepository.findUserPlantsIncompleteFertilizingToday();
        incompleteFertilizingToday.forEach(up ->
                incompleteTasksByUserPlant.computeIfAbsent(up, k -> new ArrayList<>()).add("\uD83D\uDC8A영양제 주기"));

        incompleteTasksByUserPlant.forEach((key, value) -> {
            User user = key.getUser();
            Long userPlantId = key.getUserPlantId();
            String plantName = key.getPlantName();
            String plantNickname = key.getPlantNickname();
            String tasks = String.join(", ", value);
            log.info("Incomplete tasks for UserPlant ID({}): {}", userPlantId, tasks);
            String message = String.format("%s(%s)의 아직 완료하지 않은 작업: %s. 잊지 말고 꼭 챙겨주세요!",
                    plantNickname, plantName, tasks);
            notificationService.createAndSendNotification(user, "⚠️오늘의 미완료 작업 알림", message);
        });
    }
}
