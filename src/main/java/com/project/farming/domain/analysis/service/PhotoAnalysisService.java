package com.project.farming.domain.analysis.service;

import com.project.farming.domain.analysis.dto.AnalysisRequest;
import com.project.farming.domain.analysis.dto.AnalysisResult;
import com.project.farming.domain.analysis.dto.PhotoAnalysisSidebarResponseDto;
import com.project.farming.domain.analysis.entity.PhotoAnalysis;
import com.project.farming.domain.analysis.repository.PhotoAnalysisRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import com.project.farming.global.exception.AiAnalysisException;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.service.ImageFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoAnalysisService {

    private final PhotoAnalysisRepository photoAnalysisRepository;
    private final UserRepository userRepository;
    private final ImageFileService imageFileService;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String AI_SERVER_URL = "http://localhost:8000/diagnose-by-url";

    public List<PhotoAnalysisSidebarResponseDto> getSidebarAnalysisList(Long userId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return photoAnalysisRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(pa -> PhotoAnalysisSidebarResponseDto.builder()
                        .photoAnalysisId(pa.getPhotoAnalysisId())
                        .createdDate(pa.getCreatedAt().format(formatter))
                        .detectedDisease(pa.getDetectedDisease())
                        .analysisSummary(pa.getAnalysisSummary())
                        .solution(pa.getSolution())
                        .imageUrl(pa.getPhotoImageFile().getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 사진 분석 후 DB 저장
     * - cropName, 사진 둘 다 반드시 받아야 함
     * - 동일 유저 10초 이내 중복 요청 차단
     */
    @Transactional
    public PhotoAnalysis analyzePhotoAndSave(Long userId, String cropName, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("분석을 위해 사진 파일을 반드시 전송해야 합니다.");
        }
        if (cropName == null || cropName.trim().isEmpty()) {
            throw new IllegalArgumentException("분석을 위해 cropName을 반드시 전송해야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. userId=" + userId));

        PhotoAnalysis photoAnalysis = photoAnalysisRepository.findTopByUserUserIdOrderByCreatedAtDesc(userId).orElse(null);
        if (photoAnalysis != null && photoAnalysis.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(10))) {
            throw new IllegalStateException("잠시 후 다시 시도해 주세요. 연속 분석 요청은 10초 후 가능합니다.");
        }

        // 1) S3 업로드
        ImageFile uploadedImage = imageFileService.uploadImage(file, ImageDomainType.PHOTO, userId);

        // 2) AI 서버 분석 요청
        AnalysisRequest request = new AnalysisRequest(cropName, uploadedImage.getImageUrl());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AnalysisRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<AnalysisResult> response = restTemplate.postForEntity(
                AI_SERVER_URL, entity, AnalysisResult.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new AiAnalysisException("AI 서버 분석에 실패하였습니다.");
        }

        AnalysisResult result = response.getBody();

        // 3) PhotoAnalysis 저장
        PhotoAnalysis saved = PhotoAnalysis.builder()
                .user(user)
                .photoImageFile(uploadedImage)
                .analysisSummary(result.getDisease_info() != null ? result.getDisease_info().getSummary() : "정보 없음")
                .detectedDisease(result.getDisease_info() != null ? result.getDisease_info().getName() : "분석 실패")
                .solution(result.getDisease_info() != null ? result.getDisease_info().getSolution() : "정보 없음")
                .build();

        return photoAnalysisRepository.save(saved);
    }
}
