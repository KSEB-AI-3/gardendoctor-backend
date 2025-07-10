package com.project.farming.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private String analysisSummary;
    private String detectedDisease;
    private String solution;
}
