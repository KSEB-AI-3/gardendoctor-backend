// src/main/java/com/project/farming/global/exception/AccessDeniedException.java
package com.project.farming.global.exception;

/**
 * 권한이 없을 때 (403) 발생시키는 예외
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
