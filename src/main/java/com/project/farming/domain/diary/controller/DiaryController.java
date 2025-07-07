package com.project.farming.domain.diary.controller;

import com.project.farming.domain.diary.dto.DiaryRequest;
import com.project.farming.domain.diary.dto.DiaryResponse;
import com.project.farming.domain.diary.entity.Diary;
import com.project.farming.domain.diary.service.DiaryService;
import com.project.farming.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Diary API", description = "일지(Diary) 관련 API") // Controller 레벨 태그
@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@SecurityRequirement(name = "jwtAuth") // 이 컨트롤러의 모든 API에 jwtAuth 보안 스키마 적용
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 새로운 일지 생성
     * POST /api/diaries
     * @param user 현재 로그인한 사용자 (인증 정보에서 추출)
     * @param request 일지 생성 요청 DTO
     * @return 생성된 일지 응답 DTO
     */
    @Operation(summary = "새 일지 생성", description = "새로운 일지를 생성하고, 하나 또는 여러 개의 사용자 식물과 연결합니다.")
    @PostMapping
    public ResponseEntity<DiaryResponse> createDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal User user, // Swagger UI에서 파라미터 숨김
            @Valid @RequestBody DiaryRequest request) {
        // 실제 프로젝트에서는 Spring Security 설정을 통해 null 체크 없이 유효한 User 객체를 보장할 수 있습니다.
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Diary diary = diaryService.createDiary(
                user,
                request.getTitle(),
                request.getContent(),
                request.getImageUrl(),
                request.getWatered(),
                request.getPruned(),
                request.getFertilized(),
                request.getSelectedUserPlantIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new DiaryResponse(diary));
    }

    /**
     * 특정 일지 조회
     * GET /api/diaries/{diaryId}
     * @param user 현재 로그인한 사용자
     * @param diaryId 조회할 일지 ID
     * @return 일지 응답 DTO
     */
    @Operation(summary = "특정 일지 조회", description = "특정 ID에 해당하는 일지의 상세 정보를 조회합니다.")
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PathVariable Long diaryId) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Diary diary = diaryService.getDiaryById(diaryId, user);
        return ResponseEntity.ok(new DiaryResponse(diary));
    }

    /**
     * 특정 사용자의 모든 일지 조회 (캘린더 기본 뷰 - 최신순)
     * GET /api/diaries/my-diaries
     * @param user 현재 로그인한 사용자
     * @return 일지 목록 응답 DTO
     */
    @Operation(summary = "특정 사용자의 모든 일지 조회 (최신순)", description = "현재 로그인된 사용자가 작성한 모든 일지 목록을 최신순으로 조회합니다.")
    @GetMapping("/my-diaries")
    public ResponseEntity<List<DiaryResponse>> getAllMyDiaries(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Diary> diaries = diaryService.getAllDiariesByUser(user);
        List<DiaryResponse> responses = diaries.stream()
                .map(DiaryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 사용자의 특정 기간 동안의 일지 조회 (캘린더 날짜별 정렬)
     * GET /api/diaries/my-diaries/by-date
     * @param user 현재 로그인한 사용자
     * @param startDate 시작 날짜/시간 (ISO 8601 형식)
     * @param endDate 종료 날짜/시간 (ISO 8601 형식)
     * @return 일지 목록 응답 DTO
     */
    @Operation(summary = "특정 기간 동안의 일지 조회", description = "현재 로그인된 사용자가 작성한 일지 중 특정 기간 내의 일지 목록을 조회합니다. 날짜별 캘린더 조회에 사용됩니다.")
    @GetMapping("/my-diaries/by-date")
    public ResponseEntity<List<DiaryResponse>> getMyDiariesByDateRange(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "검색 시작 날짜/시간 (ISO 8601 형식, 예: 2024-07-01T00:00:00)") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "검색 종료 날짜/시간 (ISO 8601 형식, 예: 2024-07-31T23:59:59)") @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Diary> diaries = diaryService.getDiariesByUserAndDateRange(user, startDate, endDate);
        List<DiaryResponse> responses = diaries.stream()
                .map(DiaryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 사용자 식물(UserPlant)에 연결된 일지 조회 (닉네임 기반 태그 검색)
     * GET /api/diaries/my-diaries/by-user-plant/{userPlantId}
     * @param user 현재 로그인한 사용자
     * @param userPlantId 검색할 UserPlant ID
     * @return 해당 UserPlant에 연결된 일지 목록 응답 DTO
     */
    @Operation(summary = "특정 사용자 식물(UserPlant)에 연결된 일지 조회", description = "특정 UserPlant에 연결된 현재 사용자의 모든 일지를 조회합니다. 클라이언트 드롭다운에서 특정 닉네임의 작물을 선택하여 일지를 필터링할 때 사용됩니다.")
    @GetMapping("/my-diaries/by-user-plant/{userPlantId}")
    public ResponseEntity<List<DiaryResponse>> getMyDiariesByUserPlant(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "검색할 사용자 식물 ID") @PathVariable Long userPlantId) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Diary> diaries = diaryService.getDiariesByUserAndUserPlant(user, userPlantId);
        List<DiaryResponse> responses = diaries.stream()
                .map(DiaryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * 여러 사용자 식물(UserPlant) 중 하나라도 연결된 일지 조회 (다중 태그 검색)
     * GET /api/diaries/my-diaries/by-user-plants?ids=1,2,3
     * @param user 현재 로그인한 사용자
     * @param userPlantIds 검색할 UserPlant ID 목록 (콤마로 구분)
     * @return 해당 UserPlant 중 하나라도 연결된 일지 목록 응사 DTO
     */
    @Operation(summary = "여러 사용자 식물(UserPlant) 중 하나라도 연결된 일지 조회", description = "현재 사용자가 등록한 여러 UserPlant 중 하나라도 연결된 일지 목록을 조회합니다. 다중 태그 검색과 유사합니다.")
    @GetMapping("/my-diaries/by-user-plants")
    public ResponseEntity<List<DiaryResponse>> getMyDiariesByUserPlants(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "콤마로 구분된 사용자 식물 ID 목록 (예: 101,103)") @RequestParam List<Long> userPlantIds) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Diary> diaries = diaryService.getDiariesByUserAndUserPlants(user, userPlantIds);
        List<DiaryResponse> responses = diaries.stream()
                .map(DiaryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }


    /**
     * 일지 수정
     * PUT /api/diaries/{diaryId}
     * @param user 현재 로그인한 사용자
     * @param diaryId 수정할 일지 ID
     * @param request 일지 수정 요청 DTO
     * @return 수정된 일지 응답 DTO
     */
    @Operation(summary = "일지 수정", description = "특정 ID에 해당하는 일지의 정보를 수정합니다. 연결된 사용자 식물 목록도 새로 지정할 수 있습니다.")
    @PutMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> updateDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Diary updatedDiary = diaryService.updateDiary(
                diaryId,
                user,
                request.getTitle(),
                request.getContent(),
                request.getImageUrl(),
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
     * @param user 현재 로그인한 사용자
     * @param diaryId 삭제할 일지 ID
     * @return 응답 없음 (No Content)
     */
    @Operation(summary = "일지 삭제", description = "특정 ID에 해당하는 일지를 삭제합니다.")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @PathVariable Long diaryId) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        diaryService.deleteDiary(diaryId, user);
        return ResponseEntity.noContent().build();
    }
}