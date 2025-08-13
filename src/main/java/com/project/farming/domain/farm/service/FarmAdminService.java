package com.project.farming.domain.farm.service;

import com.project.farming.domain.farm.dto.FarmAdminRequest;
import com.project.farming.domain.farm.dto.FarmResponse;
import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.farm.repository.FarmRepository;
import com.project.farming.domain.userplant.repository.UserPlantRepository;
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
@RequiredArgsConstructor
@Service
public class FarmAdminService {

    private final FarmRepository farmRepository;
    private final ImageFileService imageFileService;
    private final ImageFileRepository imageFileRepository;
    private final UserPlantRepository userPlantRepository;

    /**
     * 새로운 텃밭 정보 등록
     * - 값 미입력시 기본값으로 저장
     *
     * @param request 등록할 텃밭 정보
     * @param file 업로드할 텃밭 이미지 파일 (선택적)
     */
    @Transactional
    public void saveFarm(FarmAdminRequest request, MultipartFile file) {
        if (farmRepository.existsByGardenUniqueId(request.getGardenUniqueId())) {
            log.error("이미 존재하는 텃밭입니다: {}", request.getGardenUniqueId());
            throw new IllegalArgumentException("이미 존재하는 텃밭입니다: " + request.getGardenUniqueId());
        }
        ImageFile defaultImageFile = getDefaultImageFile();
        Farm newFarm = Farm.builder()
                .gardenUniqueId(request.getGardenUniqueId())
                .operator(getOrDefault(request.getOperator()))
                .farmName(getOrDefault(request.getFarmName()))
                .roadNameAddress(getOrDefault(request.getRoadNameAddress()))
                .lotNumberAddress(getOrDefault(request.getLotNumberAddress()))
                .facilities(getOrDefault(request.getFacilities()))
                .contact(getOrDefault(request.getContact()))
                .latitude(getOrDefault(request.getLatitude()))
                .longitude(getOrDefault(request.getLongitude()))
                .available(request.getAvailable())
                .farmImageFile(defaultImageFile)
                .build();
        Farm savedFarm = farmRepository.save(newFarm);
        Long farmId = savedFarm.getFarmId();

        if (file != null && !file.isEmpty()) {
            // 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.uploadImage(file, ImageDomainType.FARM, farmId);
            savedFarm.updateFarmImage(imageFile);
        }
    }

    /**
     * 텃밭 목록 검색(고유번호순)
     * - 텃밭의 이름으로 검색
     * - 텃밭의 주소(도로명주소, 지번주소)로 검색
     * - 일부 정보만 반환
     *
     * @param searchType 검색 조건(name 또는 address) - 기본값은 name
     * @param keyword 검색어(텃밭 이름 또는 주소)
     * @return 검색된 텃밭 정보의 Response DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<FarmResponse> findFarmsByKeyword(String searchType, String keyword) {
        List<Farm> foundFarms = switch (searchType) {
            case "name" -> farmRepository.findByFarmNameContainingOrderByGardenUniqueIdAsc(keyword);
            case "address" -> farmRepository.findByAddressContainingOrderByGardenUniqueIdAsc("%"+keyword+"%");
            default -> {
                log.error("지원하지 않는 검색 조건입니다: {}", searchType);
                throw new IllegalArgumentException("지원하지 않는 검색 조건입니다: " + searchType);
            }
        };
        return foundFarms.stream()
                .map(farm -> FarmResponse.builder()
                        .farmId(farm.getFarmId())
                        .gardenUniqueId(farm.getGardenUniqueId())
                        .operator(farm.getOperator())
                        .farmName(farm.getFarmName())
                        .lotNumberAddress(farm.getLotNumberAddress())
                        .updatedAt(farm.getUpdatedAt())
                        .farmImageUrl(farm.getFarmImageFile().getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 텃밭 정보 수정
     * - 값 미입력시 기본값으로 수정
     *
     * @param farmId 수정할 텃밭 정보의 ID
     * @param request 새로 저장할 텃밭 정보
     * @param newFile 새로 업로드할 텃밭 이미지 파일 (선택적)
     */
    @Transactional
    public void updateFarm(Long farmId, FarmAdminRequest request, MultipartFile newFile) {
        Farm farm = findFarmById(farmId);
        if (newFile != null && !newFile.isEmpty()) {
            // 새로운 이미지 파일이 첨부되어 있는 경우
            ImageFile imageFile = imageFileService.updateImage(
                    farm.getFarmImageFile().getImageFileId(), // 기존 이미지 파일
                    newFile, ImageDomainType.FARM, farmId);
            farm.updateFarmImage(imageFile);
        }
        farm.updateFarmInfo(request.getGardenUniqueId(),
                getOrDefault(request.getOperator()), getOrDefault(request.getFarmName()),
                getOrDefault(request.getRoadNameAddress()), getOrDefault(request.getLotNumberAddress()),
                getOrDefault(request.getFacilities()), getOrDefault(request.getContact()),
                getOrDefault(request.getLatitude()), getOrDefault(request.getLongitude()), request.getAvailable());
        farmRepository.save(farm);
    }

    /**
     * 특정 텃밭 정보 삭제
     * - 삭제할 텃밭과 매핑된 userPlant가 있다면
     *   해당 userPlant의 텃밭을 '기타(Other)'로 변경
     *
     * @param farmId 삭제할 텃밭 정보의 ID
     */
    @Transactional
    public void deleteFarm(Long farmId) {
        Farm farm = findFarmById(farmId);
        if ("기타(Other)".equals(farm.getFarmName())) {
            log.error("기본 텃밭 정보는 삭제할 수 없습니다.");
            throw new RuntimeException("기본 텃밭 정보는 삭제할 수 없습니다.");
        }
        Farm otherFarm = farmRepository.getOtherFarm("기타(Other)")
                .orElseThrow(() -> {
                    log.error("DB에 '기타(Other)' 항목이 존재하지 않습니다.");
                    return new FarmNotFoundException("DB에 '기타(Other)' 항목이 존재하지 않습니다.");
                });
        int updatedCount = userPlantRepository.reassignFarm(otherFarm, farm);
        if (updatedCount == 0) log.info("해당 텃밭({})과 매핑된 사용자 식물이 없습니다.", farmId);
        else log.info(
                "해당 텃밭({})과 매핑된 사용자 식물 {}개의 텃밭 정보가 '기타(Other)'로 수정되었습니다.", farmId, updatedCount);
        farmRepository.delete(farm);
        imageFileService.deleteImage(farm.getFarmImageFile().getImageFileId()); // 기존 이미지 파일
    }

    /**
     * 텃밭 기본 이미지 반환
     *
     * @return 텃밭 기본 이미지
     */
    private ImageFile getDefaultImageFile() {
        return imageFileRepository.findByS3Key(DefaultImages.DEFAULT_FARM_IMAGE)
                .orElseThrow(() -> {
                    log.error("기본 텃밭 이미지가 존재하지 않습니다.");
                    return new ImageFileNotFoundException("기본 텃밭 이미지가 존재하지 않습니다.");
                });
    }

    /**
     * 기본값 설정(String)
     *
     * @param val 확인할 값
     * @return 기본값 또는 request 값
     */
    private String getOrDefault(String val) {
        return val == null || val.isBlank() || val.equals("-") ? "N/A" : val;
    }

    /**
     * 기본값 설정(Double)
     *
     * @param val 확인할 값
     * @return 기본값 또는 request 값
     */
    private Double getOrDefault(Double val) {
        return val == null ? 0.0 : val;
    }

    /**
     * ID로 텃밭 정보 조회
     *
     * @param farmId 조회할 텃밭 정보의 ID
     * @return 조회한 텃밭 정보
     */
    private Farm findFarmById(Long farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> {
                    log.error("해당 텃밭이 존재하지 않습니다: {}", farmId);
                    return new FarmNotFoundException("해당 텃밭이 존재하지 않습니다: " + farmId);
                });
    }
}
