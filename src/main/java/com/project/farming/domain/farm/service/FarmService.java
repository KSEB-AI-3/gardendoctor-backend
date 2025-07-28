package com.project.farming.domain.farm.service;

import com.project.farming.domain.farm.dto.FarmRequest;
import com.project.farming.domain.farm.dto.FarmResponse;
import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.farm.repository.FarmRepository;
import com.project.farming.global.exception.FarmNotFoundException;
import com.project.farming.global.exception.ImageFileNotFoundException;
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
import java.util.stream.Collectors;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class FarmService {

    private final FarmRepository farmRepository;
    private final ImageFileService imageFileService;
    private final ImageFileRepository imageFileRepository;

    /**
     * 새로운 텃밭 정보 등록
     *
     * @param request 등록할 텃밭 정보
     * @param file 업로드할 텃밭 이미지 파일 (선택적)
     * @return 저장된 텃밭 정보의 Response DTO
     */
    @Transactional
    public FarmResponse saveFarm(FarmRequest request, MultipartFile file) {
        if (farmRepository.existsByGardenUniqueId(request.getGardenUniqueId())) {
            throw new IllegalArgumentException("이미 존재하는 텃밭입니다: " + request.getGardenUniqueId());
        }
        ImageFile defaultImageFile = getDefaultImageFile();
        Farm newFarm = Farm.builder()
                .gardenUniqueId(request.getGardenUniqueId())
                .operator(request.getOperator())
                .farmName(request.getFarmName())
                .roadNameAddress(request.getRoadNameAddress())
                .lotNumberAddress(request.getLotNumberAddress())
                .facilities(request.getFacilities())
                .contact(request.getContact())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .available(request.getAvailable())
                .farmImageFile(defaultImageFile)
                .build();
        Farm savedFarm = farmRepository.save(newFarm);
        Long farmId = savedFarm.getFarmId();

        if (!file.isEmpty()) {
            // 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.uploadImage(file, ImageDomainType.FARM, farmId);
            savedFarm.updateFarmImage(imageFile);
        }
        return toFarmResponseBuilder(savedFarm, true)
                .farmId(farmId)
                .build();
    }

    /**
     * 전체 텃밭 목록 조회(고유번호순)
     * - 일부 정보만 반환
     *
     * @return 각 텃밭 정보의 Response DTO 리스트
     */
    public List<FarmResponse> findAllFarms() {
        List<Farm> foundFarms = farmRepository.findAllByOrderByGardenUniqueIdAsc();
        if (foundFarms.isEmpty()) {
            throw new FarmNotFoundException("등록된 텃밭이 없습니다.");
        }
        return foundFarms.stream()
                .map(farm -> toFarmResponseBuilder(farm, false).build())
                .collect(Collectors.toList());
    }

    /**
     * 텃밭 목록 검색(고유번호순)
     * - 텃밭의 이름 또는 주소(도로명주소, 지번주소)로 검색
     * - 일부 정보만 반환
     *
     * @param searchType 검색 조건(name 또는 address) - 기본값은 name
     * @param keyword 검색어(텃밭 이름 또는 주소)
     * @return 검색된 텃밭 정보의 Response DTO 리스트
     */
    public List<FarmResponse> findFarmsByKeyword(String searchType, String keyword) {
        List<Farm> foundFarms = switch (searchType) {
            case "name" -> farmRepository.findByFarmNameContainingOrderByGardenUniqueIdAsc(keyword);
            case "address" -> farmRepository.findByAddressContainingOrderByGardenUniqueIdAsc("%"+keyword+"%");
            default -> throw new IllegalArgumentException("지원하지 않는 검색 조건입니다: " + searchType);
        };
        return foundFarms.stream()
                .map(farm -> toFarmResponseBuilder(farm, false).build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 텃밭 정보 조회
     *
     * @param farmId 조회할 텃밭 정보의 ID
     * @return 해당 텃밭 정보의 Response DTO
     */
    public FarmResponse findFarm(Long farmId) {
        Farm foundFarm = findFarmById(farmId);
        return toFarmResponseBuilder(foundFarm, true).build();
    }

    /**
     * 특정 텃밭 정보 수정
     *
     * @param farmId 수정할 텃밭 정보의 ID
     * @param request 새로 저장할 텃밭 정보
     * @param newFile 새로 업로드할 텃밭 이미지 파일 (선택적)
     * @return 수정된 텃밭 정보의 Response DTO
     */
    @Transactional
    public FarmResponse updateFarm(Long farmId, FarmRequest request, MultipartFile newFile) {
        Farm farm = findFarmById(farmId);
        if (!newFile.isEmpty()) {
            // 새로운 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.updateImage(
                    farm.getFarmImageFile().getImageFileId(), // 기존 이미지 파일
                    newFile, ImageDomainType.FARM, farmId);
            farm.updateFarmImage(imageFile);
        }
        farm.updateFarmInfo(request.getGardenUniqueId(), request.getOperator(), request.getFarmName(),
                request.getRoadNameAddress(), request.getLotNumberAddress(),
                request.getFacilities(), request.getContact(),
                request.getLatitude(), request.getLongitude(), request.getAvailable());
        Farm updatedFarm = farmRepository.save(farm);
        return toFarmResponseBuilder(updatedFarm, true).build();
    }

    /**
     * 특정 텃밭 정보 삭제
     *
     * @param farmId 삭제할 텃밭 정보의 ID
     */
    @Transactional
    public void deleteFarm(Long farmId) {
        Farm farm = findFarmById(farmId);
        farmRepository.delete(farm);
        imageFileService.deleteImage(farm.getFarmImageFile().getImageFileId()); // 기존 이미지 파일
    }


    /**
     * 주변 텃밭 정보 조회
     *  - 현재 위치를 기준으로 지정된 반경 내에 위치한 텃밭들의 정보 조회
     *
     * @param latitude 현재 위치의 위도
     * @param longitude 현재 위치의 경도
     * @param radius 조회할 반경(단위: km) - 기본값은 20km
     * @return 지정된 반경 내에 위치한 텃밭의 정보 Response DTO 리스트
     */
    public List<FarmResponse> findFarmsByCurrentLocation (Double latitude, Double longitude, Double radius) {
        log.info("현재 위치: {}, {} / 반경: {}", latitude, longitude, radius);
        List<Farm> foundFarms = farmRepository.findFarmsWithinRadius(
                latitude, longitude, radius * 1000); // 미터 단위로 계산
        return foundFarms.stream()
                .map(farm -> toFarmResponseBuilder(farm, true).build())
                .collect(Collectors.toList());
    }

    /**
     * Response DTO로 변환
     * - 텃밭 리스트를 반환하는 경우에는 일부 정보만 반환
     *
     * @param farm Response DTO로 변환할 텃밭 정보 엔티티
     * @param includeDetails 전체 정보 반환 여부
     * @return 텃밭 정보 Response DTO
     */
    private FarmResponse.FarmResponseBuilder toFarmResponseBuilder(Farm farm, boolean includeDetails) {
        FarmResponse.FarmResponseBuilder builder = FarmResponse.builder()
                .gardenUniqueId(farm.getGardenUniqueId())
                .operator(farm.getOperator())
                .farmName(farm.getFarmName())
                .lotNumberAddress(farm.getLotNumberAddress())
                .updatedAt(farm.getUpdatedAt())
                .farmImageUrl(farm.getFarmImageFile().getImageUrl());
        if (includeDetails) {
            builder.roadNameAddress(farm.getRoadNameAddress())
                    .facilities(farm.getFacilities())
                    .contact(farm.getContact())
                    .latitude(farm.getLatitude())
                    .longitude(farm.getLongitude())
                    .available(farm.getAvailable())
                    .createdAt(farm.getCreatedAt());
        }
        return builder;
    }

    /**
     * ID로 텃밭 정보 조회
     *
     * @param farmId 조회할 텃밭 정보의 ID
     * @return 조회한 텃밭 정보
     */
    private Farm findFarmById(Long farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new FarmNotFoundException("해당 텃밭이 존재하지 않습니다: " + farmId));
    }

    /**
     * 텃밭 기본 이미지 반환
     *
     * @return 텃밭 기본 이미지
     */
    private ImageFile getDefaultImageFile() {
        return imageFileRepository.findByS3Key(DefaultImages.DEFAULT_FARM_IMAGE)
                .orElseThrow(() -> new ImageFileNotFoundException("기본 텃밭 이미지가 존재하지 않습니다."));
    }

    /**
     * FarmDataInitializer에서 사용
     *
     * @param farmList 저장할 초기 텃밭 정보 목록
     */
    @Transactional
    public void saveFarms(List<Farm> farmList) {
        farmRepository.saveAll(farmList);
    }

    /**
     * FarmDataInitializer에서 사용
     * - 사용자 입력 옵션 저장
     */
    @Transactional
    public void saveOtherFarmOption() {
        ImageFile defaultImageFile = getDefaultImageFile();
        Farm otherFarmOption = Farm.builder()
                .gardenUniqueId(1)
                .operator("N/A")
                .farmName("기타(Other)")
                .roadNameAddress("N/A")
                .lotNumberAddress("N/A")
                .facilities("N/A")
                .contact("N/A")
                .latitude(0.0)
                .longitude(0.0)
                .available(true)
                .farmImageFile(defaultImageFile)
                .build();
        farmRepository.save(otherFarmOption);
    }
}
