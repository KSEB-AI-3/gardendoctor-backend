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
import jakarta.transaction.Transactional;
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
        User user = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + request.getUserEmail()));

        if (userPlantRepository.existsByUserAndNickname(user, request.getNickname())) {
            throw new IllegalArgumentException("이미 등록된 식물입니다.");
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
                .nickname(savedUserPlant.getNickname())
                .build();
    }

    public List<UserPlantResponseDto> findAllUserPlants(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));

        List<UserPlant> foundUserPlants = userPlantRepository.findByUserOrderByNicknameAsc(user);
        if (foundUserPlants.isEmpty()) {
            throw new UserPlantNotFoundException("등록된 식물이 없습니다.");
        }
        return foundUserPlants.stream()
                .map(userPlant -> UserPlantResponseDto.builder()
                        .userEmail(userPlant.getUser().getEmail())
                        .PlantName(userPlant.getPlantName())
                        .nickname(userPlant.getNickname())
                        .plantingPlace(userPlant.getPlantingPlace())
                        .plantedDate(userPlant.getPlantedDate())
                        .notes(userPlant.getNotes())
                        .imageUrl(userPlant.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    public UserPlantResponseDto findUserPlantByName(String email, String nickname) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));

        UserPlant foundUserPlant = userPlantRepository.findByUserAndNickname(user, nickname)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 식물입니다."));
        return UserPlantResponseDto.builder()
                .userEmail(foundUserPlant.getUser().getEmail())
                .PlantName(foundUserPlant.getPlantName())
                .nickname(foundUserPlant.getNickname())
                .plantingPlace(foundUserPlant.getPlantingPlace())
                .plantedDate(foundUserPlant.getPlantedDate())
                .notes(foundUserPlant.getNotes())
                .imageUrl(foundUserPlant.getImageUrl())
                .build();
    }

    @Transactional
    public UserPlantResponseDto updateUserPlant(
            String email, String nickname, UserPlantRequestDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));

        UserPlant userPlant = userPlantRepository.findByUserAndNickname(user, nickname)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 식물입니다."));
        userPlant.updateUserPlant(request.getNickname(), request.getPlantingPlace(),
                request.getNotes(), request.getImageUrl());
        UserPlant updatedUserPlant = userPlantRepository.save(userPlant);
        return UserPlantResponseDto.builder()
                .message("해당 식물 정보가 성공적으로 수정되었습니다.")
                .nickname(updatedUserPlant.getNickname())
                .build();
    }

    @Transactional
    public void deleteUserPlant(String email, String nickname) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));

        UserPlant userPlant = userPlantRepository.findByUserAndNickname(user, nickname)
                .orElseThrow(() -> new UserPlantNotFoundException("등록되지 않은 식물입니다."));
        userPlantRepository.delete(userPlant);
    }
    
    // 식물별 일지 작성 수 반환하는 기능 추가
    // 등록된 식물의 정보를 가져오는 기능 추가
}
