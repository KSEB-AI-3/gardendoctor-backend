// src/main/java/com/project/farming/domain/analysis/controller/PhotoAnalysisController.java

package com.project.farming.domain.analysis.controller;


import com.project.farming.domain.analysis.dto.PhotoAnalysisSidebarResponseDto;
import com.project.farming.domain.analysis.entity.PhotoAnalysis;
import com.project.farming.domain.analysis.service.PhotoAnalysisService;
import com.project.farming.global.jwtToken.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
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

    @SecurityRequirement(name = "jwtAuth")
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사진 분석 요청", description = "JWT 인증된 사용자로부터 이미지 파일을 받아 AI 분석 후 DB에 저장합니다.")
    public ResponseEntity<PhotoAnalysisSidebarResponseDto> analyzePhoto(

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "업로드할 이미지 파일 (분석 대상)", required = true)
            @RequestPart("CropName") String cropName,
            @RequestPart("file") MultipartFile file) {

        Long userId = userDetails.getUser().getUserId();

        PhotoAnalysis saved = photoAnalysisService.analyzePhotoAndSave(userId, cropName, file);

        PhotoAnalysisSidebarResponseDto response = PhotoAnalysisSidebarResponseDto.builder()
                .photoAnalysisId(saved.getPhotoAnalysisId())
                .createdDate(saved.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .detectedDisease(saved.getDetectedDisease())
                .analysisSummary(saved.getAnalysisSummary())
                .solution(saved.getSolution())
                .imageUrl(saved.getPhotoImageFile().getImageUrl())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}