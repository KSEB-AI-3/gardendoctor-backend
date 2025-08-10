package com.project.farming.domain.farm.controller;

import com.project.farming.domain.farm.dto.FarmResponse;
import com.project.farming.domain.farm.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Farm API", description = "텃밭 관련 API")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping("/api/farms")
@RequiredArgsConstructor
@RestController
public class FarmController {

    private final FarmService farmService;

    @GetMapping
    @Operation(summary = "전체 텃밭 목록 조회",
            description = """
                    DB에 등록된 모든 텃밭을 고유번호순으로 조회합니다.
                    일부 정보만 반환합니다.
                    (farmId, gardenUniqueId(고유번호), operator(운영주체), farmName(텃밭 이름),
                     lotNumberAddress(주소), updatedAt(최종 수정일), farmImageUrl(이미지 URL))
                    """)
    public ResponseEntity<List<FarmResponse>> getAllFarms() {
        return ResponseEntity.ok(farmService.findAllFarms());
    }

    @GetMapping("/search")
    @Operation(summary = "텃밭 목록 검색",
            description = """
                    사용자가 입력한 키워드(텃밭 이름 또는 도로명/지번 주소)를 포함하는 모든 텃밭을 고유번호순으로 조회합니다.
                    일부 정보만 반환합니다.
                    (farmId, gardenUniqueId(고유번호), operator(운영주체), farmName(텃밭 이름),
                     lotNumberAddress(주소), updatedAt(최종 수정일), farmImageUrl(이미지 URL))
                    """)
    public ResponseEntity<List<FarmResponse>> searchFarms(
            @Parameter(description = "텃밭 이름 또는 주소(도로명/지번)")
            @RequestParam String keyword) {
        return ResponseEntity.ok(farmService.findFarmsByKeyword(keyword));
    }

    @GetMapping("/{farmId}")
    @Operation(summary = "특정 텃밭 정보 조회", description = "텃밭 ID에 해당하는 텃밭의 상세 정보를 조회합니다. 전체 정보를 반환합니다.")
    public ResponseEntity<FarmResponse> getFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(farmService.findFarm(farmId));
    }

    @GetMapping("/nearby")
    @Operation(summary = "주변 텃밭 정보 조회",
            description = """
                    사용자의 현재 위치(위도, 경도)를 기준으로 원하는 반경(km 단위, 기본값: 20km) 내에 위치한 텃밭 정보를 조회합니다.
                    전체 정보를 반환합니다.
                    """)
    public ResponseEntity<List<FarmResponse>> getFarmsByLocation(
            @Parameter(description = "현재 위치의 위도") @RequestParam Double latitude,
            @Parameter(description = "현재 위치의 경도") @RequestParam Double longitude,
            @Parameter(description = "조회 반경(km 단위, 기본값: 20km)") @RequestParam(defaultValue = "20") Double radius) {
        return ResponseEntity.ok(farmService.findFarmsByCurrentLocation(latitude, longitude, radius));
    }
}
