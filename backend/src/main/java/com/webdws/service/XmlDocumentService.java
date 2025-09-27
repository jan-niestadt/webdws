package com.webdws.service;

import com.webdws.dto.*;
import com.webdws.model.XmlDocument;
import com.webdws.repository.XmlDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * XmlDocumentService - Business Logic Layer for XML Document Management
 * 
 * This service provides the core business logic for XML document operations and handles:
 * - CRUD operations for XML documents with database persistence
 * - Integration with eXist-db for advanced XML processing
 * - Document validation and content management
 * - Pagination and search functionality
 * - Transaction management and error handling
 * - DTO conversion and response formatting
 */
@Service
@Transactional
public class XmlDocumentService {
    
    @Autowired
    private XmlDocumentRepository repository;
    
    @Autowired
    private ExistDbService existDbService;
    
    public XmlDocumentListDto getAllDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<XmlDocument> documents = repository.findAll(pageable);
        
        List<XmlDocumentDto> documentDtos = documents.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        return new XmlDocumentListDto(documentDtos, documents.getTotalElements());
    }
    
    public XmlDocumentDto getDocumentById(Long id) {
        XmlDocument document = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        // Load content from eXist-db
        try {
            String content = existDbService.getDocument(document.getExistDbId());
            if (content != null) {
                document.setContent(content);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load document content from eXist-db", e);
        }
        
        return convertToDto(document);
    }
    
    public XmlDocumentDto saveDocument(SaveXmlRequest request) {
        // Validate XML content
        ValidationResult validation = validateXml(request.getContent());
        if (!validation.isValid()) {
            throw new RuntimeException("Invalid XML: " + validation.getError());
        }
        
        // Store in eXist-db
        String existDbId;
        try {
            existDbId = existDbService.storeDocument(request.getName(), request.getContent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to store document in eXist-db: " + e.getMessage(), e);
        }
        
        // Store metadata in PostgreSQL
        XmlDocument document = new XmlDocument();
        document.setName(request.getName());
        document.setContent(request.getContent());
        document.setExistDbId(existDbId);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        XmlDocument savedDocument = repository.save(document);
        return convertToDto(savedDocument);
    }
    
    public XmlDocumentDto updateDocument(Long id, SaveXmlRequest request) {
        XmlDocument document = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        // Validate XML content
        ValidationResult validation = validateXml(request.getContent());
        if (!validation.isValid()) {
            throw new RuntimeException("Invalid XML: " + validation.getError());
        }
        
        // Update in eXist-db
        try {
            existDbService.updateDocument(document.getExistDbId(), request.getContent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update document in eXist-db", e);
        }
        
        // Update metadata in PostgreSQL
        document.setName(request.getName());
        document.setContent(request.getContent());
        document.setUpdatedAt(LocalDateTime.now());
        
        XmlDocument updatedDocument = repository.save(document);
        return convertToDto(updatedDocument);
    }
    
    public void deleteDocument(Long id) {
        XmlDocument document = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        
        // Delete from eXist-db (if existDbId exists)
        if (document.getExistDbId() != null && !document.getExistDbId().trim().isEmpty()) {
            try {
                existDbService.deleteDocument(document.getExistDbId());
            } catch (Exception e) {
                // Log the error but continue with PostgreSQL deletion
                System.err.println("Warning: Failed to delete document from eXist-db: " + e.getMessage());
                // Don't throw the exception - continue with PostgreSQL cleanup
            }
        }
        
        // Delete from PostgreSQL
        repository.delete(document);
    }
    
    public ValidationResult validateXml(String content) {
        try {
            // Basic XML validation - check for well-formedness
            if (content == null || content.trim().isEmpty()) {
                return ValidationResult.invalid("XML content is empty");
            }
            
            // Check for basic XML structure
            String trimmed = content.trim();
            if (!trimmed.startsWith("<")) {
                return ValidationResult.invalid("XML must start with a tag");
            }
            
            // Try to parse with a simple XML parser
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            
            // Create a simple error handler
            builder.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
                    // Ignore warnings
                }
                
                @Override
                public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
                    throw e;
                }
                
                @Override
                public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
                    throw e;
                }
            });
            
            // Parse the XML
            java.io.StringReader reader = new java.io.StringReader(content);
            org.xml.sax.InputSource inputSource = new org.xml.sax.InputSource(reader);
            builder.parse(inputSource);
            
            return ValidationResult.valid();
            
        } catch (Exception e) {
            return ValidationResult.invalid("Invalid XML: " + e.getMessage());
        }
    }
    
    private XmlDocumentDto convertToDto(XmlDocument document) {
        return new XmlDocumentDto(
            document.getId().toString(),
            document.getName(),
            document.getContent(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }
}
