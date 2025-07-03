package com.project.farming.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 클라이언트에게 보낼 에러 응답의 형식 정의
 예외가 발생했을 때, 컨트롤러는 이 클래스의 인스턴스를 JSON으로 반환
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String code;
}
