package com.project.farming.domain.plant.controller;

import com.project.farming.domain.plant.dto.PlantResponse;
import com.project.farming.domain.plant.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Plant API", description = "AI 기능을 사용할 수 있는 식물 관련 API")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping("/api/plants")
@RequiredArgsConstructor
@RestController
public class PlantController {

    private final PlantService plantService;

    @GetMapping
    @Operation(summary = "전체 식물 목록 조회", description = "DB에 등록된 모든 식물을 이름순으로 조회합니다.")
    public ResponseEntity<List<PlantResponse>> getAllPlants() {
        return ResponseEntity.ok(plantService.findAllPlants());
    }

    @GetMapping("/search")
    @Operation(summary = "식물 목록 검색",
            description = "사용자가 입력한 키워드(한글 또는 영어 식물 이름)를 포함하는 모든 식물을 이름순으로 조회합니다.")
    public ResponseEntity<List<PlantResponse>> searchPlants(@RequestParam String keyword) {
        return ResponseEntity.ok(plantService.findPlantsByKeyword(keyword));
    }

    @GetMapping("/{plantId}")
    @Operation(summary = "특정 식물 정보 조회", description = "식물 ID에 해당하는 식물의 상세 정보를 조회합니다.")
    public ResponseEntity<PlantResponse> getPlant(@PathVariable Long plantId) {
        return ResponseEntity.ok(plantService.findPlant(plantId));
    }
}
