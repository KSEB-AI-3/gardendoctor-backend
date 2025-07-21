package com.project.farming.domain.farm.controller;

import com.project.farming.domain.farm.dto.FarmRequest;
import com.project.farming.domain.farm.dto.FarmResponse;
import com.project.farming.domain.farm.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Farm API", description = "텃밭 관련 API")
@RequestMapping("/api/farms")
@RequiredArgsConstructor
@RestController
public class FarmController {

    private final FarmService farmService;

    @PostMapping
    @Operation(summary = "텃밭 정보 추가", description = "새로운 텃밭 정보를 추가합니다.")
    public ResponseEntity<FarmResponse> createFarm(@Valid @RequestBody FarmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(farmService.saveFarm(request));
    }

    @GetMapping
    @Operation(summary = "텃밭 목록 조회", description = "DB에 등록된 모든 텃밭을 고유번호순으로 조회합니다.")
    public ResponseEntity<List<FarmResponse>> getAllFarms() {
        return ResponseEntity.ok(farmService.findAllFarms());
    }

    @GetMapping("/search")
    @Operation(summary = "텃밭 목록 검색 조회", 
            description = "사용자가 입력한 키워드(searchType: 텃밭명(name) 또는 주소(address))를 포함하는 모든 텃밭을 고유번호순으로 조회합니다.")
    public ResponseEntity<List<FarmResponse>> searchFarms(
            @Parameter(description = "검색 조건: 텃밭명(name) 또는 주소(address)")
            @RequestParam(defaultValue = "name") String searchType,
            @RequestParam String keyword) {
        return ResponseEntity.ok(farmService.findFarmsByKeyword(searchType, keyword));
    }

    @GetMapping("/{farmId}")
    @Operation(summary = "특정 텃밭 정보 조회", description = "텃밭 ID에 해당하는 텃밭의 상세 정보를 조회합니다.")
    public ResponseEntity<FarmResponse> getFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(farmService.findFarm(farmId));
    }

    @PutMapping("/{farmId}")
    @Operation(summary = "특정 텃밭 정보 수정", description = "텃밭 ID에 해당하는 텃밭의 정보를 수정합니다.")
    public ResponseEntity<FarmResponse> updateFarm(@PathVariable Long farmId, @Valid @RequestBody FarmRequest request) {
        return ResponseEntity.ok(farmService.updateFarm(farmId, request));
    }

    @DeleteMapping("/{farmId}")
    @Operation(summary = "특정 텃밭 정보 삭제", description = "텃밭 ID에 해당하는 텃밭의 정보를 삭제합니다.")
    public ResponseEntity<Void> deleteFarm(@PathVariable Long farmId) {
        farmService.deleteFarm(farmId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nearby")
    @Operation(summary = "주변 텃밭 정보 조회", description = "사용자의 현재 위치(위도, 경도)를 기준으로 원하는 반경 내(기본 20km)에 위치한 텃밭 정보를 조회합니다.")
    public ResponseEntity<List<FarmResponse>> getFarmsByLocation(
            @Parameter(description = "현재 위치의 위도") @RequestParam Double latitude,
            @Parameter(description = "현재 위치의 경도") @RequestParam Double longitude,
            @Parameter(description = "조회 반경(km 단위)") @RequestParam(defaultValue = "20") Double radius) {
        return ResponseEntity.ok(farmService.findFarmsByCurrentLocation (latitude, longitude, radius));
    }
}
