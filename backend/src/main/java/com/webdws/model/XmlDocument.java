package com.webdws.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * XmlDocument Entity - JPA Entity for XML Document Storage
 * 
 * This entity represents an XML document in the database and provides:
 * - Primary key and metadata fields (id, name, timestamps)
 * - XML content storage with TEXT column type for large documents
 * - Integration with eXist-db for advanced XML processing
 * - Validation constraints for data integrity
 * - Automatic timestamp management for audit trails
 */
@Entity
@Table(name = "xml_documents")
public class XmlDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Document name is required")
    @Size(max = 255, message = "Document name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;
    
    @NotBlank(message = "Document content is required")
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "exist_db_id")
    private String existDbId;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public XmlDocument() {}
    
    public XmlDocument(String name, String content) {
        this.name = name;
        this.content = content;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
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
    
    public String getExistDbId() {
        return existDbId;
    }
    
    public void setExistDbId(String existDbId) {
        this.existDbId = existDbId;
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
