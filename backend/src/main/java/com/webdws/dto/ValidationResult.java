package com.webdws.dto;

public class ValidationResult {
    private boolean valid;
    private String error;
    
    public ValidationResult() {}
    
    public ValidationResult(boolean valid, String error) {
        this.valid = valid;
        this.error = error;
    }
    
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }
    
    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, error);
    }
    
    // Getters and Setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
}
