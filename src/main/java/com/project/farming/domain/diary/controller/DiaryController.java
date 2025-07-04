package com.project.farming.domain.diary.controller;

import com.project.farming.domain.diary.dto.DiaryRequestDto;
import com.project.farming.domain.diary.dto.DiaryResponseDto;
import com.project.farming.domain.diary.service.DiaryService;
import com.project.farming.global.jwtToken.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 일지 작성
     */
    @PostMapping
    public ResponseEntity<Long> createDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DiaryRequestDto request) {
        Long diaryId = diaryService.createDiary(userDetails.getId(), request);
        return ResponseEntity.ok(diaryId);
    }

    /**
     * 일지 수정
     */
    @PutMapping("/{diaryId}")
    public ResponseEntity<Void> updateDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId,
            @Valid @RequestBody DiaryRequestDto request) {
        diaryService.updateDiary(userDetails.getId(), diaryId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * 일지 삭제
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId) {
        diaryService.deleteDiary(userDetails.getId(), diaryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 일지 단건 조회
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponseDto> getDiary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long diaryId) {
        DiaryResponseDto diary = diaryService.getDiary(userDetails.getId(), diaryId);
        return ResponseEntity.ok(diary);
    }

    /**
     * 일지 목록 조회 (plantIds는 optional filter)
     */
    @GetMapping
    public ResponseEntity<List<DiaryResponseDto>> getDiaryList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) List<Long> plantIds) {
        List<DiaryResponseDto> diaries = diaryService.getDiaryList(userDetails.getId(), plantIds);
        return ResponseEntity.ok(diaries);
    }
}
