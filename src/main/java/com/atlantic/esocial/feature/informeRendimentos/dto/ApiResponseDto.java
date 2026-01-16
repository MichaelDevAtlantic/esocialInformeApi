package com.atlantic.esocial.feature.informeRendimentos.dto;

import java.io.Serializable;

public class ApiResponseDto<T> implements Serializable {
    private String status; // "ok" ou "error"
    private String message;
    private T data;

    public ApiResponseDto() {}

    public ApiResponseDto(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponseDto<T> ok(T data) {
        return new ApiResponseDto<>("ok", "", data);
    }

    public static <T> ApiResponseDto<T> ok(String message, T data) {
        return new ApiResponseDto<>("ok", message, data);
    }

    public static <T> ApiResponseDto<T> error(String message) {
        return new ApiResponseDto<>("error", message, null);
    }

    // getters / setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
