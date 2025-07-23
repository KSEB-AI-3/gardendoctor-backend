package com.project.farming.global.s3;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "image_files", indexes = {
        @Index(name = "idx_covering_imagefile", columnList = "image_file_id, domain_type, domain_id, url")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ImageFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_file_id")
    private Long imageFileId;

    @Column(nullable = false, length = 512) // URL 길이를 넉넉하게
    private String url; // S3 URL 또는 외부 이미지 URL

    @Column(length = 255) // 원본 파일명 (S3 업로드 시 사용)
    private String originalFileName;

    @Column(length = 255) // S3에 저장된 파일명 (S3 Key로 사용)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ImageDomainType domainType;

    @Column(nullable = false)
    private Long domainId;

    // 업데이트 메서드들
    public void updateUrl(String url) {
        this.url = url;
    }

    public void updateOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void updateS3Key(String s3Key) {
        this.s3Key = s3Key;
    }
}
