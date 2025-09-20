package com.webdws.dto;

import java.util.List;

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
