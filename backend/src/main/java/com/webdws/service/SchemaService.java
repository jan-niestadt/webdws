package com.webdws.service;

import com.webdws.dto.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SchemaService - Service for XML Schema Processing
 * 
 * This service handles:
 * - Loading XML schema files from resources
 * - Parsing XSD files into structured data
 * - Converting schema information to JSON-friendly DTOs
 * - Basic schema element and attribute extraction
 */
@Service
public class SchemaService {
    
    private static final String SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String DEFAULT_SCHEMA_PATH = "schema/library.xsd";
    
    /**
     * Load and parse the default XML schema
     */
    public SchemaInfoDto loadDefaultSchema() throws Exception {
        return loadSchema(DEFAULT_SCHEMA_PATH);
    }
    
    /**
     * Load and parse an XML schema from the classpath
     */
    public SchemaInfoDto loadSchema(String schemaPath) throws Exception {
        try (InputStream inputStream = new ClassPathResource(schemaPath).getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            
            return parseSchemaDocument(document);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new Exception("Failed to load schema: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse a schema document into structured data
     */
    private SchemaInfoDto parseSchemaDocument(Document document) {
        Element rootElement = document.getDocumentElement();
        SchemaInfoDto schemaInfo = new SchemaInfoDto();
        
        // Extract schema metadata
        schemaInfo.setTargetNamespace(rootElement.getAttribute("targetNamespace"));
        schemaInfo.setElementFormDefault(rootElement.getAttribute("elementFormDefault"));
        schemaInfo.setAttributeFormDefault(rootElement.getAttribute("attributeFormDefault"));
        
        // Find all element declarations
        List<SchemaElementDto> elements = new ArrayList<>();
        NodeList elementNodes = rootElement.getElementsByTagNameNS(SCHEMA_NAMESPACE, "element");
        
        for (int i = 0; i < elementNodes.getLength(); i++) {
            Element elementNode = (Element) elementNodes.item(i);
            SchemaElementDto element = parseElement(elementNode);
            if (element != null) {
                elements.add(element);
            }
        }
        
        schemaInfo.setElements(elements);
        return schemaInfo;
    }
    
    /**
     * Parse a single element declaration
     */
    private SchemaElementDto parseElement(Element elementNode) {
        String name = elementNode.getAttribute("name");
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        SchemaElementDto element = new SchemaElementDto(name, "element");
        
        // Parse occurrence constraints
        String minOccurs = elementNode.getAttribute("minOccurs");
        if (!minOccurs.isEmpty()) {
            element.setMinOccurs(Integer.parseInt(minOccurs));
        }
        
        String maxOccurs = elementNode.getAttribute("maxOccurs");
        if (!maxOccurs.isEmpty()) {
            element.setMaxOccurs(maxOccurs);
        }
        
        // Parse type information
        String type = elementNode.getAttribute("type");
        if (!type.isEmpty()) {
            element.setType(type);
        }
        
        // Parse default and fixed values
        String defaultValue = elementNode.getAttribute("default");
        if (!defaultValue.isEmpty()) {
            element.setDefaultValue(defaultValue);
        }
        
        String fixedValue = elementNode.getAttribute("fixed");
        if (!fixedValue.isEmpty()) {
            element.setFixedValue(fixedValue);
        }
        
        // Parse complex type if present
        Element complexType = getFirstChildElement(elementNode, "complexType");
        if (complexType != null) {
            parseComplexType(complexType, element);
        }
        
        return element;
    }
    
    /**
     * Parse a complex type definition
     */
    private void parseComplexType(Element complexTypeNode, SchemaElementDto element) {
        // Parse sequence elements
        Element sequence = getFirstChildElement(complexTypeNode, "sequence");
        if (sequence != null) {
            List<SchemaElementDto> children = parseSequence(sequence);
            element.setChildren(children);
        }
        
        // Parse attributes
        List<SchemaAttributeDto> attributes = new ArrayList<>();
        NodeList attributeNodes = complexTypeNode.getElementsByTagNameNS(SCHEMA_NAMESPACE, "attribute");
        
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Element attrNode = (Element) attributeNodes.item(i);
            SchemaAttributeDto attribute = parseAttribute(attrNode);
            if (attribute != null) {
                attributes.add(attribute);
            }
        }
        
        if (!attributes.isEmpty()) {
            element.setAttributes(attributes);
        }
    }
    
    /**
     * Parse a sequence of elements
     */
    private List<SchemaElementDto> parseSequence(Element sequenceNode) {
        List<SchemaElementDto> children = new ArrayList<>();
        NodeList childNodes = sequenceNode.getChildNodes();
        
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                if (childElement.getNamespaceURI() != null && 
                    childElement.getNamespaceURI().equals(SCHEMA_NAMESPACE) &&
                    childElement.getLocalName().equals("element")) {
                    
                    SchemaElementDto child = parseElement(childElement);
                    if (child != null) {
                        children.add(child);
                    }
                }
            }
        }
        
        return children;
    }
    
    /**
     * Parse a single attribute declaration
     */
    private SchemaAttributeDto parseAttribute(Element attributeNode) {
        String name = attributeNode.getAttribute("name");
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        SchemaAttributeDto attribute = new SchemaAttributeDto(name, "attribute");
        
        // Parse type
        String type = attributeNode.getAttribute("type");
        if (!type.isEmpty()) {
            attribute.setType(type);
        }
        
        // Parse usage
        String use = attributeNode.getAttribute("use");
        if (!use.isEmpty()) {
            attribute.setUse(use);
        }
        
        // Parse default value
        String defaultValue = attributeNode.getAttribute("default");
        if (!defaultValue.isEmpty()) {
            attribute.setDefaultValue(defaultValue);
        }
        
        // Parse fixed value
        String fixedValue = attributeNode.getAttribute("fixed");
        if (!fixedValue.isEmpty()) {
            attribute.setFixedValue(fixedValue);
        }
        
        return attribute;
    }
    
    /**
     * Helper method to get the first child element with a specific name
     */
    private Element getFirstChildElement(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                if (childElement.getNamespaceURI() != null && 
                    childElement.getNamespaceURI().equals(SCHEMA_NAMESPACE) &&
                    childElement.getLocalName().equals(localName)) {
                    return childElement;
                }
            }
        }
        return null;
    }
}
