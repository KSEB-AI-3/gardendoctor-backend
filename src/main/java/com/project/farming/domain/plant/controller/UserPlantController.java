package com.project.farming.domain.plant.controller;

import com.project.farming.domain.plant.dto.UserPlantRequest;
import com.project.farming.domain.plant.dto.UserPlantResponse;
import com.project.farming.domain.plant.service.UserPlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UserPlant Api", description = "사용자가 키우는 식물 관련 API")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping("/api/user-plants")
@RequiredArgsConstructor
@RestController
public class UserPlantController {

    private final UserPlantService userPlantService;

    @PostMapping
    @Operation(summary = "사용자 식물 등록", description = "사용자가 키우는 식물을 등록하는 기능")
    public ResponseEntity<UserPlantResponse> createUserPlant(@Valid @RequestBody UserPlantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userPlantService.saveUserPlant(request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 식물 목록 조회", description = "사용자가 키우는 모든 식물을 별명순으로 조회할 수 있는 기능")
    public ResponseEntity<List<UserPlantResponse>> getAllUserPlants(@PathVariable Long userId) {
        return ResponseEntity.ok(userPlantService.findAllUserPlants(userId));
    }

    @GetMapping("/{userId}/{userPlantId}")
    @Operation(summary = "사용자 식물 정보 조회", description = "사용자가 키우는 식물 1개의 정보를 조회할 수 있는 기능")
    public ResponseEntity<UserPlantResponse> getUserPlant(@PathVariable Long userId, @PathVariable Long userPlantId) {
        return ResponseEntity.ok(userPlantService.findUserPlant(userId, userPlantId));
    }

    @PutMapping("/{userId}/{userPlantId}")
    @Operation(summary = "사용자 식물 정보 수정", description = "사용자가 키우는 식물 1개의 정보를 수정하는 기능")
    public ResponseEntity<UserPlantResponse> updateUserPlant(
            @PathVariable Long userId, @PathVariable Long userPlantId, @Valid @RequestBody UserPlantRequest request) {
        return ResponseEntity.ok(userPlantService.updateUserPlant(userId, userPlantId, request));
    }

    @DeleteMapping("/{userId}/{userPlantId}")
    @Operation(summary = "사용자 식물 정보 삭제", description = "사용자가 키우는 식물 1개의 정보를 삭제하는 기능")
    public ResponseEntity<UserPlantResponse> deleteUserPlant(@PathVariable Long userId, @PathVariable Long userPlantId) {
        userPlantService.deleteUserPlant(userId, userPlantId);
        return ResponseEntity.noContent().build();
    }
}
