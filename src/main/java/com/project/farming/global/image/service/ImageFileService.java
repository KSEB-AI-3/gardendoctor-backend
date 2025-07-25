package com.project.farming.global.image.service;

import com.project.farming.global.exception.ImageFileNotFoundException;
import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.DefaultImages;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.repository.ImageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageFileService {

    private final S3Service s3Service;
    private final ImageFileRepository imageFileRepository;

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

        // S3에 저장될 고유한 파일명 생성 (UUID 사용)
        String s3Key = getS3Key(originalFilename, domainType, domainId);

        // S3에 파일 업로드
        String s3Url = s3Service.uploadFile(multipartFile, s3Key);

        // ImageFile 엔티티 생성 및 저장
        ImageFile imageFile = ImageFile.builder()
                .originalImageName(originalFilename)
                .s3Key(s3Key)
                .imageUrl(s3Url)
                .domainType(domainType)
                .domainId(domainId)
                .build();

        return imageFileRepository.save(imageFile);
    }

    /**
     * S3에 업로드된 이미지를 수정하고, ImageFile 엔티티를 수정하여 DB에 저장합니다.
     *
     * @param imageFileId   수정할 ImageFile의 ID
     * @param newFile       업로드할 새로운 이미지 파일
     * @param domainType    이미지가 속할 도메인 유형 (예: ImageDomainType.PLANT)
     * @param domainId      이미지가 속할 도메인 엔티티의 ID
     * @return 수정된 ImageFile 엔티티
     */
    @Transactional
    public ImageFile updateImage(
            Long imageFileId,
            MultipartFile newFile, ImageDomainType domainType, Long domainId) {

        ImageFile oldImageFile = imageFileRepository.findById(imageFileId)
                .orElseThrow(() -> new ImageFileNotFoundException("존재하지 않는 이미지 파일입니다: " + imageFileId));
        String oldImageS3Key = oldImageFile.getS3Key();

        // 기본 이미지 수정 방지(S3, imageFile DB)
        if (DefaultImages.isDefaultImage(oldImageS3Key)) {
            log.info("기본 이미지는 수정할 수 없습니다: {}", imageFileId);
            return uploadImage(newFile, domainType, domainId);
        }

        String newOriginalImageName = newFile.getOriginalFilename();
        String newS3Key = getS3Key(newOriginalImageName, domainType, domainId);
        String newImageUrl = s3Service.updateFile(oldImageS3Key, newFile, newS3Key);

        oldImageFile.updateOriginalImageName(newOriginalImageName);
        oldImageFile.updateS3Key(newS3Key);
        oldImageFile.updateImageUrl(newImageUrl);
        return imageFileRepository.save(oldImageFile);
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
                .orElseThrow(() -> new ImageFileNotFoundException("존재하지 않는 이미지 파일입니다: " + imageFileId));

        // 기본 이미지 삭제 방지(S3, imageFile DB)
        if (DefaultImages.isDefaultImage(imageFile.getS3Key())) {
            log.info("기본 이미지는 삭제할 수 없습니다: {}", imageFileId);
            return;
        }

        // S3 Key가 존재하는 경우에만 S3에서 객체 삭제 시도
        if (imageFile.getS3Key() != null && !imageFile.getS3Key().isEmpty()) {
            s3Service.deleteFile(imageFile.getS3Key());
            // S3 삭제 실패해도 DB 레코드는 삭제
        } else {
            log.info("외부 URL 이미지이거나 S3 키가 없어 S3에서 파일을 삭제하지 않습니다. ImageFile ID: {}", imageFileId);
        }

        // DB에서 ImageFile 레코드 삭제
        imageFileRepository.delete(imageFile);
        log.info("DB에서 ImageFile 레코드 삭제: imageFileId={}", imageFileId);
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
                .imageUrl(imageUrl)
                .originalImageName("external_oauth_image") // 외부 이미지임을 명시
                .s3Key(null) // S3에 저장된 파일이 아니므로 s3Key는 null
                .domainType(domainType)
                .domainId(domainId)
                .build();
        return imageFileRepository.save(imageFile);
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

    private String getS3Key(String originalFilename, ImageDomainType domainType, Long domainId) {
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // 기본 구조를 사용하려면: UUID.randomUUID().toString() + fileExtension;
        return domainType.name().toLowerCase() + "/" + domainId + "/" + UUID.randomUUID() + fileExtension;
    }
}
