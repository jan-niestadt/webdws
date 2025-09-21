package com.webdws.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * XmlDocumentDto - Data Transfer Object for XML Documents
 * 
 * This DTO represents an XML document in API requests and responses and includes:
 * - Document metadata (id, name, timestamps)
 * - XML content with validation constraints
 * - JSON serialization configuration for date formatting
 * - Validation annotations for data integrity
 * - Conversion methods between entity and DTO representations
 */
public class XmlDocumentDto {
    private String id;
    
    @NotBlank(message = "Document name is required")
    @Size(max = 255, message = "Document name must not exceed 255 characters")
    private String name;
    
    @NotBlank(message = "Document content is required")
    private String content;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // Constructors
    public XmlDocumentDto() {}
    
    public XmlDocumentDto(String id, String name, String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
