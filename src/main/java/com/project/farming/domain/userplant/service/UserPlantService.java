package com.project.farming.domain.userplant.service;

import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.farm.repository.FarmRepository;
import com.project.farming.domain.userplant.dto.UserPlantRequest;
import com.project.farming.domain.userplant.dto.UserPlantResponse;
import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.userplant.entity.UserPlant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.domain.userplant.repository.UserPlantRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.*;
import com.project.farming.global.image.entity.DefaultImages;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.repository.ImageFileRepository;
import com.project.farming.global.image.service.ImageFileService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private final ImageFileService imageFileService;
    private final ImageFileRepository imageFileRepository;

    /**
     * 사용자 식물 정보 등록
     *
     * @param userId 사용자 ID
     * @param request 등록할 사용자 식물 정보
     * @param file 업로드할 사용자 식물 이미지 파일 (선택적)
     * @return 저장된 사용자 식물 정보의 Response DTO
     */
    @Transactional
    public UserPlantResponse saveUserPlant(Long userId, UserPlantRequest request, MultipartFile file) {
        User user = findUserById(userId);
        if (userPlantRepository.existsByUserAndPlantNickname(user, request.getPlantNickname())) {
            throw new IllegalArgumentException("이미 등록된 사용자 식물입니다: " + request.getPlantNickname());
        }
        Plant plant = findPlantByPlantName(request.getPlantName());
        String plantName = getPlantName(plant.getPlantName(), request.getPlantName());
        Farm farm = findFarmByGardenUniqueId(request.getGardenUniqueId());
        String plantingPlace = getPlantingPlace(farm.getFarmName(), farm.getLotNumberAddress(), request.getPlantingPlace());
        ImageFile defaultImageFile = imageFileRepository.findByS3Key(DefaultImages.DEFAULT_PLANT_IMAGE)
                .orElseThrow(() -> new ImageFileNotFoundException("기본 식물 이미지가 존재하지 않습니다."));
        UserPlant newUserPlant = UserPlant.builder()
                .user(user)
                .plant(plant)
                .plantName(plantName)
                .plantNickname(request.getPlantNickname())
                .farm(farm)
                .plantingPlace(plantingPlace)
                .plantedDate(request.getPlantedDate())
                .notes(request.getNotes())
                .isNotificationEnabled(request.getIsNotificationEnabled())
                .waterIntervalDays(request.getWaterIntervalDays())
                .watered(request.getWatered())
                .pruneIntervalDays(request.getPruneIntervalDays())
                .pruned(request.getPruned())
                .fertilizeIntervalDays(request.getFertilizeIntervalDays())
                .fertilized(request.getFertilized())
                .userPlantImageFile(defaultImageFile)
                .build();
        newUserPlant.updateUserPlantStatus(request.getWatered(), request.getPruned(), request.getFertilized());
        UserPlant savedUserPlant = userPlantRepository.save(newUserPlant);
        Long userPlantId = savedUserPlant.getUserPlantId();

        if (file != null && !file.isEmpty()) {
            // 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.uploadImage(file, ImageDomainType.USERPLANT, userPlantId);
            savedUserPlant.updateUserPlantImage(imageFile);
        }
        return toUserPlantResponseBuilder(newUserPlant, true, false).build();
    }

    /**
     * 사용자 식물 목록 조회(별명순)
     * - 일부 정보만 반환
     *
     * @param userId 사용자 ID
     * @return 각 사용자 식물 정보의 Response DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<UserPlantResponse> findAllUserPlants(Long userId) {
        User user = findUserById(userId);
        List<UserPlant> foundUserPlants = userPlantRepository.findByUserOrderByPlantNicknameAsc(user);
        if (foundUserPlants.isEmpty()) {
            throw new UserPlantNotFoundException("등록된 사용자 식물이 없습니다.");
        }
        return foundUserPlants.stream()
                .map(userPlant -> toUserPlantResponseBuilder(userPlant, false, false).build())
                .collect(Collectors.toList());
    }

    /**
     * 사용자 식물 목록 검색(별명순)
     * - 사용자 식물의 종류(Plant) 또는 별명으로 검색(통합)
     * - 일부 정보만 반환
     *
     * @param userId 사용자 ID
     * @param keyword 검색어(사용자 식물 별명)
     * @return 검색된 사용자 식물 정보의 Response DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<UserPlantResponse> findUserPlantsByKeyword(Long userId, String keyword) {
        List<UserPlant> foundUserPlants = userPlantRepository.findByUserAndPlantContainingOrderByPlantNicknameAsc(userId, "%"+keyword+"%");
        return foundUserPlants.stream()
                .map(userPlant -> toUserPlantResponseBuilder(userPlant, false, false).build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자 식물 정보 조회
     * - 식물(Plant) 정보도 반환
     *
     * @param userId 사용자 ID
     * @param userPlantId 조회할 사용자 식물 정보의 ID
     * @return 해당 사용자 식물 정보의 Response DTO
     */
    @Transactional(readOnly = true)
    public UserPlantResponse findUserPlant(Long userId, Long userPlantId) {
        User user = findUserById(userId);
        UserPlant foundUserPlant = findUserPlantByUserAndUserPlantId(user, userPlantId);
        return toUserPlantResponseBuilder(foundUserPlant, true, true).build();
    }

    /**
     * 특정 사용자 식물 정보 수정
     *
     * @param userId 사용자 ID
     * @param userPlantId 수정할 사용자 식물 정보의 ID
     * @param request 새로 저장할 사용자 식물 정보
     * @param newFile 새로 업로드할 사용자 식물 이미지 파일 (선택적)
     * @return 수정된 사용자 식물 정보의 Response DTO
     */
    @Transactional
    public UserPlantResponse updateUserPlant(
            Long userId, Long userPlantId, UserPlantRequest request, MultipartFile newFile) {

        User user = findUserById(userId);
        UserPlant userPlant = findUserPlantByUserAndUserPlantId(user, userPlantId);
        if (newFile != null && !newFile.isEmpty()) {
            // 새로운 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.updateImage(
                    userPlant.getUserPlantImageFile().getImageFileId(), // 기존 이미지 파일
                    newFile, ImageDomainType.USERPLANT, userPlantId);
            userPlant.updateUserPlantImage(imageFile);
        }
        if (isOtherPlant(userPlant.getPlant().getPlantName(), request.getPlantName())) {
            // 사용자 입력 식물인 경우 수정
            if (!Objects.equals(request.getPlantName(), userPlant.getPlantName())) {
                userPlant.updatePlantName(request.getPlantName());
            }
        }
        Farm farm = findFarmByGardenUniqueId(request.getGardenUniqueId());
        String plantingPlace = getPlantingPlace(farm.getFarmName(), farm.getLotNumberAddress(), request.getPlantingPlace());
        userPlant.updatePlantingPlace(farm, plantingPlace);
        userPlant.updateUserPlantInfo(request.getPlantNickname(), request.getNotes());
        userPlant.updateIsNotificationEnabled(request.getIsNotificationEnabled());
        userPlant.updateUserPlantIntervalDays(
                request.getWaterIntervalDays(), request.getPruneIntervalDays(), request.getFertilizeIntervalDays());
        userPlant.updateUserPlantStatus(request.getWatered(), request.getPruned(), request.getFertilized());
        UserPlant updatedUserPlant = userPlantRepository.save(userPlant);
        return toUserPlantResponseBuilder(updatedUserPlant, true, false).build();
    }

    /**
     * 특정 사용자 식물 정보 삭제
     *
     * @param userId 사용자 ID
     * @param userPlantId 삭제할 사용자 식물 정보의 ID
     */
    @Transactional
    public void deleteUserPlant(Long userId, Long userPlantId) {
        User user = findUserById(userId);
        UserPlant userPlant = findUserPlantByUserAndUserPlantId(user, userPlantId);
        userPlantRepository.delete(userPlant);
        imageFileService.deleteImage(userPlant.getUserPlantImageFile().getImageFileId()); // 기존 이미지 파일
    }

    /**
     * Response DTO로 변환
     * - 사용자 식물 리스트를 반환하는 경우에는 일부 정보만 반환
     * - 특정 사용자 식물 정보를 반환하는 경우에는 식물(Plant) 상세 정보도 반환
     *
     * @param userPlant Response DTO로 변환할 사용자 식물 정보 엔티티
     * @param includeDetails 전체 정보 반환 여부
     * @param includePlantDetails 식물(Plant) 상세 정보 반환 여부
     * @return 사용자 식물 정보 Response DTO
     */
    private UserPlantResponse.UserPlantResponseBuilder toUserPlantResponseBuilder(
            UserPlant userPlant, boolean includeDetails, boolean includePlantDetails) {

        UserPlantResponse.UserPlantResponseBuilder builder = UserPlantResponse.builder()
                .userPlantId(userPlant.getUserPlantId())
                .plantName(userPlant.getPlantName())
                .plantNickname(userPlant.getPlantNickname())
                .plantingPlace(userPlant.getPlantingPlace())
                .userPlantImageUrl(userPlant.getUserPlantImageFile().getImageUrl());
        if (includeDetails) {
            builder.plantedDate(userPlant.getPlantedDate())
                    .notes(userPlant.getNotes())
                    .isNotificationEnabled(userPlant.isNotificationEnabled())
                    .waterIntervalDays(userPlant.getWaterIntervalDays())
                    .watered(userPlant.isWatered())
                    .pruneIntervalDays(userPlant.getPruneIntervalDays())
                    .pruned(userPlant.isPruned())
                    .fertilizeIntervalDays(userPlant.getFertilizeIntervalDays())
                    .fertilized(userPlant.isFertilized());
        }
        if (includePlantDetails) {
            Plant plant = userPlant.getPlant();
            if (!Objects.equals(plant.getPlantName(), "기타")) {
                builder.plantEnglishName(plant.getPlantEnglishName())
                        .species(plant.getSpecies())
                        .season(plant.getSeason())
                        .plantImageUrl(plant.getPlantImageFile().getImageUrl());
            }
        }
        return builder;
    }

    /**
     * ID로 사용자 조회
     *
     * @param userId 조회할 사용자의 ID
     * @return 조회한 사용자
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 사용자와 ID로 사용자 식물 정보 조회
     *
     * @param user 사용자
     * @param userPlantId 조회할 사용자 식물 정보의 ID
     * @return 조회한 사용자 식물 정보
     */
    private UserPlant findUserPlantByUserAndUserPlantId(User user, Long userPlantId) {
        return userPlantRepository.findByUserAndUserPlantId(user, userPlantId)
                .orElseThrow(() -> new UserPlantNotFoundException("사용자(" + user.getUserId() + ")가 등록하지 않은 식물입니다: " + userPlantId));
    }

    /**
     * 식물 이름으로 식물(Plant) 정보 조회
     *
     * @param plantName 조회할 식물의 이름(한글, 영어)
     * @return 조회한 식물(Plant) 정보
     */
    private Plant findPlantByPlantName(String plantName) {
        return plantRepository.findByPlantName(plantName)
                .orElseGet(() -> plantRepository.getOtherPlant("기타")
                        .orElseThrow(() -> new PlantNotFoundException("DB에 '기타' 항목이 존재하지 않습니다.")));
    }

    /**
     * 식물 종류 설정
     *
     * @param oldPlantName 원래 식물 이름(Plant)
     * @param requestPlantName 사용자가 작성한 식물 이름(other)
     * @return 설정된 식물 이름
     */
    private String getPlantName(String oldPlantName, String requestPlantName) {
        String newPlantName = oldPlantName;
        if (Objects.equals(oldPlantName, "기타")) {
            newPlantName = requestPlantName;
        }
        return newPlantName;
    }

    /**
     * 텃밭 고유번호로 텃밭 정보 조회
     *
     * @param gardenUniqueId 조회할 텃밭의 고유번호
     * @return 조회한 텃밭 정보
     */
    private Farm findFarmByGardenUniqueId(int gardenUniqueId) {
        return farmRepository.findByGardenUniqueId(gardenUniqueId)
                .orElseGet(() -> farmRepository.getOtherFarm("기타(Other)")
                        .orElseThrow(() -> new FarmNotFoundException("DB에 '기타(Other)' 항목이 존재하지 않습니다.")));
    }

    /**
     * 심은 장소 설정
     *
     * @param oldFarmName 원래 텃밭 이름(Farm)
     * @param lotNumberAddress 텃밭 이름이 없는 경우에 사용할 이름(주소)
     * @param requestFarmName 사용자가 작성한 장소 이름(Other)
     * @return 설정된 심은 장소 이름
     */
    private String getPlantingPlace(
            String oldFarmName, String lotNumberAddress, String requestFarmName) {
        String plantingPlace = oldFarmName;
        if (Objects.equals(oldFarmName, "기타(Other)")) {
            plantingPlace = requestFarmName;
        }
        if (Objects.equals(plantingPlace, "N/A")) {
            plantingPlace = lotNumberAddress;
        }
        return plantingPlace;
    }

    /**
     * 사용자 입력 식물인지 아닌지 확인
     *
     * @param oldPlantName 기존 사용자 식물 종류 이름(Plant의 PlantName)
     * @param requestPlantName 사용자가 작성한 식물 이름(other)
     * @return 결과값(TF)
     */
    private boolean isOtherPlant(String oldPlantName, String requestPlantName) {
        return Objects.equals(oldPlantName, "기타")
                && !plantRepository.existsByPlantName(requestPlantName)
                && !plantRepository.existsByPlantEnglishName(requestPlantName);
    }
}
