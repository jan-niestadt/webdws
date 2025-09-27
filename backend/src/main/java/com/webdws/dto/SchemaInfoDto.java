package com.webdws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * SchemaInfoDto - DTO for XML Schema Information
 * 
 * This DTO represents the complete information about an XML schema and includes:
 * - Schema metadata (target namespace, version, etc.)
 * - Root elements and their structure
 * - Schema validation information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaInfoDto {
    private String targetNamespace;
    private String elementFormDefault;
    private String attributeFormDefault;
    private List<SchemaElementDto> elements;
    private String schemaLocation;
    private String version;
    
    public SchemaInfoDto() {}
    
    public SchemaInfoDto(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }
    
    // Getters and Setters
    public String getTargetNamespace() {
        return targetNamespace;
    }
    
    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }
    
    public String getElementFormDefault() {
        return elementFormDefault;
    }
    
    public void setElementFormDefault(String elementFormDefault) {
        this.elementFormDefault = elementFormDefault;
    }
    
    public String getAttributeFormDefault() {
        return attributeFormDefault;
    }
    
    public void setAttributeFormDefault(String attributeFormDefault) {
        this.attributeFormDefault = attributeFormDefault;
    }
    
    public List<SchemaElementDto> getElements() {
        return elements;
    }
    
    public void setElements(List<SchemaElementDto> elements) {
        this.elements = elements;
    }
    
    public String getSchemaLocation() {
        return schemaLocation;
    }
    
    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
}
