package com.example.fireview.global.exception;

import com.example.fireview.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 Content-Type: {}", e.getContentType());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Content-Type을 'application/json'으로 설정해주세요."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문을 읽을 수 없습니다: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("요청 본문이 올바르지 않습니다. Content-Type을 application/json으로 설정하고 JSON 형식을 확인해주세요."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("데이터 무결성 오류: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED));
    }

    // ── 클라이언트 잘못을 500 으로 덮지 않는다 ─────────────────────────────────
    // 아래가 없으면 맨 끝의 Exception 핸들러가 전부 받아 500 으로 바꾼다. 실제로
    // 없는 경로·틀린 메서드·타입이 안 맞는 경로변수가 모두 500 으로 나갔고, 프론트는
    // 서버 장애로 오해했다("서버에 생기기 전에는 404 가 온다"던 기대와 달랐다).

    /**
     * 없는 경로. 운영에서는 매핑이 없으면 Spring 이 정적 리소스로 찾다가
     * NoResourceFoundException 을 던지고, 리소스 핸들러가 없는 환경에서는
     * NoHandlerFoundException 이 난다. 둘 다 같은 404 로 맞춘다.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNoResource(Exception e) {
        log.debug("없는 경로: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /** 경로는 있는데 메서드가 틀림. 허용 메서드를 Allow 헤더로 알려준다 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.debug("지원하지 않는 메서드: {}", e.getMethod());
        HttpHeaders headers = new HttpHeaders();
        if (e.getSupportedHttpMethods() != null) {
            headers.setAllow(e.getSupportedHttpMethods());
        }
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .headers(headers)
                .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    /** 경로변수·쿼리 타입 불일치. 예: /api/products/abc (Long 자리에 문자) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.debug("타입 불일치: {}={}", e.getName(), e.getValue());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(String.format("'%s' 값이 올바르지 않습니다.", e.getName())));
    }

    /** 필수 쿼리 파라미터 누락 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(String.format("'%s' 파라미터가 필요합니다.", e.getParameterName())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 위에서 따로 다루지 않은 Spring MVC 예외는 스스로 상태 코드를 알고 있다
        // (ErrorResponse). 그 값을 그대로 쓴다. 이게 없으면 새 종류의 클라이언트 오류가
        // 생길 때마다 다시 500 이 된다. ErrorResponse 는 인터페이스라 핸들러 키로 못 써서
        // 여기서 가른다.
        if (e instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            log.debug("요청 오류 {}: {}", er.getStatusCode(), e.getMessage());
            String detail = er.getBody().getDetail();
            return ResponseEntity.status(er.getStatusCode())
                    .body(ApiResponse.error(detail != null ? detail : ErrorCode.INVALID_INPUT.getMessage()));
        }
        log.error("처리되지 않은 예외 발생: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}