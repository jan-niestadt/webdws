package com.webdws.controller;

import com.webdws.dto.ApiResponse;
import com.webdws.dto.SchemaInfoDto;
import com.webdws.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SchemaController - REST API Controller for XML Schema Operations
 * 
 * This controller provides REST endpoints for XML schema management and handles:
 * - Loading and parsing XML schema files
 * - Converting schemas to JSON tree structures
 * - Schema information retrieval
 * - Error handling and HTTP status management
 * - CORS configuration for frontend integration
 */
@RestController
@RequestMapping("/api/schema")
@CrossOrigin(origins = "*")
public class SchemaController {
    
    @Autowired
    private SchemaService schemaService;
    
    /**
     * Load the default XML schema (library.xsd) and return as JSON tree
     */
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<SchemaInfoDto>> getDefaultSchema() {
        System.out.println("DEBUG: SchemaController.getDefaultSchema() called");
        try {
            SchemaInfoDto schema = schemaService.loadDefaultSchema();
            System.out.println("DEBUG: Successfully loaded schema, returning response");
            return ResponseEntity.ok(ApiResponse.success(schema));
        } catch (Exception e) {
            System.err.println("DEBUG: Error in getDefaultSchema(): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to load default schema: " + e.getMessage()));
        }
    }
    
    /**
     * Load a specific XML schema by path and return as JSON tree
     */
    @GetMapping("/load")
    public ResponseEntity<ApiResponse<SchemaInfoDto>> loadSchema(
            @RequestParam String path) {
        try {
            SchemaInfoDto schema = schemaService.loadSchema(path);
            return ResponseEntity.ok(ApiResponse.success(schema));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to load schema: " + e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint for schema service
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Schema service is running"));
    }
}
