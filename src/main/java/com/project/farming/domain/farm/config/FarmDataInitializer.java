package com.project.farming.domain.farm.config;

import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.farm.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 경기도 내 지자체, 개인, 민간단체가 분양하고 있는 텃밭 정보
 * - 텃밭 172개
 * - 사용자 입력 옵션 포함 (총 173개 정보)
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class FarmDataInitializer implements CommandLineRunner {

    private final FarmRepository farmRepository;

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        if (farmRepository.count() == 0) {
            InputStream inputStream = getClass().getResourceAsStream("/data/farmList.xlsx");
            if (inputStream == null) {
                throw new IllegalArgumentException("farmList 엑셀 파일을 찾을 수 없습니다.");
            }

            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            List<Farm> farmList = new ArrayList<>();
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                Integer gardenUniqueId = (int) Double.parseDouble(getCellValue(row.getCell(0))); // 텃밭 고유번호
                String operator = getCellValue(row.getCell(1)); // 운영주체
                String name = getCellValue(row.getCell(2));
                String roadNameAddress = getCellValue(row.getCell(3)); // 도로명주소
                String lotNumberAddress = getCellValue(row.getCell(4)); // 지번주소

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate createdAt = LocalDate.now();
                LocalDate updatedAt = LocalDate.now();

                DecimalFormat df = new DecimalFormat("#");
                String stringCreatedAt = df.format(Double.parseDouble(getCellValue(row.getCell(5))));
                String stringUpdatedAt = df.format(Double.parseDouble(getCellValue(row.getCell(6))));
                try {
                    createdAt = LocalDate.parse(stringCreatedAt, formatter);
                    updatedAt = LocalDate.parse(stringUpdatedAt, formatter);
                } catch (Exception e) {
                    log.info("날짜 파싱 오류: {} -> 기본값(현재 날짜) 사용", stringCreatedAt);
                    log.info(e.toString());
                }

                String facilities = getCellValue(row.getCell(7));
                Boolean available = true;
                String contact = getCellValue(row.getCell(8)); // 신청방법

                Double latitude = null, longitude = null;
                String stringLatitude = getCellValue(row.getCell(9));
                String stringLongitude = getCellValue(row.getCell(10));
                if (stringLatitude.isBlank() || stringLongitude.isBlank()) {
                    latitude = 0.0;
                    longitude = 0.0;
                }
                else {
                    latitude = Double.parseDouble(stringLatitude); // 위도
                    longitude = Double.parseDouble(stringLongitude); // 경도
                }

                String imageUrl = getCellValue(row.getCell(11));

                if (name.isBlank()) name = "N/A";
                if (roadNameAddress.isBlank()) roadNameAddress = "N/A";
                if (Objects.equals(facilities, "-")) facilities = "N/A";
                if (imageUrl.isBlank()) imageUrl = "추가 예정";

                farmList.add(Farm.builder()
                        .gardenUniqueId(gardenUniqueId)
                        .operator(operator)
                        .name(name)
                        .roadNameAddress(roadNameAddress)
                        .lotNumberAddress(lotNumberAddress)
                        .facilities(facilities)
                        .available(available)
                        .contact(contact)
                        .latitude(latitude)
                        .longitude(longitude)
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .imageUrl(imageUrl)
                        .build());
            }
            farmRepository.saveAll(farmList);
            log.info("farm_info 테이블에 {}개의 농장 데이터가 저장되었습니다.",  farmList.size());

            workbook.close();
            inputStream.close();
        }
        else {
            log.info("farm_info 테이블에 농장 데이터가 이미 존재합니다.");
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }
}
