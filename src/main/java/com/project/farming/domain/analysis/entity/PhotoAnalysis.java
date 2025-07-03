package com.project.farming.domain.analysis.entity;

import com.project.farming.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "photo_analysis", indexes = @Index(name = "idx_user_photo_analysis", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PhotoAnalysis {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String analysisResult;

    private String detectedDisease;

    @Column(columnDefinition = "TEXT")
    private String solutionSummary;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateAnalysisResult(String analysisResult, String detectedDisease, String solutionSummary) {
        this.analysisResult = analysisResult;
        this.detectedDisease = detectedDisease;
        this.solutionSummary = solutionSummary;
    }
}
