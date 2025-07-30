package com.project.farming.domain.userplant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class UserPlantDailyStatusRedisService {

    private final StringRedisTemplate redisTemplate;

    public void updateStatusOnDiaryWrite(Long userPlantId, boolean watered, boolean pruned, boolean fertilized) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // 20250714
        String key = "userplant:" + userPlantId + ":" + today;

        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(key);

        if (watered) {
            hashOps.put("watered", "true");
        }
        if (pruned) {
            hashOps.put("pruned", "true");
        }
        if (fertilized) {
            hashOps.put("fertilized", "true");
        }

        // TTL 설정 (오늘 자정까지 유지, 없으면 설정)
        Long ttl = redisTemplate.getExpire(key);
        if (ttl == null || ttl == -1) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime midnight = now.toLocalDate().atStartOfDay().plusDays(1);
            long secondsUntilMidnight = Duration.between(now, midnight).getSeconds();
            redisTemplate.expire(key, Duration.ofSeconds(secondsUntilMidnight));
        }
    }
}
