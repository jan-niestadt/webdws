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
            System.out.println("ExistDbService.storeDocument called for: " + name);
            System.out.println("  eXist-db URL: " + config.getUrl());
            System.out.println("  Collection: " + config.getCollection());
            
            // Check if eXist-db is available
            if (!isExistDbAvailable()) {
                System.out.println("eXist-db is not available, skipping document storage");
                return "local-" + UUID.randomUUID().toString();
            }
            
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
            System.err.println("Failed to store document in eXist-db: " + e.getMessage());
            // Return a local ID instead of failing completely
            return "local-" + UUID.randomUUID().toString();
        }
    }
    
    public String getDocument(String documentId) {
        try {
            if (!isExistDbAvailable()) {
                System.out.println("eXist-db is not available, cannot retrieve document: " + documentId);
                return null;
            }
            
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
            System.err.println("Failed to get document from eXist-db: " + e.getMessage());
            return null;
        }
    }
    
    public void updateDocument(String documentId, String content) {
        try {
            if (!isExistDbAvailable()) {
                System.out.println("eXist-db is not available, skipping document update: " + documentId);
                return;
            }
            
            String url = config.getUrl() + "/exist/rest" + config.getCollection() + "/" + documentId;
            
            HttpEntity<String> request = new HttpEntity<>(content, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to update document: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Failed to update document in eXist-db: " + e.getMessage());
        }
    }
    
    public void deleteDocument(String documentId) {
        try {
            if (!isExistDbAvailable()) {
                System.out.println("eXist-db is not available, skipping document deletion: " + documentId);
                return;
            }
            
            String url = config.getUrl() + "/exist/rest" + config.getCollection() + "/" + documentId;
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful() && response.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw new RuntimeException("Failed to delete document: " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Handle specific HTTP errors
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // Document not found in eXist-db - this is OK, just log it
                System.out.println("Document " + documentId + " not found in eXist-db, continuing with deletion");
                return;
            }
            System.err.println("Failed to delete document from eXist-db: " + e.getStatusCode());
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Handle connection issues
            System.err.println("Failed to connect to eXist-db: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Failed to delete document from eXist-db: " + e.getMessage());
        }
    }
    
    private boolean isExistDbAvailable() {
        try {
            String url = config.getUrl() + "/exist/rest/db";
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.out.println("eXist-db is not available: " + e.getMessage());
            return false;
        }
    }
    
    private void createCollectionIfNotExists() {
        try {
            String collectionPath = config.getCollection();
            String url = config.getUrl() + "/exist/rest" + collectionPath;
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                // Collection doesn't exist, create it by creating a temporary document
                System.out.println("Collection " + collectionPath + " not found, creating it...");
                
                // Create collection by putting a temporary document in it
                // This will automatically create the collection path
                String tempUrl = url + "/.temp";
                HttpEntity<String> tempRequest = new HttpEntity<>("<temp/>", headers);
                ResponseEntity<String> createResponse = restTemplate.exchange(tempUrl, HttpMethod.PUT, tempRequest, String.class);
                
                if (createResponse.getStatusCode().is2xxSuccessful()) {
                    // Delete the temporary document
                    restTemplate.exchange(tempUrl, HttpMethod.DELETE, request, String.class);
                    System.out.println("Collection " + collectionPath + " created successfully");
                } else {
                    throw new RuntimeException("Failed to create collection: " + createResponse.getStatusCode() + " - " + createResponse.getBody());
                }
            } else if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Collection " + collectionPath + " already exists");
            } else {
                throw new RuntimeException("Unexpected response when checking collection: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // Collection doesn't exist, try to create it
                System.out.println("Collection " + config.getCollection() + " not found, creating it...");
                try {
                    String tempUrl = config.getUrl() + "/exist/rest" + config.getCollection() + "/.temp";
                    HttpEntity<String> tempRequest = new HttpEntity<>("<temp/>", headers);
                    ResponseEntity<String> createResponse = restTemplate.exchange(tempUrl, HttpMethod.PUT, tempRequest, String.class);
                    
                    if (createResponse.getStatusCode().is2xxSuccessful()) {
                        // Delete the temporary document
                        restTemplate.exchange(tempUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
                        System.out.println("Collection " + config.getCollection() + " created successfully");
                    } else {
                        throw new RuntimeException("Failed to create collection: " + createResponse.getStatusCode() + " - " + createResponse.getBody());
                    }
                } catch (Exception createException) {
                    throw new RuntimeException("Failed to create collection: " + createException.getMessage(), createException);
                }
            } else {
                throw new RuntimeException("Failed to check collection: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure collection exists: " + e.getMessage(), e);
        }
    }
}
