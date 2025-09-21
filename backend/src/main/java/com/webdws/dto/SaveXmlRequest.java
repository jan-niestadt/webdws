package com.webdws.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SaveXmlRequest - Request DTO for XML Document Creation/Update
 * 
 * This DTO represents the request payload for saving XML documents and includes:
 * - Document name with validation constraints
 * - XML content with required validation
 * - Input validation for data integrity
 * - Simple structure for frontend integration
 */
public class SaveXmlRequest {
    
    @NotBlank(message = "Document name is required")
    @Size(max = 255, message = "Document name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Document content is required")
    private String content;
    
    // Constructors
    public SaveXmlRequest() {}
    
    public SaveXmlRequest(String name, String content) {
        this.name = name;
        this.content = content;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}
