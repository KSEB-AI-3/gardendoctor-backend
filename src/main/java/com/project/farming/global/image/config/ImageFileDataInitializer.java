package com.project.farming.global.image.config;

import com.project.farming.global.image.entity.ImageDomainType;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.repository.ImageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 사진을 저장하는 부분이 있는 도메인의 기본 이미지 저장
 */

@Order(1)
@Slf4j
@RequiredArgsConstructor
@Component
public class ImageFileDataInitializer implements CommandLineRunner {

    private final ImageFileRepository imageFileRepository;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        if (imageFileRepository.count() == 0) {
            InputStream inputStream = getClass().getResourceAsStream("/data/imageFileList.xlsx");
            if (inputStream == null) {
                throw new IllegalArgumentException("imageFileList 엑셀 파일을 찾을 수 없습니다.");
            }

            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            List<ImageFile> imageFileList = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String imageName = getCellValue(row.getCell(0));
                String imageUrl = getCellValue(row.getCell(1));
                String stringDomainType = getCellValue(row.getCell(2));
                ImageDomainType domainType = ImageDomainType.from(stringDomainType);

                imageFileList.add(ImageFile.builder()
                        .originalImageName(imageName)
                        .s3Key(imageName)
                        .imageUrl(imageUrl)
                        .domainType(domainType)
                        .domainId(0L)
                        .build());
            }
            imageFileRepository.saveAll(imageFileList);
            log.info("images 테이블에 {}개의 이미지 데이터가 저장되었습니다.",  imageFileList.size());

            workbook.close();
            inputStream.close();
        }
        else {
            log.info("images 테이블에 이미지 데이터가 이미 존재합니다.");
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        return "";
    }
}
