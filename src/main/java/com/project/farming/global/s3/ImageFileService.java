package com.project.farming.global.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.project.farming.global.exception.ImageUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageFileService {

    private final AmazonS3 amazonS3;
    private final ImageFileRepository imageFileRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * S3에 이미지를 업로드하고, ImageFile 엔티티를 생성하여 DB에 저장합니다.
     *
     * @param multipartFile 업로드할 이미지 파일
     * @param domainType    이미지가 속할 도메인 유형 (예: ImageDomainType.USER)
     * @param domainId      이미지가 속할 도메인 엔티티의 ID
     * @return 저장된 ImageFile 엔티티
     */
    @Transactional
    public ImageFile uploadImage(MultipartFile multipartFile, ImageDomainType domainType, Long domainId) {
        if (multipartFile.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // S3에 저장될 고유한 파일명 생성 (UUID 사용)
        String s3Key = UUID.randomUUID().toString() + fileExtension;
        // 폴더 구조를 사용하려면: String s3Key = domainType.name().toLowerCase() + "/" + domainId + "/" + UUID.randomUUID().toString() + fileExtension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        try {
            // S3에 파일 업로드
            amazonS3.putObject(new PutObjectRequest(bucketName, s3Key, multipartFile.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead)); // Public Read 권한 부여 (필요에 따라 Private으로 변경 후 Pre-signed URL 사용)

            // S3 URL 생성
            String s3Url = amazonS3.getUrl(bucketName, s3Key).toString();

            // ImageFile 엔티티 생성 및 저장
            ImageFile imageFile = ImageFile.builder()
                    .url(s3Url)
                    .originalFileName(originalFilename) // ⭐ 추가: 원본 파일명 저장
                    .s3Key(s3Key) // ⭐ 추가: S3 Key 저장
                    .domainType(domainType)
                    .domainId(domainId)
                    .build();

            return imageFileRepository.save(imageFile);

        } catch (IOException e) {
            log.error("S3 파일 업로드 중 오류 발생: {}", e.getMessage());
            throw new ImageUploadException("이미지 업로드에 실패했습니다.", e);
        }
    }

    /**
     * 외부 URL (예: OAuth 프로필 이미지)을 받아 ImageFile 엔티티를 생성하여 DB에 저장합니다.
     * 이 메서드는 S3에 실제로 파일을 업로드하지 않습니다.
     *
     * @param imageUrl   외부 이미지 URL
     * @param domainType 이미지가 속할 도메인 유형
     * @param domainId   이미지가 속할 도메인 엔티티의 ID
     * @return 저장된 ImageFile 엔티티
     */
    @Transactional
    public ImageFile createExternalImageFile(String imageUrl, ImageDomainType domainType, Long domainId) {
        // 외부 URL이므로 originalFileName과 s3Key는 null 또는 임의의 값으로 설정
        ImageFile imageFile = ImageFile.builder()
                .url(imageUrl)
                .originalFileName("external_oauth_image") // 외부 이미지임을 명시
                .s3Key(null) // S3에 저장된 파일이 아니므로 s3Key는 null
                .domainType(domainType)
                .domainId(domainId)
                .build();
        return imageFileRepository.save(imageFile);
    }

    /**
     * ImageFile ID를 기반으로 이미지를 삭제합니다.
     * ImageFile에 S3 Key가 있는 경우 S3에서 객체를 삭제하고, DB에서도 ImageFile 레코드를 삭제합니다.
     *
     * @param imageFileId 삭제할 ImageFile의 ID
     */
    @Transactional
    public void deleteImage(Long imageFileId) {
        ImageFile imageFile = imageFileRepository.findById(imageFileId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이미지 파일입니다."));

        // S3 Key가 존재하는 경우에만 S3에서 객체 삭제 시도
        if (imageFile.getS3Key() != null && !imageFile.getS3Key().isEmpty()) {
            try {
                amazonS3.deleteObject(bucketName, imageFile.getS3Key());
                log.info("S3에서 이미지 객체 삭제: {}", imageFile.getS3Key());
            } catch (Exception e) {
                log.warn("S3 파일 삭제 실패 (파일이 없거나 권한 문제 등): {}. DB에서만 제거합니다. 오류: {}", imageFile.getS3Key(), e.getMessage());
                // S3 삭제 실패해도 DB 레코드는 삭제
            }
        } else {
            log.info("외부 URL 이미지이거나 S3 키가 없어 S3에서 파일을 삭제하지 않습니다. ImageFile ID: {}", imageFileId);
        }

        // DB에서 ImageFile 레코드 삭제
        imageFileRepository.delete(imageFile);
        log.info("DB에서 ImageFile 레코드 삭제: imageFileId={}", imageFileId);
    }

    // ImageFile ID로 이미지 정보를 조회하는 메서드
    public Optional<ImageFile> getImageFileById(Long imageFileId) {
        return imageFileRepository.findById(imageFileId);
    }

    /**
     * 특정 도메인에 속하는 모든 이미지 파일을 조회합니다.
     * (예: 특정 유저가 업로드한 모든 이미지 조회 등)
     */
    public List<ImageFile> getImagesByDomainAndId(ImageDomainType domainType, Long domainId) {
        return imageFileRepository.findByDomainTypeAndDomainId(domainType, domainId);
    }
}
