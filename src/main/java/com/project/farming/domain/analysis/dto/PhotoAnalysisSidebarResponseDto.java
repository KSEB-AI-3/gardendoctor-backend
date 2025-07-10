// src/main/java/com/project/farming/domain/analysis/dto/PhotoAnalysisSidebarResponseDto.java

package com.project.farming.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PhotoAnalysisSidebarResponseDto {
    private Long photoAnalysisId;
    private String createdDate;       // yyyy-MM-dd 형태
    private String detectedDisease;   // AI 모델에서 리턴
}
