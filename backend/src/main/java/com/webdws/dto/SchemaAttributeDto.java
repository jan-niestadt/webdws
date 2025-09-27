package com.webdws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SchemaAttributeDto - DTO for XML Schema Attribute Information
 * 
 * This DTO represents a single attribute in an XML schema and includes:
 * - Attribute name and namespace information
 * - Attribute type and constraints
 * - Usage requirements (required/optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaAttributeDto {
    private String name;
    private String namespace;
    private String type;
    private String use;
    private String defaultValue;
    private String fixedValue;
    
    public SchemaAttributeDto() {}
    
    public SchemaAttributeDto(String name, String type) {
        this.name = name;
        this.type = type;
        this.use = "optional";
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUse() {
        return use;
    }
    
    public void setUse(String use) {
        this.use = use;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public String getFixedValue() {
        return fixedValue;
    }
    
    public void setFixedValue(String fixedValue) {
        this.fixedValue = fixedValue;
    }
    
    public boolean isRequired() {
        return "required".equals(use);
    }
}
