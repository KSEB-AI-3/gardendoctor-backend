package com.project.farming.domain.plant.controller;

import com.project.farming.domain.plant.dto.PlantRequest;
import com.project.farming.domain.plant.dto.PlantResponse;
import com.project.farming.domain.plant.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Plant API", description = "AI 기능을 사용할 수 있는 식물 관련 API")
@RequestMapping("/api/plants")
@RequiredArgsConstructor
@RestController
public class PlantController {

    private final PlantService plantService;

    @PostMapping
    @Operation(summary = "식물 추가", description = "AI 기능을 사용할 수 있는 식물을 추가하는 기능")
    public ResponseEntity<PlantResponse> createPlant(@Valid @RequestBody PlantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plantService.savePlant(request));
    }

    @GetMapping
    @Operation(summary = "식물 목록 조회", description = "AI 기능을 사용할 수 있는 모든 식물을 이름순으로 조회할 수 있는 기능")
    public ResponseEntity<List<PlantResponse>> getAllPlants() {
        return ResponseEntity.ok(plantService.findAllPlants());
    }

    @GetMapping("/{plantId}")
    @Operation(summary = "식물 정보 조회", description = "AI 기능을 사용할 수 있는 식물 1개의 정보를 조회할 수 있는 기능")
    public ResponseEntity<PlantResponse> getPlant(@PathVariable Long plantId) {
        return ResponseEntity.ok(plantService.findPlant(plantId));
    }

    @PutMapping("/{plantId}")
    @Operation(summary = "식물 정보 수정", description = "AI 기능을 사용할 수 있는 식물 1개의 정보를 수정하는 기능")
    public ResponseEntity<PlantResponse> updatePlant(@PathVariable Long plantId, @Valid @RequestBody PlantRequest request) {
        return ResponseEntity.ok(plantService.updatePlant(plantId, request));
    }

    @DeleteMapping("/{plantId}")
    @Operation(summary = "식물 정보 삭제", description = "AI 기능을 사용할 수 있는 식물 1개의 정보를 삭제하는 기능")
    public ResponseEntity<PlantResponse> deletePlant(@PathVariable Long plantId) {
        plantService.deletePlant(plantId);
        return ResponseEntity.noContent().build();
    }
}
