package com.project.farming.domain.userplant.controller;

import com.project.farming.domain.userplant.dto.UserPlantRequest;
import com.project.farming.domain.userplant.service.UserPlantService;
import com.project.farming.domain.userplant.dto.UserPlantResponse;
import com.project.farming.global.jwtToken.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "UserPlant API", description = "사용자가 키우는 식물 관련 API")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping("/api/user-plants")
@RequiredArgsConstructor
@RestController
public class UserPlantController {

    private final UserPlantService userPlantService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사용자 식물 정보 등록",
            description = """
                    사용자가 키우는 식물의 정보를 등록합니다.
                    사용자 식물 정보는 JSON 형태로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    Content-Type은 multipart/form-data입니다.
                    """
    )
    public ResponseEntity<UserPlantResponse> createUserPlant(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "사용자 식물 정보(JSON), 식물 종류(Plant) 입력은 한글, 영어(소문자) 모두 가능합니다.")
            @Valid @RequestPart("data") UserPlantRequest request,
            @Parameter(description = "업로드할 사용자 식물 이미지 파일")
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Long userId = customUserDetails.getUser().getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userPlantService.saveUserPlant(userId, request, file));
    }

    @GetMapping
    @Operation(summary = "사용자 식물 목록 조회",
            description = """
                    사용자 ID에 해당하는 사용자가 등록한 모든 식물을 별명 순으로 조회합니다.
                    일부 정보만 반환합니다.
                    (userPlantId, plantName(식물 종류), plantNickname(식물 별명),
                     plantingPlace(심은 장소), userPlantImageUrl(이미지 URL))
                    """)
    public ResponseEntity<List<UserPlantResponse>> getAllUserPlants(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getUserId();
        return ResponseEntity.ok(userPlantService.findAllUserPlants(userId));
    }

    @GetMapping("/search")
    @Operation(summary = "사용자 식물 목록 검색",
            description = """
                    사용자 ID에 해당하는 사용자가 등록한 식물 중에서 입력한 키워드(식물 종류 또는 별명)를 포함하는
                     모든 식물을 별명 순으로 조회합니다. 일부 정보만 반환합니다.
                     (userPlantId, plantName(식물 종류), plantNickname(식물 별명),
                     plantingPlace(심은 장소), userPlantImageUrl(이미지 URL))
                    """
    )
    public ResponseEntity<List<UserPlantResponse>> searchUserPlants(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam String keyword) {
        Long userId = customUserDetails.getUser().getUserId();
        return ResponseEntity.ok(userPlantService.findUserPlantsByKeyword(userId, keyword));
    }

    @GetMapping("/{userPlantId}")
    @Operation(summary = "특정 사용자 식물 정보 조회",
            description = """
                    사용자 ID에 해당하는 사용자의 특정 식물의 상세 정보를 조회합니다.
                    식물(Plant)의 상세 정보도 반환합니다.
                    (plantEnglishName(식물 영문 이름), species(식물 분류), season(계절), plantImageUrl(이미지 URL))
                    """)
    public ResponseEntity<UserPlantResponse> getUserPlant(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long userPlantId) {
        Long userId = customUserDetails.getUser().getUserId();
        return ResponseEntity.ok(userPlantService.findUserPlant(userId, userPlantId));
    }

    @PutMapping(value = "/{userPlantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "특정 사용자 식물 정보 수정",
            description = """
                    사용자 ID에 해당하는 사용자의 특정 식물의 정보를 수정합니다.
                    1. 기본 식물 12종 외의 사용자 입력 식물이라면 식물 종류(plantName)을 수정할 수 있습니다.
                    2. 식물의 별명(plantNickname), 메모(notes), 심은 장소(plantingPlace, 옮겨 심는 경우),
                       식물의 상태(물/가지치기/영양제 여부)와 물/가지치기/영양제 수행 주기(일 단위), 알림 수신 여부를 수정할 수 있습니다.
                    3. 사용자 식물 정보는 JSON 형태로 전달하며, 이미지 파일은 선택적으로 함께 첨부할 수 있습니다.
                    4. Content-Type은 multipart/form-data입니다.
                    """
    )
    public ResponseEntity<UserPlantResponse> updateUserPlant(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long userPlantId,
            @Parameter(description = "사용자 식물 정보(JSON)")
            @Valid @RequestPart("data") UserPlantRequest request,
            @Parameter(description = "업로드할 사용자 식물 이미지 파일")
            @RequestPart(value = "file", required = false) MultipartFile file) {
        Long userId = customUserDetails.getUser().getUserId();
        return ResponseEntity.ok(userPlantService.updateUserPlant(userId, userPlantId, request, file));
    }

    @DeleteMapping("/{userPlantId}")
    @Operation(summary = "특정 사용자 식물 정보 삭제", description = "사용자 ID에 해당하는 사용자의 특정 식물의 정보를 삭제합니다.")
    public ResponseEntity<Void> deleteUserPlant(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long userPlantId) {
        Long userId = customUserDetails.getUser().getUserId();
        userPlantService.deleteUserPlant(userId, userPlantId);
        return ResponseEntity.noContent().build();
    }
}
