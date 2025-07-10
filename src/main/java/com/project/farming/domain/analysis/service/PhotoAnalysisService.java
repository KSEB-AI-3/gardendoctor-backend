// src/main/java/com/project/farming/domain/analysis/service/PhotoAnalysisService.java

package com.project.farming.domain.analysis.service;

import com.project.farming.domain.analysis.dto.AnalysisRequest;
import com.project.farming.domain.analysis.dto.AnalysisResult;
import com.project.farming.domain.analysis.dto.PhotoAnalysisSidebarResponseDto;
import com.project.farming.domain.analysis.entity.PhotoAnalysis;
import com.project.farming.domain.analysis.repository.PhotoAnalysisRepository;
import com.project.farming.domain.user.entity.User;
import com.project.farming.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoAnalysisService {

    private final PhotoAnalysisRepository photoAnalysisRepository;
    private final UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();


    private final String AI_SERVER_URL = "http://localhost:8000/analyze"; // FastAPI AI 서버 URL


    public List<PhotoAnalysisSidebarResponseDto> getSidebarAnalysisList(Long userId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return photoAnalysisRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(pa -> PhotoAnalysisSidebarResponseDto.builder()
                        .photoAnalysisId(pa.getPhotoAnalysisId())
                        .createdDate(pa.getCreatedAt().format(formatter))
                        .detectedDisease(pa.getDetectedDisease()) // AI가 넘겨준 값 그대로
                        .build())
                .collect(Collectors.toList());
    }

    // 사진 URL 받고 AI 서버 호출 + DB 저장
    public PhotoAnalysis analyzePhotoAndSave(Long userId, String imageUrl) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. userId=" + userId));

        // 2. AI 서버 호출용 DTO
        AnalysisRequest request = new AnalysisRequest(imageUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AnalysisRequest> entity = new HttpEntity<>(request, headers);

        // 3. AI 서버 호출
        ResponseEntity<AnalysisResult> response = restTemplate.postForEntity(AI_SERVER_URL, entity, AnalysisResult.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("AI 서버 분석 실패");
        }

        AnalysisResult result = response.getBody();

        // 4. PhotoAnalysis 엔티티 생성 및 저장
        PhotoAnalysis photoAnalysis = PhotoAnalysis.builder()
                .user(user)
                .imageUrl(imageUrl)
                .analysisSummary(result.getAnalysisSummary())
                .detectedDisease(result.getDetectedDisease())
                .solution(result.getSolution())
                .build();

        return photoAnalysisRepository.save(photoAnalysis);
    }
}
