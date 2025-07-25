// src/main/java/com/project/farming/global/exception/GlobalExceptionHandler.java
package com.project.farming.global.exception;

import com.project.farming.domain.user.dto.AuthResponseDto; // AuthResponseDto 임포트
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Spring Security의 UsernameNotFoundException
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // 모든 @Controller, @RestController에서 발생하는 예외를 처리
@Slf4j
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
     * IllegalArgumentException (예: 유효하지 않은 인자 값) 예외를 처리합니다.
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
     * 커스텀 예외: NotificationNotFoundException (알림을 찾을 수 없을 때) 처리
     * @param ex NotificationNotFoundException
     * @return NOT_FOUND (404) 응답과 예외 메시지
     */
    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<AuthResponseDto> handleNotificationNotFoundException(NotificationNotFoundException ex) {
        log.warn("NotificationNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AuthResponseDto.builder()
                        .message(ex.getMessage())
                        .errorCode("NOTIFICATION_NOT_FOUND")
                        .build()
        );
    }

    /**
     * 커스텀 예외: UserNotFoundException (사용자를 찾을 수 없을 때) 처리
     * @param ex UserNotFoundException
     * @return NOT_FOUND (404) 응답과 예외 메시지
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<AuthResponseDto> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("UserNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AuthResponseDto.builder()
                        .message(ex.getMessage())
                        .errorCode("USER_NOT_FOUND")
                        .build()
        );
    }

    /**
     * 커스텀 예외: AccessDeniedException (권한이 없을 때) 처리
     * @param ex AccessDeniedException
     * @return FORBIDDEN (403) 응답과 예외 메시지
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AuthResponseDto> handleCustomAccessDeniedException(AccessDeniedException ex) {
        log.warn("CustomAccessDeniedException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                AuthResponseDto.builder()
                        .message(ex.getMessage())
                        .errorCode("ACCESS_DENIED")
                        .build()
        );
    }

    /**
     * 커스텀 예외: CustomException (일반적인 비즈니스 로직 오류) 처리
     * @param ex CustomException
     * @return BAD_REQUEST (400) 응답과 예외 메시지
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<AuthResponseDto> handleCustomException(CustomException ex) {
        log.warn("CustomException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                AuthResponseDto.builder()
                        .message(ex.getMessage())
                        .errorCode("CUSTOM_ERROR")
                        .build()
        );
    }

    /**
     * 커스텀 예외: AiAnalysisException (AI 분석 실패 등 서버 내부 오류) 처리
     * @param ex AiAnalysisException
     * @return INTERNAL_SERVER_ERROR (500) 응답과 예외 메시지
     */
    @ExceptionHandler(AiAnalysisException.class)
    public ResponseEntity<AuthResponseDto> handleAiAnalysisException(AiAnalysisException ex) {
        log.error("AiAnalysisException: {}", ex.getMessage(), ex); // AI 분석 오류는 상세 로깅
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthResponseDto.builder()
                        .message("AI 분석 중 오류가 발생했습니다: " + ex.getMessage())
                        .errorCode("AI_ANALYSIS_ERROR")
                        .build()
        );
    }

    /**
     * 커스텀 예외: UsernameException (사용자 이름 관련 유효성 또는 충돌 오류) 처리
     * @param ex UsernameException
     * @return BAD_REQUEST (400) 응답과 예외 메시지
     */
    @ExceptionHandler(UsernameException.class)
    public ResponseEntity<AuthResponseDto> handleUsernameException(UsernameException ex) {
        log.warn("UsernameException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                AuthResponseDto.builder()
                        .message(ex.getMessage())
                        .errorCode("USERNAME_ERROR")
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
     * 그 외 모든 예상치 못한 예외를 처리합니다. (가장 일반적인 예외 핸들러)
     * 이 핸들러는 위에 정의된 특정 예외 핸들러들이 처리하지 못한 모든 RuntimeException을 포함합니다.
     * @param ex Exception
     * @return INTERNAL_SERVER_ERROR (500) 응답과 일반적인 서버 오류 메시지
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponseDto> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex); // 스택 트레이스 포함 상세 로깅
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthResponseDto.builder()
                        .message("서버 오류가 발생했습니다.")
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .build()
        );
    }
}