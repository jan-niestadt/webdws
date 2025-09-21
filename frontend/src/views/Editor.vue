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
            Text Editor
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
            <span v-else-if="validationResult" :class="validationResult.valid ? 'valid' : 'invalid'">
              {{ validationResult.valid ? '✓ Valid XML' : '✗ Invalid XML' }}
            </span>
            <span v-else-if="isModified">Modified</span>
            <span v-else>Ready</span>
          </span>
        </div>
        
        <div v-if="editorMode === 'text'" class="monaco-editor-container" ref="editorContainer"></div>
        <XmlTreeEditor 
          v-else-if="editorMode === 'tree'"
          v-model:xmlContent="xmlContent"
          @tree-modified="handleTreeModified"
        />
      </div>
    </div>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { xmlApi } from '@/services/api';
import type { XmlDocument, SaveXmlRequest } from '@/types/xml';
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

// Editor instance
let editor: monaco.editor.IStandaloneCodeEditor | null = null;
const editorContainer = ref<HTMLElement>();

// Initialize Monaco Editor
const initEditor = async () => {
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
    formatOnPaste: true,
    formatOnType: true,
    readOnly: editorMode.value === 'text', // Make text editor read-only only in text mode
  });

  // Listen for content changes (disabled for read-only mode)
  // editor.onDidChangeModelContent(() => {
  //   if (editor) {
  //     xmlContent.value = editor.getValue();
  //     isModified.value = true;
  //     validationResult.value = null;
  //   }
  // });
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
    xmlContent.value = fullDoc.content;
    
    if (editor) {
      editor.setValue(fullDoc.content);
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
  console.log('Save button clicked');
  console.log('Document name:', documentName.value);
  console.log('XML content length:', xmlContent.value.length);
  console.log('Is modified:', isModified.value);
  
  if (!documentName.value.trim() || !xmlContent.value.trim()) {
    error.value = 'Document name and content are required';
    return;
  }

  try {
    isLoading.value = true;
    const documentData: SaveXmlRequest = {
      name: documentName.value.trim(),
      content: xmlContent.value
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

// Create new document
const newDocument = () => {
  selectedDocument.value = null;
  documentName.value = '';
  xmlContent.value = '<?xml version="1.0" encoding="UTF-8"?>\n<root></root>';
  
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
  if (!xmlContent.value.trim()) {
    error.value = 'No content to validate';
    return;
  }

  try {
    validationResult.value = await xmlApi.validateXml(xmlContent.value);
    if (!validationResult.value.valid) {
      error.value = validationResult.value.error || 'Invalid XML';
    } else {
      error.value = '';
    }
  } catch (err) {
    error.value = `Validation failed: ${err}`;
  }
};

// Format date
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString();
};

// Mode switching
const setMode = async (mode: 'text' | 'tree') => {
  console.log('Switching to mode:', mode);
  console.log('Current XML content length:', xmlContent.value.length);
  console.log('Current XML content preview:', xmlContent.value.substring(0, 100) + '...');
  
  editorMode.value = mode;
  
  if (mode === 'text') {
    // Wait for the DOM to update
    await nextTick();
    
    // Dispose existing editor if it exists
    if (editor) {
      editor.dispose();
      editor = null;
    }
    
    // Reinitialize the editor with current content
    await initEditor();
  } else if (editor) {
    // Update existing editor for tree mode
    editor.setValue(xmlContent.value);
    editor.updateOptions({ readOnly: false });
  }
};

// Handle tree modifications
const handleTreeModified = (modified: boolean) => {
  isModified.value = modified;
  validationResult.value = null;
};

// Lifecycle
onMounted(async () => {
  await nextTick();
  await initEditor();
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
</style>
