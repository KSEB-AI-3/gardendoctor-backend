package com.project.farming.domain.plant.config;

import com.project.farming.domain.plant.entity.Plant;
import com.project.farming.domain.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * AI로 질병 진단이 가능한 주요 시설 원예 작물 정보
 * - 작물 12종
 * - 사용자 입력 옵션 포함 (총 13개 정보)
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class PlantDataInitializer implements CommandLineRunner {

    private final PlantRepository plantRepository;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        if (plantRepository.count() == 0) {
            InputStream inputStream = getClass().getResourceAsStream("/data/plantList.xlsx");
            if (inputStream == null) {
                throw new IllegalArgumentException("plantList 엑셀 파일을 찾을 수 없습니다.");
            }

            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            List<Plant> plantList = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                String name = getCellValue(row.getCell(0));
                String englishName = getCellValue(row.getCell(1));
                String species = getCellValue(row.getCell(2));
                String season = getCellValue(row.getCell(3));
                String imageUrl = getCellValue(row.getCell(4));

                plantList.add(Plant.builder()
                        .name(name)
                        .englishName(englishName)
                        .species(species)
                        .season(season)
                        .imageUrl(imageUrl)
                        .createdAt(LocalDate.now())
                        .updatedAt(LocalDate.now())
                        .build());
            }
            plantRepository.saveAll(plantList);
            log.info("plant_info 테이블에 {}개의 식물 데이터가 저장되었습니다.",  plantList.size());

            workbook.close();
            inputStream.close();
        }
        else {
            log.info("plant_info 테이블에 식물 데이터가 이미 존재합니다.");
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        return "";
    }
}
