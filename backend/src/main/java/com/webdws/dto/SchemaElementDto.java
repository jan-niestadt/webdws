package com.webdws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * SchemaElementDto - DTO for XML Schema Element Information
 * 
 * This DTO represents a single element in an XML schema and includes:
 * - Element name and namespace information
 * - Element type and occurrence constraints
 * - Child elements and attributes
 * - Schema validation rules
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaElementDto {
    private String name;
    private String namespace;
    private String type;
    private String baseType;
    private int minOccurs;
    private String maxOccurs;
    private List<SchemaElementDto> children;
    private List<SchemaAttributeDto> attributes;
    private Map<String, Object> properties;
    
    public SchemaElementDto() {}
    
    public SchemaElementDto(String name, String type) {
        this.name = name;
        this.type = type;
        this.minOccurs = 1;
        this.maxOccurs = "1";
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
    
    public String getBaseType() {
        return baseType;
    }
    
    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }
    
    public int getMinOccurs() {
        return minOccurs;
    }
    
    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }
    
    public String getMaxOccurs() {
        return maxOccurs;
    }
    
    public void setMaxOccurs(String maxOccurs) {
        this.maxOccurs = maxOccurs;
    }
    
    public boolean isRequired() {
        return minOccurs > 0;
    }
    
    public List<SchemaElementDto> getChildren() {
        return children;
    }
    
    public void setChildren(List<SchemaElementDto> children) {
        this.children = children;
    }
    
    public List<SchemaAttributeDto> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(List<SchemaAttributeDto> attributes) {
        this.attributes = attributes;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
