package com.project.farming.domain.plant.controller;

import com.project.farming.domain.plant.dto.PlantRequest;
import com.project.farming.domain.plant.dto.PlantResponse;
import com.project.farming.domain.plant.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Plant API", description = "AI 기능을 사용할 수 있는 식물 관련 API")
@RequestMapping("/api/plants")
@RequiredArgsConstructor
@RestController
public class PlantController {

    private final PlantService plantService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "새로운 식물 정보 등록",
            description = """
                    AI 기능을 활용할 수 있는 새로운 식물 정보를 등록합니다.
                    식물 정보는 JSON 형태로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    Content-Type은 multipart/form-data입니다.
                    """
//            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    content = @Content(
//                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
//                            schema = @Schema(implementation = PlantRequest.class)
//                    )
//            )
    )
    public ResponseEntity<PlantResponse> createPlant(
            @Parameter(description = "식물 정보(JSON)")
            @Valid @RequestPart("data") PlantRequest request,
            @Parameter(description = "업로드할 식물 이미지 파일")
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plantService.savePlant(request, file));
    }

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

    @PutMapping(value = "/{plantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "특정 식물 정보 수정",
            description = """
                    식물 ID에 해당하는 식물의 정보를 수정합니다.
                    식물 정보는 JSON 형태로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    Content-Type은 multipart/form-data입니다.
                    """
//            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    content = @Content(
//                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
//                            schema = @Schema(implementation = PlantRequest.class)
//                    )
//            )
    )
    public ResponseEntity<PlantResponse> updatePlant(@PathVariable Long plantId,
            @Parameter(description = "식물 정보(JSON)")
            @Valid @RequestPart("data") PlantRequest request,
            @Parameter(description = "업로드할 식물 이미지 파일")
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(plantService.updatePlant(plantId, request, file));
    }

    @DeleteMapping("/{plantId}")
    @Operation(summary = "특정 식물 정보 삭제", description = "식물 ID에 해당하는 식물의 정보를 삭제합니다.")
    public ResponseEntity<Void> deletePlant(@PathVariable Long plantId) {
        plantService.deletePlant(plantId);
        return ResponseEntity.noContent().build();
    }
}
