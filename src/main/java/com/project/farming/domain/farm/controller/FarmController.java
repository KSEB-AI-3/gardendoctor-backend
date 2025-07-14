package com.project.farming.domain.farm.controller;

import com.project.farming.domain.farm.dto.FarmRequest;
import com.project.farming.domain.farm.dto.FarmResponse;
import com.project.farming.domain.farm.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "텃밭 추가", description = "텃밭 정보를 추가하는 기능")
    public ResponseEntity<FarmResponse> createFarm(@Valid @RequestBody FarmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(farmService.saveFarm(request));
    }

    @GetMapping
    @Operation(summary = "텃밭 목록 조회", description = "모든 텃밭 정보를 고유번호순으로 조회할 수 있는 기능")
    public ResponseEntity<List<FarmResponse>> getAllFarms() {
        return ResponseEntity.ok(farmService.findAllFarms());
    }

    @GetMapping("/{farmId}")
    @Operation(summary = "텃밭 정보 조회", description = "텃밭 1개의 정보를 조회할 수 있는 기능")
    public ResponseEntity<FarmResponse> getFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(farmService.findFarm(farmId));
    }

    @PutMapping("/{farmId}")
    @Operation(summary = "텃밭 정보 수정", description = "텃밭 1개의 정보를 수정하는 기능")
    public ResponseEntity<FarmResponse> updateFarm(@PathVariable Long farmId, @Valid @RequestBody FarmRequest request) {
        return ResponseEntity.ok(farmService.updateFarm(farmId, request));
    }

    @DeleteMapping("/{farmId}")
    @Operation(summary = "텃밭 정보 삭제", description = "텃밭 1개의 정보를 삭제하는 기능")
    public ResponseEntity<FarmResponse> deleteFarm(@PathVariable Long farmId) {
        farmService.deleteFarm(farmId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "주변 텃밭 정보 조회", description = "사용자의 현재 위치를 기반으로 주변의 모든 텃밭 정보를 조회할 수 있는 기능")
    public ResponseEntity<List<FarmResponse>> getFarmsByLocation(
            @RequestParam Double latitude, @RequestParam Double longitude,
            @RequestParam(defaultValue = "20") Double radius) {
        return ResponseEntity.ok(farmService.findFarmsByCurrentLocation (latitude, longitude, radius));
    }
}
