// src/main/java/com/project/farming/domain/analysis/controller/PhotoAnalysisController.java

package com.project.farming.domain.analysis.controller;

import com.project.farming.domain.analysis.dto.AnalysisRequest;
import com.project.farming.domain.analysis.dto.AnalysisResult;
import com.project.farming.domain.analysis.dto.PhotoAnalysisSidebarResponseDto;
import com.project.farming.domain.analysis.entity.PhotoAnalysis;
import com.project.farming.domain.analysis.service.PhotoAnalysisService;
import com.project.farming.global.jwtToken.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "PhotoAnalysis API", description = "사진 분석 API")
@RestController
@RequestMapping("/api/photo-analysis")
@RequiredArgsConstructor
public class PhotoAnalysisController {

    private final PhotoAnalysisService photoAnalysisService;

    @GetMapping("/sidebar/{userId}")
    @Operation(summary = "사이드바 분석 목록", description = "사이드바용 사진 분석 기록 날짜 + 병명 반환")
    public ResponseEntity<List<PhotoAnalysisSidebarResponseDto>> getSidebarAnalysisList(@PathVariable Long userId) {
        return ResponseEntity.ok(photoAnalysisService.getSidebarAnalysisList(userId));
    }

    @PostMapping("/analyze")
    @Operation(summary = "사진 분석 요청", description = "JWT 인증된 사용자로부터 사진 URL 받고 AI 분석 후 DB 저장")
    public ResponseEntity<PhotoAnalysisSidebarResponseDto> analyzePhoto(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AnalysisRequest request) {
        Long userId = userDetails.getUser().getUserId();
        PhotoAnalysis saved = photoAnalysisService.analyzePhotoAndSave(userId, request.getImageUrl());

        return ResponseEntity.ok(
                PhotoAnalysisSidebarResponseDto.builder()
                        .photoAnalysisId(saved.getPhotoAnalysisId())
                        .createdDate(saved.getCreatedAt().toLocalDate().toString())
                        .detectedDisease(saved.getDetectedDisease())
                        .build()
        );
    }
}
