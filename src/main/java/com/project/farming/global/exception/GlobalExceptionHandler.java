// src/main/java/com/project/farming/global/exception/GlobalExceptionHandler.java
package com.project.farming.global.exception;

import com.project.farming.domain.user.dto.AuthResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException; // ⭐ AuthenticationException 임포트
import org.springframework.security.core.userdetails.UsernameNotFoundException; // ⭐ UsernameNotFoundException 임포트
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // 모든 @Controller, @RestController에서 발생하는 예외를 처리
@Slf4j // Lombok을 이용한 로거 자동 생성
public class GlobalExceptionHandler {

    /**
     * @Valid 어노테이션으로 인한 유효성 검증 실패 시 발생하는 예외를 처리합니다.
     * @param ex MethodArgumentNotValidException
     * @return BAD_REQUEST (400) 응답과 유효성 검증 오류 상세 정보
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation failed: {}", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                AuthResponseDto.builder()
                        .message("유효성 검증 실패")
                        .errorCode("VALIDATION_ERROR")
                        .data(errors) // 어떤 필드에서 어떤 오류가 났는지 상세 정보 제공
                        .build()
        );
    }

    /**
     * IllegalArgumentException (예: 이미 존재하는 이메일, 유효하지 않은 토큰 등) 예외를 처리합니다.
     * @param ex IllegalArgumentException
     * @return BAD_REQUEST (400) 응답과 예외 메시지
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthResponseDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(
                AuthResponseDto.builder()
                        .message(ex.getMessage())
                        .errorCode("BAD_REQUEST")
                        .build()
        );
    }

    /**
     * Spring Security의 인증 관련 예외 (예: UsernameNotFoundException, BadCredentialsException 등)를 처리합니다.
     * @param ex AuthenticationException
     * @return UNAUTHORIZED (401) 응답과 인증 실패 메시지
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AuthResponseDto> handleAuthenticationException(AuthenticationException ex) {
        log.warn("AuthenticationException: {}", ex.getMessage());
        String errorMessage = "인증에 실패했습니다."; // 기본 메시지

        if (ex instanceof UsernameNotFoundException) {
            errorMessage = "이메일 또는 비밀번호가 잘못되었습니다."; // 사용자에게 구체적인 정보 노출 자제
        }
        // 다른 AuthenticationException 유형에 따라 메시지를 다르게 설정할 수 있습니다.
        // 예: if (ex instanceof BadCredentialsException) { errorMessage = "비밀번호가 일치하지 않습니다."; }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AuthResponseDto.builder()
                        .message(errorMessage)
                        .errorCode("AUTHENTICATION_FAILED")
                        .build()
        );
    }

    /**
     * 그 외 모든 예상치 못한 예외를 처리합니다.
     * @param ex Exception
     * @return INTERNAL_SERVER_ERROR (500) 응답과 일반적인 서버 오류 메시지
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponseDto> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthResponseDto.builder()
                        .message("서버 오류가 발생했습니다.")
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .build()
        );
    }
}
