package com.example.fireview.global.response;

import com.example.fireview.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", null, null);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getMessage(), null, errorCode.name());
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }
}