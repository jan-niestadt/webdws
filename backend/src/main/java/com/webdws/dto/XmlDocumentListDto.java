package com.webdws.dto;

import java.util.List;

/**
 * XmlDocumentListDto - DTO for Paginated XML Document Lists
 * 
 * This DTO represents a paginated list of XML documents and includes:
 * - List of XML document DTOs
 * - Total count for pagination metadata
 * - Support for paginated API responses
 * - Simple structure for frontend integration
 */
public class XmlDocumentListDto {
    private List<XmlDocumentDto> documents;
    private long total;
    
    public XmlDocumentListDto() {}
    
    public XmlDocumentListDto(List<XmlDocumentDto> documents, long total) {
        this.documents = documents;
        this.total = total;
    }
    
    // Getters and Setters
    public List<XmlDocumentDto> getDocuments() {
        return documents;
    }
    
    public void setDocuments(List<XmlDocumentDto> documents) {
        this.documents = documents;
    }
    
    public long getTotal() {
        return total;
    }
    
    public void setTotal(long total) {
        this.total = total;
    }
}
