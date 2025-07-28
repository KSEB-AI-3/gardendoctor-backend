package com.project.farming.domain.plant.config;

import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.repository.PlantRepository;
import com.project.farming.domain.plant.service.PlantService;
import com.project.farming.global.exception.ImageFileNotFoundException;
import com.project.farming.global.image.entity.DefaultImages;
import com.project.farming.global.image.entity.ImageFile;
import com.project.farming.global.image.repository.ImageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 식물 정보 총 13개
 * - AI로 질병 진단이 가능한 주요 시설 원예 작물 정보 저장
 * - 초기 데이터이므로 추후 수정 가능
 * - 작물 12종
 * - 사용자 입력 옵션 1개
 */

@Order(3)
@Slf4j
@RequiredArgsConstructor
@Component
public class PlantDataInitializer implements CommandLineRunner {

    private final PlantRepository plantRepository;
    private final PlantService plantService;
    private final ImageFileRepository imageFileRepository;

    @Override
    public void run(String... args) throws Exception {
        if (plantRepository.count() > 0) {
            log.info("plant_info 테이블에 초기 식물 데이터가 이미 존재합니다.");
            return;
        }
        initializePlants(getDefaultPlantImage());
    }

    private void initializePlants(ImageFile defaultPlantImage) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/data/plantList.xlsx");
        if (inputStream == null) {
            throw new IllegalArgumentException("plantList 엑셀 파일을 찾을 수 없습니다.");
        }

        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        List<Plant> plantList = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // 제목 행
            String plantName = getCellValue(row.getCell(0));
            String plantEnglishName = getCellValue(row.getCell(1));
            String species = getCellValue(row.getCell(2));
            String season = getCellValue(row.getCell(3));

            plantList.add(Plant.builder()
                    .plantName(plantName)
                    .plantEnglishName(plantEnglishName)
                    .species(species)
                    .season(season)
                    .plantImageFile(defaultPlantImage)
                    .build());
        }
        plantService.savePlants(plantList);
        log.info("plant_info 테이블에 {}개의 초기 식물 데이터가 저장되었습니다.",  plantList.size());

        workbook.close();
        inputStream.close();
    }

    private ImageFile getDefaultPlantImage() {
        return imageFileRepository.findByS3Key(DefaultImages.DEFAULT_PLANT_IMAGE)
                .orElseThrow(() -> new ImageFileNotFoundException("기본 식물 이미지가 존재하지 않습니다."));
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        return "";
    }
}
