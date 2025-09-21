package com.webdws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ApiResponse - Generic API Response Wrapper
 * 
 * This DTO provides a standardized response format for all API endpoints and includes:
 * - Success/failure status indication
 * - Generic data payload for successful responses
 * - Error message for failed responses
 * - JSON serialization configuration
 * - Static factory methods for common response patterns
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    
    public ApiResponse() {}
    
    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }
    
    public ApiResponse(boolean success, String error) {
        this.success = success;
        this.error = error;
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }
    
    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, error);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
