package com.webdws.service;

import com.webdws.dto.SchemaInfoDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaServiceTest {
    
    @Test
    public void testLoadDefaultSchema() throws Exception {
        SchemaService schemaService = new SchemaService();
        SchemaInfoDto schema = schemaService.loadDefaultSchema();
        
        assertNotNull(schema);
        assertEquals("http://example.com/library", schema.getTargetNamespace());
        assertEquals("qualified", schema.getElementFormDefault());
        assertNotNull(schema.getElements());
        assertFalse(schema.getElements().isEmpty());
        
        // Check that we have the library element
        boolean hasLibraryElement = schema.getElements().stream()
            .anyMatch(element -> "library".equals(element.getName()));
        assertTrue(hasLibraryElement, "Schema should contain library element");
    }
}
