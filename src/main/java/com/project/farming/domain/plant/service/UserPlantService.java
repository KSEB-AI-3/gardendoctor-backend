package com.project.farming.domain.plant.service;

import com.project.farming.domain.plant.dto.UserPlantRequestDto;
import com.project.farming.domain.plant.dto.UserPlantResponseDto;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.entity.UserPlant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.domain.plant.repository.UserPlantRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.PlantNotFoundException;
import com.project.farming.global.exception.UserNotFoundException;
import com.project.farming.global.exception.UserPlantNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserPlantService {

    private final UserPlantRepository userPlantRepository;
    private final UserRepository userRepository;
    private final PlantRepository plantRepository;

    @Transactional
    public UserPlantResponseDto saveUserPlant(UserPlantRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + request.getUserId()));
        if (userPlantRepository.existsByUserAndNickname(user, request.getNickname())) {
            throw new IllegalArgumentException("이미 등록된 사용자 식물입니다: " + request.getNickname());
        }
        Plant plant = plantRepository.findByName(request.getPlantName())
                .orElseGet(() -> plantRepository.getDummyPlant("기타")
                        .orElseThrow(() -> new PlantNotFoundException("DB에 기타 항목이 존재하지 않습니다")));
        String plantName = plant.getName();
        if (Objects.equals(plantName, "기타")) {
            plantName = request.getPlantName();
        }
        UserPlant newUserPlant = UserPlant.builder()
                .user(user)
                .plant(plant)
                .plantName(plantName)
                .nickname(request.getNickname())
                .plantingPlace(request.getPlantingPlace())
                .plantedDate(request.getPlantedDate())
                .notes(request.getNotes())
                .imageUrl(request.getImageUrl())
                .build();
        UserPlant savedUserPlant = userPlantRepository.save(newUserPlant);
        return UserPlantResponseDto.builder()
                .message("해당 식물이 성공적으로 등록되었습니다.")
                .plantName(savedUserPlant.getPlantName())
                .nickname(savedUserPlant.getNickname())
                .build();
    }

    public List<UserPlantResponseDto> findAllUserPlants(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        List<UserPlant> foundUserPlants = userPlantRepository.findByUserOrderByNicknameAsc(user);
        if (foundUserPlants.isEmpty()) {
            throw new UserPlantNotFoundException("등록된 사용자 식물이 없습니다.");
        }
        return foundUserPlants.stream()
                .map(userPlant -> UserPlantResponseDto.builder()
                        .userId(userId)
                        .plantName(userPlant.getPlantName())
                        .nickname(userPlant.getNickname())
                        .plantingPlace(userPlant.getPlantingPlace())
                        .imageUrl(userPlant.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    public UserPlantResponseDto findUserPlant(Long userId, Long userPlantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        UserPlant foundUserPlant = userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 사용자 식물입니다: " + userPlantId));
        return UserPlantResponseDto.builder()
                .userId(userId)
                .plantName(foundUserPlant.getPlantName())
                .nickname(foundUserPlant.getNickname())
                .plantingPlace(foundUserPlant.getPlantingPlace())
                .plantedDate(foundUserPlant.getPlantedDate())
                .notes(foundUserPlant.getNotes())
                .imageUrl(foundUserPlant.getImageUrl())
                .build();
    }

    @Transactional
    public UserPlantResponseDto updateUserPlant(
            Long userId, Long userPlantId, UserPlantRequestDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        UserPlant userPlant = userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 사용자 식물입니다: " + userPlantId));
        userPlant.updateUserPlant(request.getNickname(), request.getPlantingPlace(),
                request.getNotes(), request.getImageUrl());
        UserPlant updatedUserPlant = userPlantRepository.save(userPlant);
        return UserPlantResponseDto.builder()
                .message("해당 식물 정보가 성공적으로 수정되었습니다.")
                .plantName(updatedUserPlant.getPlantName())
                .nickname(updatedUserPlant.getNickname())
                .build();
    }

    @Transactional
    public void deleteUserPlant(Long userId, Long userPlantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        UserPlant userPlant = userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 사용자 식물입니다: " + userPlantId));
        userPlantRepository.delete(userPlant);
    }
}
