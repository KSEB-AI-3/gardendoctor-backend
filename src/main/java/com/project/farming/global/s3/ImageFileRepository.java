package com.project.farming.global.s3;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {
    Optional<ImageFile> findById(Long imageFileId);

    List<ImageFile> findByDomainTypeAndDomainId(ImageDomainType domainType, Long domainId);
}
