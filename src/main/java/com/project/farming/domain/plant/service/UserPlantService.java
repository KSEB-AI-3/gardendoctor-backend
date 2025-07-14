package com.project.farming.domain.plant.service;

import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.farm.repository.FarmRepository;
import com.project.farming.domain.plant.dto.UserPlantRequest;
import com.project.farming.domain.plant.dto.UserPlantResponse;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.entity.UserPlant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.domain.plant.repository.UserPlantRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.FarmNotFoundException;
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
    private final FarmRepository farmRepository;

    @Transactional
    public UserPlantResponse saveUserPlant(UserPlantRequest request) {
        User user = findUserById(request.getUserId());
        if (userPlantRepository.existsByUserAndNickname(user, request.getNickname())) {
            throw new IllegalArgumentException("이미 등록된 사용자 식물입니다: " + request.getNickname());
        }
        Plant plant = plantRepository.findByName(request.getPlantName())
                .orElseGet(() -> plantRepository.getDummyPlant("기타")
                        .orElseThrow(() -> new PlantNotFoundException("DB에 '기타' 항목이 존재하지 않습니다.")));
        String plantName = plant.getName();
        if (Objects.equals(plantName, "기타")) {
            plantName = request.getPlantName();
        }
        Farm farm = findFarmByGardenUniqueId(request.getGardenUniqueId());
        String plantingPlace = farm.getName();
        if (Objects.equals(plantingPlace, "기타(Other)")) {
            plantingPlace = request.getPlantingPlace();
        }
        else if (Objects.equals(plantingPlace, "N/A")) {
            plantingPlace = farm.getLotNumberAddress();
        }

        UserPlant newUserPlant = UserPlant.builder()
                .user(user)
                .plant(plant)
                .plantName(plantName)
                .nickname(request.getNickname())
                .farm(farm)
                .plantingPlace(plantingPlace)
                .plantedDate(request.getPlantedDate())
                .notes(request.getNotes())
                .imageUrl(request.getImageUrl())
                .build();
        UserPlant savedUserPlant = userPlantRepository.save(newUserPlant);
        return UserPlantResponse.builder()
                .message("해당 식물이 성공적으로 등록되었습니다.")
                .userPlantId(savedUserPlant.getUserPlantId())
                .plantName(savedUserPlant.getPlantName())
                .nickname(savedUserPlant.getNickname())
                .build();
    }

    public List<UserPlantResponse> findAllUserPlants(Long userId) {
        User user = findUserById(userId);
        List<UserPlant> foundUserPlants = userPlantRepository.findByUserOrderByNicknameAsc(user);
        if (foundUserPlants.isEmpty()) {
            throw new UserPlantNotFoundException("등록된 사용자 식물이 없습니다.");
        }
        return foundUserPlants.stream()
                .map(userPlant -> UserPlantResponse.builder()
                        .userId(userId)
                        .plantName(userPlant.getPlantName())
                        .nickname(userPlant.getNickname())
                        .plantingPlace(userPlant.getPlantingPlace())
                        .userPlantImageUrl(userPlant.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserPlantResponse findUserPlant(Long userId, Long userPlantId) {
        User user = findUserById(userId);
        UserPlant foundUserPlant = findUserPlantByUserAndUserPlantId(user, userPlantId);
        Plant plant = foundUserPlant.getPlant();

        UserPlantResponse.UserPlantResponseBuilder builder = UserPlantResponse.builder()
                .userId(userId)
                .plantName(foundUserPlant.getPlantName())
                .nickname(foundUserPlant.getNickname())
                .plantingPlace(foundUserPlant.getPlantingPlace())
                .plantedDate(foundUserPlant.getPlantedDate())
                .notes(foundUserPlant.getNotes())
                .userPlantImageUrl(foundUserPlant.getImageUrl());

        if (!Objects.equals(plant.getName(), "기타")) {
            builder.plantEnglishName(plant.getEnglishName())
                    .species(plant.getSpecies())
                    .season(plant.getSeason())
                    .plantImageUrl(plant.getImageUrl());
        }
        return builder.build();
    }

    @Transactional
    public UserPlantResponse updateUserPlant(
            Long userId, Long userPlantId, UserPlantRequest request) {

        User user = findUserById(userId);
        UserPlant userPlant = userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 사용자 식물입니다: " + userPlantId));
        Farm farm = findFarmByGardenUniqueId(request.getGardenUniqueId());
        String plantingPlace = farm.getName();
        if (Objects.equals(plantingPlace, "기타(Other)")) {
            plantingPlace = request.getPlantingPlace();
        }
        else if (Objects.equals(plantingPlace, "N/A")) {
            plantingPlace = farm.getLotNumberAddress();
        }
        userPlant.updateUserPlant(request.getNickname(), farm, plantingPlace,
                request.getNotes(), request.getImageUrl());
        UserPlant updatedUserPlant = userPlantRepository.save(userPlant);
        return UserPlantResponse.builder()
                .message("해당 식물 정보가 성공적으로 수정되었습니다.")
                .plantName(updatedUserPlant.getPlantName())
                .nickname(updatedUserPlant.getNickname())
                .build();
    }

    @Transactional
    public void deleteUserPlant(Long userId, Long userPlantId) {
        User user = findUserById(userId);
        UserPlant userPlant = userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 사용자 식물입니다: " + userPlantId));
        userPlantRepository.delete(userPlant);
    }

    private UserPlantResponse.UserPlantResponseBuilder toUserPlantResponseBuilder(UserPlant userPlant, User user) {
        return UserPlantResponse.builder()
                .userId(user.getUserId())
                .plantName(userPlant.getPlantName())
                .nickname(userPlant.getNickname())
                .plantingPlace(userPlant.getPlantingPlace())
                .plantedDate(userPlant.getPlantedDate())
                .notes(userPlant.getNotes())
                .userPlantImageUrl(userPlant.getImageUrl());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    private UserPlant findUserPlantByUserAndUserPlantId(User user, Long userPlantId) {
        return userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 사용자 식물입니다: " + userPlantId));
    }

    private Farm findFarmByGardenUniqueId(Integer gardenUniqueId) {
        return farmRepository.findByGardenUniqueId(gardenUniqueId)
                .orElseGet(() -> farmRepository.getDummyFarm("기타(Other)")
                        .orElseThrow(() -> new FarmNotFoundException("DB에 '기타(Other)' 항목이 존재하지 않습니다.")));

    }
}
