package com.project.farming.domain.farm.controller;

import com.project.farming.domain.farm.dto.FarmRequest;
import com.project.farming.domain.farm.dto.FarmResponse;
import com.project.farming.domain.farm.service.FarmService;
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

@Tag(name = "Farm Admin API", description = "텃밭 관련 **관리자 전용** API")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping("/api/admin/farms")
@RequiredArgsConstructor
@RestController
public class FarmAdminController {

    private final FarmService farmService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "새로운 텃밭 정보 등록 (관리자 전용)",
            description = """
                    새로운 텃밭 정보를 등록합니다.
                    **관리자만 접근 가능합니다.**
                    텃밭 정보는 JSON 형태로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    Content-Type은 multipart/form-data입니다.
                    """
    )
    public ResponseEntity<FarmResponse> createFarm(
            @Parameter(description = "텃밭 정보(JSON)")
            @Valid @RequestPart("data") FarmRequest request,
            @Parameter(description = "업로드할 텃밭 이미지 파일")
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(farmService.saveFarm(request, file));
    }

    @PutMapping(value = "/{farmId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "특정 텃밭 정보 수정 (관리자 전용)",
            description = """
                    텃밭 ID에 해당하는 텃밭의 정보를 수정합니다.
                    **관리자만 접근 가능합니다.**
                    텃밭 정보는 JSON 형태로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    Content-Type은 multipart/form-data입니다.
                    """
    )
    public ResponseEntity<FarmResponse> updateFarm(@PathVariable Long farmId,
            @Parameter(description = "텃밭 정보(JSON)")
            @Valid @RequestPart("data") FarmRequest request,
            @Parameter(description = "업로드할 텃밭 이미지 파일")
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(farmService.updateFarm(farmId, request, file));
    }

    @DeleteMapping("/{farmId}")
    @Operation(summary = "특정 텃밭 정보 삭제 (관리자 전용)",
            description = "텃밭 ID에 해당하는 텃밭의 정보를 삭제합니다. **관리자만 접근 가능합니다.**")
    public ResponseEntity<Void> deleteFarm(@PathVariable Long farmId) {
        farmService.deleteFarm(farmId);
        return ResponseEntity.noContent().build();
    }
}
