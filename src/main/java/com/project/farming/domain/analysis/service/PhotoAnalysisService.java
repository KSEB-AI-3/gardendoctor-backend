package com.project.farming.domain.analysis.service;

import com.project.farming.domain.analysis.dto.AnalysisRequest;
import com.project.farming.domain.analysis.dto.AnalysisResult;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoAnalysisService {

    private final PhotoAnalysisRepository photoAnalysisRepository;
    private final UserRepository userRepository;
    private final ImageFileService imageFileService;

    @Qualifier("pythonWebClient")
    private final WebClient pythonWebClient;

    private final String AI_SERVER_ENDPOINT = "/diagnose-by-url";

    /**
     * 사진 분석 후 DB 저장
     * - 동일 유저 10초 이내 중복 요청 차단
     */
    @Transactional
    public PhotoAnalysis analyzePhotoAndSave(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("분석을 위해 사진 파일을 반드시 전송해야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. userId=" + userId));

        PhotoAnalysis lastAnalysis = photoAnalysisRepository.findTopByUserUserIdOrderByCreatedAtDesc(userId).orElse(null);
        if (lastAnalysis != null && lastAnalysis.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(10))) {
            throw new IllegalStateException("잠시 후 다시 시도해 주세요. 연속 분석 요청은 10초 후 가능합니다.");
        }

        // 1) 이미지 S3 업로드
        ImageFile uploadedImage = imageFileService.uploadImage(file, ImageDomainType.PHOTO, userId);

        // 2) WebClient를 통한 AI 서버 호출
        AnalysisRequest request = new AnalysisRequest(uploadedImage.getImageUrl());

        AnalysisResult result = pythonWebClient.post()
                .uri(AI_SERVER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AnalysisResult.class)
                .block();

        if (result == null) {
            throw new AiAnalysisException("AI 서버 분석에 실패하였습니다.");
        }

        // 3) 분석 결과 저장
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
