package com.webdws.controller;

import com.webdws.dto.*;
import com.webdws.service.XmlDocumentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/xml")
@CrossOrigin(origins = "*")
public class XmlDocumentController {
    
    @Autowired
    private XmlDocumentService xmlDocumentService;
    
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<XmlDocumentListDto>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            XmlDocumentListDto documents = xmlDocumentService.getAllDocuments(page, size);
            return ResponseEntity.ok(ApiResponse.success(documents));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch documents: " + e.getMessage()));
        }
    }
    
    @GetMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<XmlDocumentDto>> getDocument(@PathVariable Long id) {
        try {
            XmlDocumentDto document = xmlDocumentService.getDocumentById(id);
            return ResponseEntity.ok(ApiResponse.success(document));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch document: " + e.getMessage()));
        }
    }
    
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<XmlDocumentDto>> saveDocument(@Valid @RequestBody SaveXmlRequest request) {
        try {
            XmlDocumentDto document = xmlDocumentService.saveDocument(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(document));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to save document: " + e.getMessage()));
        }
    }
    
    @PutMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<XmlDocumentDto>> updateDocument(
            @PathVariable Long id, 
            @Valid @RequestBody SaveXmlRequest request) {
        try {
            XmlDocumentDto document = xmlDocumentService.updateDocument(id, request);
            return ResponseEntity.ok(ApiResponse.success(document));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to update document: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        try {
            xmlDocumentService.deleteDocument(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete document: " + e.getMessage()));
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<ValidationResult>> validateXml(@RequestBody SaveXmlRequest request) {
        try {
            ValidationResult result = xmlDocumentService.validateXml(request.getContent());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Validation failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("XML Editor Backend is running"));
    }
}
