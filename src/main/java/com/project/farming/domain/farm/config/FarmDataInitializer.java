package com.project.farming.domain.farm.config;

import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.farm.repository.FarmRepository;
import com.project.farming.domain.farm.service.FarmService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 텃밭 정보 총 453개
 * - 초기 데이터이므로 추후 수정 가능
 * 1. 사용자 입력 옵션 1개
 * 2. 경기데이터드림: 경기도 내 지자체, 개인, 민간단체가 분양하고 있는 텃밭 정보
 * - 텃밭 172개
 * 3. 농림축산식품 공공데이터 포털: 지자체, 개인, 민간단체가 분양 중인 텃밭 정보
 * - 텃밭 452개
 * (중복 데이터는 2번 데이터로 사용)
 */

@Order(2)
@Slf4j
@RequiredArgsConstructor
@Component
public class FarmDataInitializer implements CommandLineRunner {

    private final FarmRepository farmRepository;
    private final FarmService farmService;
    private final ImageFileRepository imageFileRepository;

    private static final String DEFAULT_STRING = "N/A";
    private static final double DEFAULT_LAT_LNG = 0.0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void run(String... args) throws Exception {
        if (farmRepository.count() > 0) {
            log.info("farm_info 테이블에 초기 텃밭 데이터가 이미 존재합니다.");
            return;
        }
        initializeFarms();
    }

    private void initializeFarms() throws IOException {
        farmService.saveOtherFarmOption();
        List<Farm> farmList = loadAndMergeFarmLists();
        farmService.saveFarms(farmList);
        log.info("farm_info 테이블에 {}개의 초기 텃밭 데이터가 저장되었습니다.",  farmList.size() + 1);
    }

    private List<Farm> loadAndMergeFarmLists() throws IOException {
        List<Farm> gyeonggiFarmList = loadFarmData("/data/farmList.xlsx");
        List<Farm> mafraFarmList = loadFarmData("/data/farmList2.xls");
        Map<Integer, Farm> farmMap = new LinkedHashMap<>();

        mafraFarmList.forEach(f -> farmMap.put(f.getGardenUniqueId(), f));
        gyeonggiFarmList.forEach(f -> farmMap.put(f.getGardenUniqueId(), f));
        return new ArrayList<>(farmMap.values());
    }

    private List<Farm> loadFarmData(String fileName) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException(fileName + " 엑셀 파일을 찾을 수 없습니다.");
        }

        Workbook workbook = WorkbookFactory.create(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Map<String, Integer> columnIndexMap = getColumnIndexMap(sheet);
        List<Farm> farmList = new ArrayList<>();
        ImageFile defaultImageFile = imageFileRepository.findByS3Key(DefaultImages.DEFAULT_FARM_IMAGE)
                .orElseThrow(() -> new ImageFileNotFoundException("기본 텃밭 이미지가 존재하지 않습니다."));

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // 제목 행
            farmList.add(createFarmFromRow(row, columnIndexMap, defaultImageFile));
        }
        return farmList;
    }

    private Map<String, Integer> getColumnIndexMap(Sheet sheet) {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        Row headerRow = sheet.getRow(0);
        for (Cell cell : headerRow) {
            columnIndexMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
        }
        return columnIndexMap;
    }

    private Farm createFarmFromRow(Row row, Map<String, Integer> columnIndexMap, ImageFile defaultImageFile) {
        Integer gardenUniqueId = parseIntegerCell(row, columnIndexMap.get("gardenUniqueId")); // 텃밭 고유번호
        String operator = getOrDefault(row, columnIndexMap.get("operator"), DEFAULT_STRING); // 운영주체
        String farmName = getOrDefault(row, columnIndexMap.get("farmName"), DEFAULT_STRING);
        String roadNameAddress = getOrDefault(row, columnIndexMap.get("roadNameAddress"), DEFAULT_STRING); // 도로명주소
        String lotNumberAddress = getOrDefault(row, columnIndexMap.get("lotNumberAddress"), DEFAULT_STRING); // 지번주소
        String facilities =  getOrDefault(row, columnIndexMap.get("facilities"), DEFAULT_STRING); // 부대시설
        String contact = getOrDefault(row, columnIndexMap.get("contact"), DEFAULT_STRING); // 신청방법
        Double latitude = parseDoubleOrDefault(row, columnIndexMap.get("latitude"), DEFAULT_LAT_LNG); // 위도
        Double longitude = parseDoubleOrDefault(row, columnIndexMap.get("longitude"), DEFAULT_LAT_LNG); // 경도
        LocalDate createdAt = parseDate(row, columnIndexMap.get("createdAt"));
        LocalDate updatedAt = parseDate(row, columnIndexMap.get("updatedAt"));

        return Farm.builder()
                .gardenUniqueId(gardenUniqueId)
                .operator(operator)
                .farmName(farmName)
                .roadNameAddress(roadNameAddress)
                .lotNumberAddress(lotNumberAddress)
                .facilities(facilities)
                .contact(contact)
                .latitude(latitude)
                .longitude(longitude)
                .available(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .farmImageFile(defaultImageFile)
                .build();
    }

    private int parseIntegerCell(Row row, Integer colIndex) {
        return (int) Double.parseDouble(getCellValue(row.getCell(colIndex)));
    }

    private double parseDoubleOrDefault(Row row, Integer colIndex, double defaultValue) {
        String val = getCellValue(row.getCell(colIndex));
        return val.isBlank() ? defaultValue : Double.parseDouble(val);
    }

    private LocalDate parseDate(Row row, Integer colIndex) {
        String val = getCellValue(row.getCell(colIndex));
        if (val.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(val, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {} -> 기본값(현재 날짜) 사용", e.getMessage());
            log.info(e.getMessage());
            return LocalDate.now();
        }
    }

    private String getOrDefault(Row row, Integer colIndex, String defaultVal) {
        String val = getCellValue(row.getCell(colIndex));
        return val.isBlank() || val.equals("-") ? defaultVal : val;
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
