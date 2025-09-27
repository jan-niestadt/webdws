<!--
  Main Editor View - XML Document Editor with Dual Mode Support
  
  This component provides:
  - Text-based XML editing using Monaco Editor (read-only mode)
  - Tree-based XML editing with visual node manipulation
  - Document management (new, load, save, validate)
  - Mode switching between text and tree editors
  - XPath query interface integration
-->
<template>
  <div class="editor-container">
    <div class="editor-header">
      <div class="document-info">
        <input 
          v-model="documentName" 
          class="document-name-input"
          placeholder="Document name..."
          @blur="saveDocument"
        />
        <span v-if="isModified" class="modified-indicator">*</span>
      </div>
      <div class="editor-actions">
        <div class="mode-toggle">
          <button 
            @click="setMode('text')" 
            :class="['btn', 'btn-sm', { 'active': editorMode === 'text' }]"
          >
            XML View
          </button>
          <button 
            @click="setMode('tree')" 
            :class="['btn', 'btn-sm', { 'active': editorMode === 'tree' }]"
          >
            Tree Editor
          </button>
        </div>
        <button @click="newDocument" class="btn btn-secondary">New</button>
        <button @click="loadDocument" class="btn btn-secondary">Load</button>
        <button @click="saveDocument" class="btn btn-success" :disabled="!isModified">Save</button>
        <button @click="validateXml" class="btn">Validate</button>
      </div>
    </div>

    <div class="editor-content">
      <div class="document-list" v-if="showDocumentList">
        <h3>Available Documents</h3>
        <div class="document-item" 
             v-for="doc in documents" 
             :key="doc.id"
             @click="selectDocument(doc)"
             :class="{ active: selectedDocument?.id === doc.id }">
          <div class="document-name">{{ doc.name }}</div>
          <div class="document-meta">
            <small>Updated: {{ formatDate(doc.updatedAt) }}</small>
            <button @click.stop="deleteDocument(doc.id)" class="btn-delete">×</button>
          </div>
        </div>
      </div>

      <div class="editor-main">
        <div class="editor-toolbar">
          <span class="editor-status">
            <span v-if="isLoading" class="loading">Loading...</span>
            <span v-else-if="isLoadingSchema" class="loading">Loading Schema...</span>
            <span v-else-if="validationResult" :class="validationResult.valid ? 'valid' : 'invalid'">
              {{ validationResult.valid ? '✓ Valid XML' : '✗ Invalid XML' }}
            </span>
            <span v-else-if="isModified">Modified</span>
            <span v-else-if="schemaInfo" class="schema-loaded">📋 Schema: {{ rootElement?.name || 'Loaded' }}</span>
            <span v-else>Ready</span>
          </span>
        </div>
        
        <div v-if="editorMode === 'text'" class="monaco-editor-container" ref="editorContainer"></div>
        <XmlTreeEditor 
          v-else-if="editorMode === 'tree'"
          :initial-xml-content="initialTreeContent"
          :schema-info="schemaInfo"
          :root-element="rootElement"
          @xml-changed="handleXmlChanged"
          @tree-modified="handleTreeModified"
          ref="treeEditorRef"
        />
      </div>
    </div>

    <div v-if="error" class="alert alert-error" style="white-space: pre-wrap;">
      {{ error }}
    </div>

    <!-- Detailed Validation Errors -->
    <div v-if="validationResult && !validationResult.valid && validationResult.error" class="collapsible-panel">
      <div class="panel-header" @click="showValidationErrors = !showValidationErrors">
        <h4>Validation Errors</h4>
        <span class="panel-toggle">{{ showValidationErrors ? '▼' : '▶' }}</span>
      </div>
      <div v-if="showValidationErrors" class="panel-content">
        <div class="error-details">
          {{ validationResult.error }}
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { xmlApi, schemaApi } from '@/services/api';
import type { XmlDocument, SaveXmlRequest } from '@/types/xml';
import type { SchemaInfo, SchemaElement } from '@/services/api';
import { xmlService } from '@/services/xmlService';
import * as monaco from 'monaco-editor';
import XmlTreeEditor from '@/components/XmlTreeEditor.vue';

// Reactive state
const documentName = ref('');
const xmlContent = ref('');
const isModified = ref(false);
const isLoading = ref(false);
const error = ref('');
const showDocumentList = ref(false);
const documents = ref<XmlDocument[]>([]);
const selectedDocument = ref<XmlDocument | null>(null);
const validationResult = ref<{ valid: boolean; error?: string } | null>(null);
const editorMode = ref<'text' | 'tree'>('text');
const initialTreeContent = ref('');

// Schema-related state
const schemaInfo = ref<SchemaInfo | null>(null);
const isLoadingSchema = ref(false);
const schemaError = ref('');
const rootElement = ref<SchemaElement | null>(null);

// Panel visibility state
const showValidationErrors = ref(false);

// Editor instance
let editor: monaco.editor.IStandaloneCodeEditor | null = null;
const editorContainer = ref<HTMLElement>();
const treeEditorRef = ref<InstanceType<typeof XmlTreeEditor>>();

// Initialize Monaco Editor
const initEditor = () => {
  if (!editorContainer.value) return;


  editor = monaco.editor.create(editorContainer.value, {
    value: xmlContent.value,
    language: 'xml',
    theme: 'vs-dark',
    automaticLayout: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    wordWrap: 'on',
    lineNumbers: 'on',
    folding: true,
    bracketPairColorization: { enabled: true },
    formatOnPaste: editorMode.value === 'tree', // Only allow formatting in tree mode
    formatOnType: editorMode.value === 'tree',  // Only allow formatting in tree mode
    readOnly: editorMode.value === 'text', // Make text editor read-only only in text mode
  });

  // Only listen for content changes in tree mode (when editor is editable)
  if (editorMode.value === 'tree' && editor) {
    editor.onDidChangeModelContent(() => {
      if (editor) {
        xmlContent.value = editor.getValue();
        isModified.value = true;
        validationResult.value = null;
      }
    });
  }
  // In text mode, the editor is read-only and should never trigger content changes
};

// Load schema
const loadSchema = async () => {
  try {
    isLoadingSchema.value = true;
    schemaError.value = '';
    const schema = await schemaApi.getDefaultSchema();
    schemaInfo.value = schema;
    
    // Find the root element (first element in the schema)
    if (schema.elements && schema.elements.length > 0) {
      rootElement.value = schema.elements[0];
    }
  } catch (err) {
    schemaError.value = `Failed to load schema: ${err}`;
    console.warn('Schema loading failed:', err);
  } finally {
    isLoadingSchema.value = false;
  }
};

// Load documents list
const loadDocuments = async () => {
  try {
    isLoading.value = true;
    const response = await xmlApi.getDocuments();
    documents.value = response.documents;
  } catch (err) {
    error.value = `Failed to load documents: ${err}`;
  } finally {
    isLoading.value = false;
  }
};

// Load a specific document
const loadDocument = async () => {
  showDocumentList.value = !showDocumentList.value;
  if (showDocumentList.value) {
    await loadDocuments();
  }
};

// Select and load a document
const selectDocument = async (doc: XmlDocument) => {
  try {
    isLoading.value = true;
    const fullDoc = await xmlApi.getDocument(doc.id);
    selectedDocument.value = fullDoc;
    documentName.value = fullDoc.name;
    
    // Format XML content if in text mode
    if (editorMode.value === 'text' && fullDoc.content.trim()) {
      try {
        const formattedXml = xmlService.formatXml(fullDoc.content);
        xmlContent.value = formattedXml;
        initialTreeContent.value = fullDoc.content; // Keep original for tree mode
      } catch (error) {
        console.warn('Failed to format loaded XML:', error);
        xmlContent.value = fullDoc.content;
        initialTreeContent.value = fullDoc.content;
      }
    } else {
      xmlContent.value = fullDoc.content;
      initialTreeContent.value = fullDoc.content;
    }
    
    if (editor) {
      editor.setValue(xmlContent.value);
    }
    
    isModified.value = false;
    showDocumentList.value = false;
  } catch (err) {
    error.value = `Failed to load document: ${err}`;
  } finally {
    isLoading.value = false;
  }
};

// Save document
const saveDocument = async () => {
  
  // Get current XML content from tree editor if in tree mode
  let currentXmlContent = xmlContent.value;
  if (editorMode.value === 'tree' && treeEditorRef.value) {
    currentXmlContent = treeEditorRef.value.getCurrentXmlContent();
  }
  
  if (!documentName.value.trim() || !currentXmlContent.trim()) {
    error.value = 'Document name and content are required';
    return;
  }

  try {
    isLoading.value = true;
    const documentData: SaveXmlRequest = {
      name: documentName.value.trim(),
      content: currentXmlContent
    };

    if (selectedDocument.value) {
      await xmlApi.updateDocument(selectedDocument.value.id, documentData);
    } else {
      const newDoc = await xmlApi.saveDocument(documentData);
      selectedDocument.value = newDoc;
    }

    isModified.value = false;
    error.value = '';
  } catch (err) {
    error.value = `Failed to save document: ${err}`;
  } finally {
    isLoading.value = false;
  }
};

// Helper function to get default attribute value
const getDefaultAttributeValue = (attr: { type: string; defaultValue?: string; fixedValue?: string }): string => {
  // Use default value if specified in schema
  if (attr.defaultValue) {
    return attr.defaultValue;
  }
  
  // Use fixed value if specified in schema
  if (attr.fixedValue) {
    return attr.fixedValue;
  }
  
  // Generate type-based defaults
  const type = attr.type.toLowerCase();
  
  if (type.includes('boolean')) {
    return 'false';
  }
  
  if (type.includes('int') && !type.includes('string')) {
    return '0';
  }
  
  if (type.includes('decimal') || type.includes('float') || type.includes('double')) {
    return '0';
  }
  
  if (type.includes('date')) {
    return new Date().toISOString().split('T')[0];
  }
  
  if (type.includes('year')) {
    return new Date().getFullYear().toString();
  }
  
  // For strings and other types, return empty string
  return '';
};

// Helper function to get default text content
const getDefaultTextContent = (element: any): string => {
  // Use default value if specified in schema
  if (element.defaultValue) {
    return element.defaultValue;
  }
  
  // Use fixed value if specified in schema
  if (element.fixedValue) {
    return element.fixedValue;
  }
  
  // Generate type-based defaults for text content (but not for date fields)
  const type = element.type.toLowerCase();
  
  if (type.includes('boolean')) {
    return 'false';
  }
  
  if (type.includes('int') && !type.includes('string')) {
    return '0';
  }
  
  if (type.includes('decimal') || type.includes('float') || type.includes('double')) {
    return '0';
  }
  
  // Don't provide defaults for date fields - leave them empty
  // if (type.includes('date')) {
  //   return new Date().toISOString().split('T')[0];
  // }
  
  // if (type.includes('year')) {
  //   return new Date().getFullYear().toString();
  // }
  
  // For strings and other types, return empty string
  return '';
};

// Unified function to create XML element with required content
const createElementXML = (element: any, indentLevel: number = 0, isRoot: boolean = false): string => {
  const indent = '  '.repeat(indentLevel);
  
  let xml = '';
  
  // Add XML declaration for root element
  if (isRoot) {
    xml += '<?xml version="1.0" encoding="UTF-8"?>\n';
  }
  
  // Start element tag
  xml += `${indent}<${element.name}`;
  
  // Add required attributes
  if (element.attributes) {
    for (const attr of element.attributes) {
      if (attr.use === 'required') {
        const value = getDefaultAttributeValue(attr);
        xml += ` ${attr.name}="${value}"`;
      }
    }
  }
  
  xml += '>';
  
  // Add default text content if element has a simple type
  if (element.type && !element.children) {
    const textContent = getDefaultTextContent(element);
    if (textContent) {
      xml += textContent;
    }
  } else if (element.children) {
    // Add required child elements recursively
    for (const child of element.children) {
      if (child.minOccurs > 0) {
        xml += createElementXML(child, indentLevel + 1);
      }
    }
  }
  
  // Close element tag
  xml += `</${element.name}>`;
  
  // Add newline for non-root elements
  if (!isRoot) {
    xml += '\n';
  }
  
  return xml;
};

// Helper function to create root element with required content
const createRootElementWithRequiredContent = (rootElement: any): string => {
  return createElementXML(rootElement, 0, true);
};

// Create new document
const newDocument = () => {
  selectedDocument.value = null;
  documentName.value = '';
  
  // Use schema root element if available, otherwise fallback to 'root'
  const rootElementName = rootElement.value?.name || 'root';
  let newXml = `<?xml version="1.0" encoding="UTF-8"?>\n<${rootElementName}></${rootElementName}>`;
  
  // If schema is available, create a more complete root element with required content
  if (rootElement.value && schemaInfo.value) {
    newXml = createRootElementWithRequiredContent(rootElement.value);
  }
  
  // Format XML content if in text mode
  if (editorMode.value === 'text') {
    try {
      xmlContent.value = xmlService.formatXml(newXml);
      initialTreeContent.value = newXml; // Keep original for tree mode
    } catch (error) {
      console.warn('Failed to format new XML:', error);
      xmlContent.value = newXml;
      initialTreeContent.value = newXml;
    }
  } else {
    xmlContent.value = newXml;
    initialTreeContent.value = newXml;
  }
  
  if (editor) {
    editor.setValue(xmlContent.value);
  }
  
  isModified.value = false;
  showDocumentList.value = false;
  validationResult.value = null;
};

// Delete document
const deleteDocument = async (id: string) => {
  if (!confirm('Are you sure you want to delete this document?')) return;

  try {
    await xmlApi.deleteDocument(id);
    await loadDocuments();
    
    if (selectedDocument.value?.id === id) {
      newDocument();
    }
  } catch (err) {
    error.value = `Failed to delete document: ${err}`;
  }
};

// Validate XML
const validateXml = async () => {
  try {
    isLoading.value = true;
    validationResult.value = null;
    
    // Get current XML content from tree editor if in tree mode
    let currentXmlContent = xmlContent.value;
    if (editorMode.value === 'tree' && treeEditorRef.value) {
      currentXmlContent = treeEditorRef.value.getCurrentXmlContent();
    }
    
    if (!currentXmlContent.trim()) {
      validationResult.value = { valid: false, error: 'No content to validate' };
      error.value = 'No content to validate';
      return;
    }
    
    // First validate basic XML structure
    const basicResult = await xmlApi.validateXml(currentXmlContent);
    if (!basicResult.valid) {
      validationResult.value = basicResult;
      error.value = basicResult.error || 'XML validation failed';
      showValidationErrors.value = true;
      return;
    }
    
    // If schema is available, perform schema validation
    if (schemaInfo.value) {
      const schemaValidationResult = await validateAgainstSchema(currentXmlContent);
      validationResult.value = schemaValidationResult;
      
      if (!schemaValidationResult.valid) {
        error.value = schemaValidationResult.error || 'Schema validation failed';
        showValidationErrors.value = true;
      } else {
        error.value = '';
        showValidationErrors.value = false;
      }
    } else {
      // No schema available, just use basic validation
      validationResult.value = basicResult;
      if (basicResult.valid) {
        error.value = '';
        showValidationErrors.value = false;
      } else {
        error.value = basicResult.error || 'Validation failed';
        showValidationErrors.value = true;
      }
    }
  } catch (err) {
    validationResult.value = { valid: false, error: `Validation error: ${err}` };
    error.value = `Validation failed: ${err}`;
    showValidationErrors.value = true;
  } finally {
    isLoading.value = false;
  }
};

// Validate XML against schema
const validateAgainstSchema = async (xmlContent: string): Promise<{ valid: boolean; error?: string }> => {
  if (!schemaInfo.value) {
    return { valid: true }; // No schema to validate against
  }
  
  try {
    // Parse the XML content
    const parseResult = xmlService.parseXml(xmlContent);
    if (!parseResult.success || !parseResult.document) {
      return { valid: false, error: 'Invalid XML structure' };
    }
    
    const rootElement = parseResult.document.documentElement;
    if (!rootElement) {
      return { valid: false, error: 'No root element found' };
    }
    
    // Check if root element matches schema
    const expectedRootElement = schemaInfo.value.elements[0];
    if (expectedRootElement && rootElement.tagName !== expectedRootElement.name) {
      return { 
        valid: false, 
        error: `Root element must be '${expectedRootElement.name}', found '${rootElement.tagName}'` 
      };
    }
    
    // Validate the entire document tree
    const validationResult = validateElementAgainstSchema(rootElement, expectedRootElement, schemaInfo.value);
    return validationResult;
  } catch (error) {
    return { valid: false, error: `Schema validation error: ${error}` };
  }
};

// Recursively validate an element and its children against schema
const validateElementAgainstSchema = (element: Element, schemaElement: any, schemaInfo: any): { valid: boolean; error?: string } => {
  // Check required attributes
  if (schemaElement?.attributes) {
    for (const attr of schemaElement.attributes) {
      if (attr.use === 'required' && !element.hasAttribute(attr.name)) {
        return { 
          valid: false, 
          error: `Required attribute '${attr.name}' is missing on element '${element.tagName}'` 
        };
      }
      
      // Validate attribute values if present
      if (element.hasAttribute(attr.name)) {
        const attrValue = element.getAttribute(attr.name) || '';
        const validationResult = validateValue(attrValue, attr.type);
        if (!validationResult.valid) {
          return { 
            valid: false, 
            error: `Invalid value for attribute '${attr.name}' on element '${element.tagName}': ${validationResult.error}` 
          };
        }
      }
    }
  }
  
  // Validate text content if element has text
  const textContent = element.textContent?.trim();
  if (textContent && schemaElement?.type) {
    const validationResult = validateValue(textContent, schemaElement.type);
    if (!validationResult.valid) {
      return { 
        valid: false, 
        error: `Invalid text content for element '${element.tagName}': ${validationResult.error}` 
      };
    }
  }
  
  // Validate child elements
  if (schemaElement?.children) {
    const childElements = Array.from(element.children);
    const childElementCounts: { [key: string]: number } = {};
    
    for (const childElement of childElements) {
      const childName = childElement.tagName;
      childElementCounts[childName] = (childElementCounts[childName] || 0) + 1;
      
      // Find schema definition for this child
      const childSchema = schemaElement.children.find((child: any) => child.name === childName);
      if (!childSchema) {
        return { 
          valid: false, 
          error: `Element '${childName}' is not allowed as child of '${element.tagName}'` 
        };
      }
      
      // Check occurrence constraints
      if (childElementCounts[childName] > childSchema.maxOccurs) {
        return { 
          valid: false, 
          error: `Element '${childName}' appears too many times (max: ${childSchema.maxOccurs})` 
        };
      }
      
      // Recursively validate child element
      const childValidation = validateElementAgainstSchema(childElement, childSchema, schemaInfo);
      if (!childValidation.valid) {
        return childValidation;
      }
    }
    
    // Check minimum occurrences
    for (const childSchema of schemaElement.children) {
      const actualCount = childElementCounts[childSchema.name] || 0;
      if (actualCount < childSchema.minOccurs) {
        return { 
          valid: false, 
          error: `Element '${childSchema.name}' must appear at least ${childSchema.minOccurs} times (found: ${actualCount})` 
        };
      }
    }
  }
  
  return { valid: true };
};

// Type validation function (same as in XmlTreeEditor)
const validateValue = (value: string, type: string): { valid: boolean; error?: string } => {
  if (!value.trim()) {
    return { valid: true }; // Empty values are valid (handled by required validation)
  }
  
  const normalizedType = type.toLowerCase();
  
  // Boolean validation
  if (normalizedType.includes('boolean')) {
    const lowerValue = value.toLowerCase();
    if (lowerValue === 'true' || lowerValue === 'false' || lowerValue === '1' || lowerValue === '0') {
      return { valid: true };
    }
    return { valid: false, error: 'Boolean values must be "true", "false", "1", or "0"' };
  }
  
  // Integer validation
  if (normalizedType.includes('int') && !normalizedType.includes('string')) {
    if (/^-?\d+$/.test(value)) {
      return { valid: true };
    }
    return { valid: false, error: 'Integer values must contain only digits (optionally with leading minus sign)' };
  }
  
  // Decimal/float validation
  if (normalizedType.includes('decimal') || normalizedType.includes('float') || normalizedType.includes('double')) {
    if (/^-?\d+(\.\d+)?$/.test(value)) {
      return { valid: true };
    }
    return { valid: false, error: 'Decimal values must be valid numbers (e.g., "123.45", "-67.89")' };
  }
  
  // Date validation (basic)
  if (normalizedType.includes('date')) {
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      const date = new Date(value);
      if (!isNaN(date.getTime())) {
        return { valid: true };
      }
    }
    return { valid: false, error: 'Date values must be in YYYY-MM-DD format' };
  }
  
  // Year validation
  if (normalizedType.includes('year')) {
    if (/^\d{4}$/.test(value)) {
      const year = parseInt(value);
      if (year >= 1 && year <= 9999) {
        return { valid: true };
      }
    }
    return { valid: false, error: 'Year values must be 4-digit years (1-9999)' };
  }
  
  // ID validation
  if (normalizedType.includes('id')) {
    if (/^[a-zA-Z_][a-zA-Z0-9_.-]*$/.test(value)) {
      return { valid: true };
    }
    return { valid: false, error: 'ID values must start with a letter or underscore and contain only letters, numbers, underscores, dots, and hyphens' };
  }
  
  // String validation (most permissive)
  if (normalizedType.includes('string')) {
    return { valid: true };
  }
  
  // Default: allow any value
  return { valid: true };
};

// Format date
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString();
};

// Mode switching
const setMode = async (mode: 'text' | 'tree') => {
  
  editorMode.value = mode;
  
  if (mode === 'text') {
    // Wait for the DOM to update
    await nextTick();
    
    // Dispose existing editor if it exists
    if (editor) {
      editor.dispose();
      editor = null;
    }
    
    // Format XML content for pretty printing
    if (xmlContent.value.trim()) {
      try {
        const formattedXml = xmlService.formatXml(xmlContent.value);
        xmlContent.value = formattedXml;
      } catch (error) {
        console.warn('Failed to format XML:', error);
        // Continue with unformatted XML if formatting fails
      }
    }
    
    // Reinitialize the editor with formatted content (read-only)
  } else {
    // Switching to tree mode - dispose and recreate editor to ensure clean state
    if (editor) {
      editor.dispose();
      editor = null;
    }
    
    // Update initial tree content with current XML content
    initialTreeContent.value = xmlContent.value;
    
    // Wait for DOM update and recreate editor in tree mode
    await nextTick();
  }
  initEditor();
};

// Handle tree modifications
const handleTreeModified = (modified: boolean) => {
  isModified.value = modified;
  validationResult.value = null;
};

// Handle XML content changes from tree editor
const handleXmlChanged = (newContent: string) => {
  // Update xmlContent.value so XML view shows current content
  xmlContent.value = newContent;
  isModified.value = true;
  validationResult.value = null;
};


// Lifecycle
onMounted(async () => {
  await nextTick();
  initEditor();
  
  // Load schema first, then create new document
  await loadSchema();
  newDocument(); // Start with a new document
});

onUnmounted(() => {
  if (editor) {
    editor.dispose();
  }
});
</script>

<style scoped>
.editor-container {
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  overflow: hidden;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.document-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.document-name-input {
  border: none;
  background: transparent;
  font-size: 1.2rem;
  font-weight: 500;
  color: #2c3e50;
  min-width: 200px;
}

.document-name-input:focus {
  outline: none;
  background: white;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #3498db;
}

.modified-indicator {
  color: #e74c3c;
  font-weight: bold;
  font-size: 1.2rem;
}

.editor-actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.mode-toggle {
  display: flex;
  gap: 0.25rem;
  margin-right: 1rem;
  padding: 0.25rem;
  background: #e9ecef;
  border-radius: 6px;
}

.mode-toggle .btn {
  margin: 0;
  border: none;
  background: transparent;
  color: #6c757d;
  padding: 0.375rem 0.75rem;
  border-radius: 4px;
  transition: all 0.2s;
}

.mode-toggle .btn.active {
  background: white;
  color: #2c3e50;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.mode-toggle .btn:hover:not(.active) {
  background: rgba(255, 255, 255, 0.5);
  color: #2c3e50;
}

.editor-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.document-list {
  width: 300px;
  background: #f8f9fa;
  border-right: 1px solid #dee2e6;
  padding: 1rem;
  overflow-y: auto;
}

.document-list h3 {
  margin-bottom: 1rem;
  color: #2c3e50;
}

.document-item {
  padding: 0.75rem;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  margin-bottom: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.document-item:hover {
  border-color: #3498db;
  box-shadow: 0 2px 4px rgba(52, 152, 219, 0.1);
}

.document-item.active {
  border-color: #3498db;
  background: #e3f2fd;
}

.document-name {
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 0.25rem;
}

.document-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #6c757d;
  font-size: 0.875rem;
}

.btn-delete {
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  cursor: pointer;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-delete:hover {
  background: #c0392b;
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.editor-toolbar {
  padding: 0.5rem 1rem;
  background: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  font-size: 0.875rem;
  color: #6c757d;
}

.editor-status .valid {
  color: #27ae60;
}

.editor-status .invalid {
  color: #e74c3c;
}

.editor-status .schema-loaded {
  color: #007bff;
  font-size: 0.9em;
}

.monaco-editor-container {
  flex: 1;
  min-height: 400px;
}

.loading {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.loading::before {
  content: '';
  width: 12px;
  height: 12px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.collapsible-panel {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  margin: 1rem 0;
  overflow: hidden;
}

.panel-header {
  background: #e9ecef;
  padding: 0.75rem 1rem;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  user-select: none;
  transition: background-color 0.2s;
}

.panel-header:hover {
  background: #dee2e6;
}

.panel-header h3,
.panel-header h4 {
  margin: 0;
  color: #495057;
  font-size: 1rem;
}

.panel-toggle {
  font-size: 0.8rem;
  color: #6c757d;
  transition: transform 0.2s;
}

.panel-content {
  padding: 1rem;
}


.error-details {
  color: #721c24;
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.4;
  white-space: pre-wrap;
}
</style>
