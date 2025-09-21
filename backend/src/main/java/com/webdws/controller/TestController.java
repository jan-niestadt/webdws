package com.webdws.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Test Controller for serving static resources
 * 
 * This controller provides endpoints for testing static resource access
 * and serves schema files directly from the classpath
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/schema/{filename}")
    public ResponseEntity<Resource> getSchema(@PathVariable String filename) {
        try {
            Resource resource = new ClassPathResource("static/schema/" + filename);
            
            if (resource.exists()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_XML);
                
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
