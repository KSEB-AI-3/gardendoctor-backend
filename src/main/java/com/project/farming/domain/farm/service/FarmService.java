package com.project.farming.domain.farm.service;

import com.project.farming.domain.farm.dto.FarmRequestDto;
import com.project.farming.domain.farm.dto.FarmResponseDto;
import com.project.farming.domain.farm.entity.FarmInfo;
import com.project.farming.domain.farm.repository.FarmInfoRepository;
import com.project.farming.global.exception.FarmNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class FarmService {

    private final FarmInfoRepository farmInfoRepository;

    @Transactional
    public FarmResponseDto saveFarm(FarmRequestDto request) {
        if (farmInfoRepository.existsByGardenUniqueId(request.getGardenUniqueId())) {
            throw new IllegalArgumentException("이미 존재하는 텃밭입니다: " + request.getGardenUniqueId());
        }
        FarmInfo newFarm = FarmInfo.builder()
                .gardenUniqueId(request.getGardenUniqueId())
                .operator(request.getOperator())
                .name(request.getName())
                .roadNameAddress(request.getRoadNameAddress())
                .lotNumberAddress(request.getLotNumberAddress())
                .facilities(request.getFacilities())
                .available(request.getAvailable())
                .contact(request.getContact())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(request.getImageUrl())
                .build();
        FarmInfo savedFarm = farmInfoRepository.save(newFarm);
        return FarmResponseDto.builder()
                .message("해당 텃밭이 성공적으로 등록되었습니다.")
                .gardenUniqueId(savedFarm.getGardenUniqueId())
                .build();
    }
    
    public List<FarmResponseDto> findAllFarms() {
        List<FarmInfo> foundFarms = farmInfoRepository.findAllByOrderByGardenUniqueIdAsc();
        if (foundFarms.isEmpty()) {
            throw new FarmNotFoundException("등록된 텃밭이 없습니다.");
        }
        return foundFarms.stream()
                .map(farm -> FarmResponseDto.builder()
                        .farmId(farm.getFarmId())
                        .gardenUniqueId(farm.getGardenUniqueId())
                        .operator(farm.getOperator())
                        .name(farm.getName())
                        .updatedAt(farm.getUpdatedAt())
                        .imageUrl(farm.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    public FarmResponseDto findFarm(Long farmId) {
        FarmInfo foundFarm = farmInfoRepository.findById(farmId)
                .orElseThrow(() -> new FarmNotFoundException("해당 텃밭이 존재하지 않습니다: " + farmId));
        return FarmResponseDto.builder()
                .farmId(foundFarm.getFarmId())
                .gardenUniqueId(foundFarm.getGardenUniqueId())
                .operator(foundFarm.getOperator())
                .name(foundFarm.getName())
                .roadNameAddress(foundFarm.getRoadNameAddress())
                .lotNumberAddress(foundFarm.getLotNumberAddress())
                .facilities(foundFarm.getFacilities())
                .available(foundFarm.getAvailable())
                .contact(foundFarm.getContact())
                .latitude(foundFarm.getLatitude())
                .longitude(foundFarm.getLongitude())
                .updatedAt(foundFarm.getUpdatedAt())
                .imageUrl(foundFarm.getImageUrl())
                .build();
    }

    @Transactional
    public FarmResponseDto updateFarm(Long farmId, FarmRequestDto request) {
        FarmInfo farm = farmInfoRepository.findById(farmId)
                .orElseThrow(() -> new FarmNotFoundException("해당 텃밭이 존재하지 않습니다: " + farmId));
        farm.updateFarmInfo(request.getGardenUniqueId(), request.getOperator(), request.getName(),
                request.getRoadNameAddress(), request.getLotNumberAddress(), request.getFacilities(),
                request.getAvailable(), request.getContact(), request.getLatitude(), request.getLongitude(),
                request.getImageUrl());
        FarmInfo updatedFarm = farmInfoRepository.save(farm);
        return FarmResponseDto.builder()
                .message("해당 텃밭 정보가 성공적으로 수정되었습니다.")
                .gardenUniqueId(updatedFarm.getGardenUniqueId())
                .build();
    }

    @Transactional
    public void deleteFarm(Long farmId) {
        FarmInfo farm = farmInfoRepository.findById(farmId)
                .orElseThrow(() -> new FarmNotFoundException("해당 텃밭이 존재하지 않습니다: " + farmId));
        farmInfoRepository.delete(farm);
    }
}
