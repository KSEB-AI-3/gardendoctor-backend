package com.project.farming.domain.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FarmAdminRequest {

    @NotNull(message = "텃밭 고유번호를 입력해주세요.")
    private Integer gardenUniqueId; // 텃밭 고유번호

    private String operator; // 운영주체
    private String farmName;
    private String roadNameAddress; // 도로명주소

    @NotBlank(message = "텃밭 주소를 입력해주세요.")
    private String lotNumberAddress; // 지번주소

    private String facilities; // 부대시설
    private String contact; // 신청방법
    private Double latitude; // 위도
    private Double longitude; // 경도
    private Boolean available;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
