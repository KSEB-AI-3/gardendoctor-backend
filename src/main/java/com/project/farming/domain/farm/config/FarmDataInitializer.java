package com.project.farming.domain.farm.config;

import com.project.farming.domain.farm.entity.Farm;
import com.project.farming.domain.farm.repository.FarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 1. 사용자 입력 옵션 1개
 * 2. 경기데이터드림: 경기도 내 지자체, 개인, 민간단체가 분양하고 있는 텃밭 정보
 * - 텃밭 172개
 * 3. 농림축산식품 공공데이터 포털: 지자체, 개인, 민간단체가 분양 중인 텃밭 정보
 * - 텃밭 452개
 * (중복 데이터는 2번 데이터로 사용)
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class FarmDataInitializer implements CommandLineRunner {

    private final FarmRepository farmRepository;

    private static final String DEFAULT_STRING = "N/A";
    private static final String DEFAULT_IMAGE = "추가 예정";
    private static final double DEFAULT_LAT_LNG = 0.0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public void run(String... args) throws Exception {
        if (farmRepository.count() > 0) {
            log.info("farm_info 테이블에 농장 데이터가 이미 존재합니다.");
            return;
        }
        initializeFarms();
    }

    @Transactional
    private void initializeFarms() throws IOException {
        saveOtherFarmOption();
        List<Farm> farmList = loadAndMergeFarmLists();
        farmRepository.saveAll(farmList);
        log.info("farm_info 테이블에 {}개의 농장 데이터가 저장되었습니다.",  farmList.size());
    }

    private void saveOtherFarmOption() {
        Farm otherFarmOption = Farm.builder()
                .gardenUniqueId(1)
                .operator("N/A")
                .name("기타(Other)")
                .roadNameAddress("N/A")
                .lotNumberAddress("N/A")
                .facilities("N/A")
                .available(true)
                .contact("N/A")
                .latitude(0.0)
                .longitude(0.0)
                .imageUrl("추가 예정")
                .build();
        farmRepository.save(otherFarmOption);
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
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            farmList.add(createFarmFromRow(row, columnIndexMap));
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

    private Farm createFarmFromRow(Row row, Map<String, Integer> columnIndexMap) {
        Integer gardenUniqueId = parseIntegerCell(row, columnIndexMap.get("gardenUniqueId")); // 텃밭 고유번호
        String operator = getOrDefault(row, columnIndexMap.get("operator"), DEFAULT_STRING); // 운영주체
        String name = getOrDefault(row, columnIndexMap.get("name"), DEFAULT_STRING);
        String roadNameAddress = getOrDefault(row, columnIndexMap.get("roadNameAddress"), DEFAULT_STRING); // 도로명주소
        String lotNumberAddress = getOrDefault(row, columnIndexMap.get("lotNumberAddress"), DEFAULT_STRING); // 지번주소
        String facilities =  getOrDefault(row, columnIndexMap.get("facilities"), DEFAULT_STRING);
        String contact = getOrDefault(row, columnIndexMap.get("contact"), DEFAULT_STRING); // 신청방법
        Double latitude = parseDoubleOrDefault(row, columnIndexMap.get("latitude"), DEFAULT_LAT_LNG); // 위도
        Double longitude = parseDoubleOrDefault(row, columnIndexMap.get("longitude"), DEFAULT_LAT_LNG); // 경도
        LocalDate createdAt = parseDate(row, columnIndexMap.get("createdAt"));
        LocalDate updatedAt = parseDate(row, columnIndexMap.get("updatedAt"));
        String imageUrl = getOrDefault(row, columnIndexMap.get("imageUrl"), DEFAULT_IMAGE);
        return Farm.builder()
                .gardenUniqueId(gardenUniqueId)
                .operator(operator)
                .name(name)
                .roadNameAddress(roadNameAddress)
                .lotNumberAddress(lotNumberAddress)
                .facilities(facilities)
                .available(true)
                .contact(contact)
                .latitude(latitude)
                .longitude(longitude)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .imageUrl(imageUrl)
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
