package com.project.farming.domain.plant.controller;

import com.project.farming.domain.plant.dto.PlantRequest;
import com.project.farming.domain.plant.dto.PlantResponse;
import com.project.farming.domain.plant.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Plant Admin API", description = "AI 기능을 사용할 수 있는 식물 관련 **관리자 전용** API")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping("/api/admin/plants")
@RequiredArgsConstructor
@RestController
public class PlantAdminController {

    private final PlantService plantService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "새로운 식물 정보 등록 (관리자 전용)",
            description = """
                    AI 기능을 활용할 수 있는 새로운 식물 정보를 등록합니다.
                    **관리자만 접근 가능합니다.**
                    식물 정보는 JSON 형태로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    Content-Type은 multipart/form-data입니다.
                    """
    )
    public ResponseEntity<PlantResponse> createPlant(
            @Parameter(description = "식물 정보(JSON)")
            @Valid @RequestPart("data") PlantRequest request,
            @Parameter(description = "업로드할 식물 이미지 파일")
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plantService.savePlant(request, file));
    }

    @PutMapping(value = "/{plantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "특정 식물 정보 수정 (관리자 전용)",
            description = """
                    식물 ID에 해당하는 식물의 정보를 수정합니다.
                    **관리자만 접근 가능합니다.**
                    식물 정보는 JSON 형태로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    Content-Type은 multipart/form-data입니다.
                    """
    )
    public ResponseEntity<PlantResponse> updatePlant(@PathVariable Long plantId,
            @Parameter(description = "식물 정보(JSON)")
            @Valid @RequestPart("data") PlantRequest request,
            @Parameter(description = "업로드할 식물 이미지 파일")
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(plantService.updatePlant(plantId, request, file));
    }

    @DeleteMapping("/{plantId}")
    @Operation(summary = "특정 식물 정보 삭제 (관리자 전용)",
            description = "식물 ID에 해당하는 식물의 정보를 삭제합니다. **관리자만 접근 가능합니다.**")
    public ResponseEntity<Void> deletePlant(@PathVariable Long plantId) {
        plantService.deletePlant(plantId);
        return ResponseEntity.noContent().build();
    }
}
