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


    private final String AI_SERVER_URL = "http://localhost:8000"; // FastAPI AI 서버 URL


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
                        .imageUrl(pa.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    // 사진 URL 받고 AI 서버 호출 + DB 저장

    public PhotoAnalysis analyzePhotoAndSave(Long userId, String cropName, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. userId=" + userId));

        // 수정 후:
        AnalysisRequest request = new AnalysisRequest(cropName, imageUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AnalysisRequest> entity = new HttpEntity<>(request, headers);

        String url = AI_SERVER_URL + "/diagnose-by-url/" + cropName;
        ResponseEntity<AnalysisResult> response = restTemplate.postForEntity(url, entity, AnalysisResult.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("AI 서버 분석 실패");
        }

        AnalysisResult result = response.getBody();

        PhotoAnalysis photoAnalysis = PhotoAnalysis.builder()
                .user(user)
                .imageUrl(imageUrl)
                .analysisSummary(result.getDisease_info().getSummary())
                .detectedDisease(result.getDisease_info().getName())
                .solution(result.getDisease_info().getSolution())
                .build();


        return photoAnalysisRepository.save(photoAnalysis);
    }
}