// src/main/java/com/project/farming/domain/diary/controller/DiaryController.java
package com.project.farming.domain.diary.controller;

import com.project.farming.domain.diary.dto.DiaryRequest;
import com.project.farming.domain.diary.dto.DiaryResponse;
import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.diary.service.DiaryService;
import com.project.farming.global.jwtToken.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema; // 올바른 Schema 임포트
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Diary API", description = "일지(Diary) 관련 API")
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@SecurityRequirement(name = "jwtAuth")
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 새로운 일지 생성
     * POST /api/diaries
     * @param customUserDetails 현재 로그인한 사용자 (인증 정보에서 추출)
     * @param diaryRequest 일지 생성 요청 DTO (JSON 데이터)
     * @param imageFile 업로드할 이미지 파일 (선택 사항)
     * @return 생성된 일지 응답 DTO
     */
    @Operation(
            summary = "새 일지 생성",
            description = """
        새로운 일지를 생성합니다.
        - `request`: JSON 형식의 DiaryRequest
        - `imageFile`: 첨부 이미지 (선택)
        **주의:** Content-Type은 multipart/form-data로 설정하여 업로드하세요.
        request는 JSON 문자열로 전송해야 합니다.
        """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "일지 생성 성공", content = @Content(schema = @Schema(implementation = DiaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
                    @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponse> createDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestPart("diaryRequest") @Valid DiaryRequest diaryRequest,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Diary diary = diaryService.createDiary(
                customUserDetails.getUser(),
                diaryRequest.getTitle(),
                diaryRequest.getContent(),
                imageFile,
                diaryRequest.getWatered(),
                diaryRequest.getPruned(),
                diaryRequest.getFertilized(),
                diaryRequest.getSelectedUserPlantIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new DiaryResponse(diary));
    }

    /**
     * 특정 일지 조회
     */
    @Operation(summary = "특정 일지 조회", description = "특정 ID에 해당하는 일지의 상세 정보를 조회합니다.")
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long diaryId) {
        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 서비스에서 DTO를 직접 받아서 반환
        DiaryResponse response = diaryService.getDiaryById(diaryId, customUserDetails.getUser());
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자의 모든 일지 조회 (캘린더 기본 뷰 - 최신순)
     * GET /api/diaries/my-diaries
     * @param customUserDetails 현재 로그인한 사용자
     * @return 일지 목록 응답 DTO
     */
    @Operation(summary = "특정 사용자의 모든 일지 조회 (최신순)", description = "현재 로그인된 사용자가 작성한 모든 일지 목록을 최신순으로 조회합니다.")
    @GetMapping("/my-diaries")
    public ResponseEntity<List<DiaryResponse>> getAllMyDiaries(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 서비스에서 DTO 목록을 직접 받아서 반환
        List<DiaryResponse> responses = diaryService.getAllDiariesByUser(customUserDetails.getUser());
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 사용자의 특정 기간 동안의 일지 조회 (캘린더 날짜별 정렬)
     * GET /api/diaries/my-diaries/by-date
     * @param customUserDetails 현재 로그인한 사용자
     * @param startDate 시작 날짜/시간 (ISO 8601 형식)
     * @param endDate 종료 날짜/시간 (ISO 8601 형식)
     * @return 일지 목록 응답 DTO
     */
    @Operation(summary = "특정 기간 동안의 일지 조회", description = "현재 로그인된 사용자가 작성한 일지 중 특정 기간 내의 일지 목록을 조회합니다. 날짜별 캘린더 조회에 사용됩니다.")
    @GetMapping("/my-diaries/by-date")
    public ResponseEntity<List<DiaryResponse>> getMyDiariesByDateRange(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "검색 시작 날짜/시간 (ISO 8601 형식, 예: 2024-07-01T00:00:00)") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "검색 종료 날짜/시간 (ISO 8601 형식, 예: 2024-07-31T23:59:59)") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 서비스에서 DTO 목록을 직접 받아서 반환
        List<DiaryResponse> responses = diaryService.getDiariesByUserAndDateRange(customUserDetails.getUser(), startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 사용자 식물(UserPlant)에 연결된 일지 조회 (닉네임 기반 태그 검색)
     * GET /api/diaries/my-diaries/by-user-plant/{userPlantId}
     * @param customUserDetails 현재 로그인한 사용자
     * @param userPlantId 검색할 UserPlant ID
     * @return 해당 UserPlant에 연결된 일지 목록 응답 DTO
     */
    @Operation(summary = "특정 사용자 식물(UserPlant)에 연결된 일지 조회", description = "특정 UserPlant에 연결된 현재 사용자의 모든 일지를 조회합니다. 클라이언트 드롭다운에서 특정 닉네임의 작물을 선택하여 일지를 필터링할 때 사용됩니다.")
    @GetMapping("/my-diaries/by-user-plant/{userPlantId}")
    public ResponseEntity<List<DiaryResponse>> getMyDiariesByUserPlant(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "검색할 사용자 식물 ID") @PathVariable Long userPlantId) {
        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 서비스에서 DTO 목록을 직접 받아서 반환
        List<DiaryResponse> responses = diaryService.getDiariesByUserAndUserPlant(customUserDetails.getUser(), userPlantId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 여러 사용자 식물(UserPlant) 중 하나라도 연결된 일지 조회 (다중 태그 검색)
     * GET /api/diaries/my-diaries/by-user-plants?ids=1,2,3
     * @param customUserDetails 현재 로그인한 사용자
     * @param userPlantIds 검색할 UserPlant ID 목록 (콤마로 구분)
     * @return 해당 UserPlant 중 하나라도 연결된 일지 목록 응답 DTO
     */
    @Operation(summary = "여러 사용자 식물(UserPlant) 중 하나라도 연결된 일지 조회", description = "현재 사용자가 등록한 여러 UserPlant 중 하나라도 연결된 일지 목록을 조회합니다. 다중 태그 검색과 유사합니다.")
    @GetMapping("/my-diaries/by-user-plants")
    public ResponseEntity<List<DiaryResponse>> getMyDiariesByUserPlants(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "콤마로 구분된 사용자 식물 ID 목록 (예: 101,103)") @RequestParam List<Long> userPlantIds) {
        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 서비스에서 DTO 목록을 직접 받아서 반환
        List<DiaryResponse> responses = diaryService.getDiariesByUserAndUserPlants(customUserDetails.getUser(), userPlantIds);
        return ResponseEntity.ok(responses);
    }


    /**
     * 일지 수정
     * PUT /api/diaries/{diaryId}
     * @param customUserDetails 현재 로그인한 사용자
     * @param diaryId 수정할 일지 ID
     * @param request 일지 수정 요청 DTO (JSON 데이터)
     * @param newImageFile 새로운 이미지 파일 (선택 사항)
     * @return 수정된 일지 응답 DTO
     */
    @Operation(
            summary = "일지 수정",
            description = """
                특정 ID의 일지 정보를 수정합니다.
                
                - `request`: JSON 형식의 DiaryRequest (`deleteExistingImage` 포함)
                - `newImageFile`: 새 이미지 파일 (선택, 기존 이미지 교체용)
                
                **Content-Type:** multipart/form-data
                """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "일지 수정 성공", content = @Content(schema = @Schema(implementation = DiaryResponse.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "404", description = "일지를 찾을 수 없음")
            }
    )
    @PutMapping(value = "/{diaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponse> updateDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long diaryId,
            @RequestPart("request") @Valid DiaryRequest request,
            @RequestPart(value = "newImageFile", required = false) MultipartFile newImageFile) {

        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Diary updatedDiary = diaryService.updateDiary(
                diaryId,
                customUserDetails.getUser(),
                request.getTitle(),
                request.getContent(),
                newImageFile,
                request.isDeleteExistingImage(),
                request.getWatered(),
                request.getPruned(),
                request.getFertilized(),
                request.getSelectedUserPlantIds()
        );
        return ResponseEntity.ok(new DiaryResponse(updatedDiary));
    }

    /**
     * 일지 삭제
     * DELETE /api/diaries/{diaryId}
     * @param customUserDetails 현재 로그인한 사용자
     * @param diaryId 삭제할 일지 ID
     * @return 응답 없음 (No Content)
     */
    @Operation(summary = "일지 삭제", description = "특정 ID에 해당하는 일지를 삭제합니다. 연결된 이미지 파일도 함께 삭제됩니다.")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long diaryId) {
        if (customUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        diaryService.deleteDiary(diaryId, customUserDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}