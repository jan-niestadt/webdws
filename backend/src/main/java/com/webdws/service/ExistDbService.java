package com.webdws.service;

import com.webdws.config.ExistDbConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.UUID;

/**
 * ExistDbService - eXist-db Integration Service
 * 
 * This service provides integration with eXist-db XML database and handles:
 * - XML document storage and retrieval from eXist-db
 * - REST API communication with eXist-db server
 * - Authentication and connection management
 * - Collection management and document operations
 * - Error handling and response processing
 */
@Service
public class ExistDbService {
    
    @Autowired
    private ExistDbConfig config;
    
    private RestTemplate restTemplate;
    private HttpHeaders headers;
    
    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        
        // Set up basic authentication if username is provided
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            String password = config.getPassword() != null ? config.getPassword() : "";
            String auth = config.getUsername() + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);
        }
    }
    
    public String storeDocument(String name, String content) {
        try {
            // Ensure collection exists
            createCollectionIfNotExists();
            
            String documentId = UUID.randomUUID().toString();
            String url = config.getUrl() + "/exist/rest" + config.getCollection() + "/" + documentId;
            
            HttpEntity<String> request = new HttpEntity<>(content, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return documentId;
            } else {
                throw new RuntimeException("Failed to store document: " + response.getStatusCode());
            }   
        } catch (Exception e) {
            throw new RuntimeException("Failed to store document in eXist-db: " + e.getMessage(), e);
        }
    }
    
    public String getDocument(String documentId) {
        try {
            String url = config.getUrl() + "/exist/rest" + config.getCollection() + "/" + documentId;
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                throw new RuntimeException("Failed to get document: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get document from eXist-db", e);
        }
    }
    
    public void updateDocument(String documentId, String content) {
        try {
            String url = config.getUrl() + "/exist/rest" + config.getCollection() + "/" + documentId;
            
            HttpEntity<String> request = new HttpEntity<>(content, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to update document: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update document in eXist-db", e);
        }
    }
    
    public void deleteDocument(String documentId) {
        try {
            String url = config.getUrl() + "/exist/rest" + config.getCollection() + "/" + documentId;
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw new RuntimeException("Failed to delete document: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete document from eXist-db", e);
        }
    }
    
    private void createCollectionIfNotExists() {
        try {
            String url = config.getUrl() + "/exist/rest" + config.getCollection();
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                // Collection doesn't exist, try to create it by creating a temporary document
                // This will create the collection automatically
                String tempUrl = url + "/.temp";
                HttpEntity<String> tempRequest = new HttpEntity<>("<temp/>", headers);
                ResponseEntity<String> createResponse = restTemplate.exchange(tempUrl, HttpMethod.PUT, tempRequest, String.class);
                
                if (createResponse.getStatusCode().is2xxSuccessful()) {
                    // Delete the temporary document
                    restTemplate.exchange(tempUrl, HttpMethod.DELETE, request, String.class);
                } else {
                    throw new RuntimeException("Failed to create collection: " + createResponse.getStatusCode());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure collection exists: " + e.getMessage(), e);
        }
    }
}
