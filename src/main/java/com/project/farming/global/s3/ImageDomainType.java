// com.project.farming.global.s3.ImageDomainType.java
package com.project.farming.global.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageDomainType {
    // 각 도메인에 대한 설명을 추가할 수 있습니다.
    USER("사용자 프로필"),
    PLANT("식물 정보"),
    DIARY("일지"),
    // TODO: 다른 도메인이 추가될 때 여기에 정의
    // BOARD("게시글"),
    // COMMENT("댓글"),
    // PRODUCT("상품")
    ;

    private final String description;
}