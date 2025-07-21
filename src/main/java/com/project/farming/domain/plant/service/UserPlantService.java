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
        Plant plant = findPlantByName(request.getPlantName());
        String plantName = getPlantName(plant, request);
        Farm farm = findFarmByGardenUniqueId(request.getGardenUniqueId());
        String plantingPlace = getPlantingPlace(farm, request);
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
        return toUserPlantResponseBuilder(newUserPlant, user, true, false)
                .userPlantId(savedUserPlant.getUserPlantId())
                .build();
    }

    public List<UserPlantResponse> findAllUserPlants(Long userId) {
        User user = findUserById(userId);
        List<UserPlant> foundUserPlants = userPlantRepository.findByUserOrderByNicknameAsc(user);
        if (foundUserPlants.isEmpty()) {
            throw new UserPlantNotFoundException("등록된 사용자 식물이 없습니다.");
        }
        return foundUserPlants.stream()
                .map(userPlant -> toUserPlantResponseBuilder(userPlant, user, false, false).build())
                .collect(Collectors.toList());
    }

    public List<UserPlantResponse> findUserPlantsByKeyword(Long userId, String keyword) {
        User user = findUserById(userId);
        List<UserPlant> foundUserPlants = userPlantRepository.findByUserAndNicknameContainingOrderByNicknameAsc(user, keyword);
        return foundUserPlants.stream()
                .map(userPlant -> toUserPlantResponseBuilder(userPlant, user, false, false).build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserPlantResponse findUserPlant(Long userId, Long userPlantId) {
        User user = findUserById(userId);
        UserPlant foundUserPlant = findUserPlantByUserAndUserPlantId(user, userPlantId);
        return toUserPlantResponseBuilder(foundUserPlant, user, true, true).build();
    }

    @Transactional
    public UserPlantResponse updateUserPlant(
            Long userId, Long userPlantId, UserPlantRequest request) {

        User user = findUserById(userId);
        UserPlant userPlant = findUserPlantByUserAndUserPlantId(user, userPlantId);
        Farm farm = findFarmByGardenUniqueId(request.getGardenUniqueId());
        String plantingPlace = getPlantingPlace(farm, request);
        userPlant.updateUserPlant(request.getNickname(), farm, plantingPlace,
                request.getNotes(), request.getImageUrl());
        UserPlant updatedUserPlant = userPlantRepository.save(userPlant);
        return toUserPlantResponseBuilder(updatedUserPlant, user, true, false).build();
    }

    @Transactional
    public void deleteUserPlant(Long userId, Long userPlantId) {
        User user = findUserById(userId);
        UserPlant userPlant = findUserPlantByUserAndUserPlantId(user, userPlantId);
        userPlantRepository.delete(userPlant);
    }

    private UserPlantResponse.UserPlantResponseBuilder toUserPlantResponseBuilder(
            UserPlant userPlant, User user, boolean includeDetails, boolean includePlantDetails) {

        UserPlantResponse.UserPlantResponseBuilder builder = UserPlantResponse.builder()
                .userId(user.getUserId())
                .plantName(userPlant.getPlantName())
                .nickname(userPlant.getNickname())
                .plantingPlace(userPlant.getPlantingPlace())
                .userPlantImageUrl(userPlant.getImageUrl());
        if (includeDetails) {
            builder.plantedDate(userPlant.getPlantedDate())
                    .notes(userPlant.getNotes());
        }
        if (includePlantDetails) {
            Plant plant = userPlant.getPlant();
            if (!Objects.equals(plant.getName(), "기타")) {
                builder.plantEnglishName(plant.getEnglishName())
                        .species(plant.getSpecies())
                        .season(plant.getSeason())
                        .plantImageUrl(plant.getImageUrl());
            }
        }
        return builder;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    private UserPlant findUserPlantByUserAndUserPlantId(User user, Long userPlantId) {
        return userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new UserPlantNotFoundException("사용자(" + user.getUserId() + ")가 등록하지 않은 식물입니다: " + userPlantId));
    }

    private Plant findPlantByName(String plantName) {
        return plantRepository.findByName(plantName)
                .orElseGet(() -> plantRepository.getDummyPlant("기타")
                        .orElseThrow(() -> new PlantNotFoundException("DB에 '기타' 항목이 존재하지 않습니다.")));
    }

    private String getPlantName(Plant plant, UserPlantRequest request) {
        String plantName = plant.getName();
        if (Objects.equals(plantName, "기타")) {
            plantName = request.getPlantName();
        }
        return plantName;
    }

    private Farm findFarmByGardenUniqueId(Integer gardenUniqueId) {
        return farmRepository.findByGardenUniqueId(gardenUniqueId)
                .orElseGet(() -> farmRepository.getDummyFarm("기타(Other)")
                        .orElseThrow(() -> new FarmNotFoundException("DB에 '기타(Other)' 항목이 존재하지 않습니다.")));

    }

    private String getPlantingPlace(Farm farm, UserPlantRequest request) {
        String plantingPlace = farm.getName();
        if (Objects.equals(plantingPlace, "기타(Other)")) {
            plantingPlace = request.getPlantingPlace();
        }
        if (Objects.equals(plantingPlace, "N/A")) {
            plantingPlace = farm.getLotNumberAddress();
        }
        return plantingPlace;
    }
}
