<!--
  XML Tree Editor - Visual Tree-Based XML Editor
  
  This component provides:
  - Visual tree representation of XML documents
  - Node manipulation (add, delete, edit elements, text, comments)
  - XML structure validation and enforcement
  - Integration with XPath query interface
  - Real-time synchronization with text editor
  - Node property editing and expansion controls
-->
<template>
  <div class="xml-tree-editor">
    <div class="tree-toolbar">
      <div class="toolbar-left">
        <button 
          @click="addElement" 
          class="btn btn-sm" 
          :disabled="!selectedNode || !canHaveChildren(selectedNode)"
          :title="!selectedNode ? 'Select a node first' : !canHaveChildren(selectedNode) ? 'Only element nodes can have children' : 'Add new element'"
        >
          Add Element
        </button>
        <button 
          @click="addText" 
          class="btn btn-sm" 
          :disabled="!selectedNode || !canHaveChildren(selectedNode)"
          :title="!selectedNode ? 'Select a node first' : !canHaveChildren(selectedNode) ? 'Only element nodes can have children' : 'Add text content'"
        >
          Add Text
        </button>
        <button 
          @click="addComment" 
          class="btn btn-sm" 
          :disabled="!selectedNode || !canHaveChildren(selectedNode)"
          :title="!selectedNode ? 'Select a node first' : !canHaveChildren(selectedNode) ? 'Only element nodes can have children' : 'Add comment'"
        >
          Add Comment
        </button>
        <button 
          @click="deleteNode" 
          class="btn btn-sm btn-danger" 
          :disabled="!selectedNode || isRoot"
          :title="!selectedNode ? 'Select a node first' : isRoot ? 'Cannot delete root element' : 'Delete selected node'"
        >
          Delete
        </button>
      </div>
      <div class="toolbar-right">
        <button @click="expandAll" class="btn btn-sm">Expand All</button>
        <button @click="collapseAll" class="btn btn-sm">Collapse All</button>
      </div>
    </div>

    <div class="tree-container">
      <div v-if="!treeState.root" class="empty-state">
        <p>No XML content to display</p>
        <button @click="createRootElement" class="btn">Create Root Element</button>
      </div>
      
      <div v-else class="tree-content">
        <XmlTreeNode
          :node="treeState.root"
          :selected-id="treeState.selectedNodeId"
          @select="selectNode"
          @toggle="toggleNode"
          @edit="editNode"
          @add-child="addChildNode"
        />
      </div>
    </div>

    <!-- XPath Query Panel -->
    <XPathQuery :xmlContent="props.xmlContent" />

    <!-- Node Properties Panel -->
    <div v-if="selectedNode" class="properties-panel">
      <h4>Node Properties</h4>
      <div class="property-group">
        <label>Type:</label>
        <span class="node-type">{{ selectedNode.type }}</span>
      </div>
      
      <div v-if="selectedNode.type === 'element'" class="property-group">
        <label>Name:</label>
        <input 
          v-model="editingNode.name" 
          @blur="updateNode"
          class="property-input"
        />
      </div>
      
      <div v-if="selectedNode.type === 'text' || selectedNode.type === 'comment'" class="property-group">
        <label>Value:</label>
        <textarea 
          v-model="editingNode.value" 
          @blur="updateNode"
          class="property-textarea"
          rows="3"
        ></textarea>
      </div>
      
      <div v-if="selectedNode.type === 'element'" class="property-group">
        <label>Attributes:</label>
        <div class="attributes-list">
          <div 
            v-for="(value, key) in selectedNode.attributes" 
            :key="key"
            class="attribute-item"
          >
            <input 
              v-model="editingAttributeKeys[key]" 
              @blur="updateAttributeKey(key, editingAttributeKeys[key])"
              @keyup.enter="updateAttributeKey(key, editingAttributeKeys[key])"
              :class="['attribute-key', { 'invalid': !isValidAttributeName(editingAttributeKeys[key]) }]"
              placeholder="Attribute name"
              title="Attribute names must start with a letter or underscore and contain only letters, numbers, underscores, dots, and hyphens"
            />
            <input 
              v-model="selectedNode.attributes![key]" 
              @blur="updateAttributeValue(key, selectedNode.attributes![key])"
              @keyup.enter="updateAttributeValue(key, selectedNode.attributes![key])"
              class="attribute-value"
              placeholder="Attribute value"
            />
            <button @click="removeAttribute(key)" class="btn-remove-attr" title="Remove attribute">×</button>
          </div>
          <button @click="addAttribute" class="btn btn-sm">Add Attribute</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import type { XmlNode, XmlTreeState } from '@/types/xml';
import XmlTreeNode from './XmlTreeNode.vue';
import XPathQuery from './XPathQuery.vue';
import { xmlService } from '@/services/xmlService';

// Props
const props = defineProps<{
  xmlContent: string;
}>();

// Emits
const emit = defineEmits<{
  'update:xmlContent': [content: string];
  'tree-modified': [modified: boolean];
}>();

// State
const treeState = ref<XmlTreeState>({
  root: null,
  selectedNodeId: null,
  modified: false
});

const editingNode = ref<XmlNode | null>(null);
const editingAttributeKeys = ref<{ [key: string]: string }>({});

// Computed
const selectedNode = computed(() => {
  if (!treeState.value.selectedNodeId || !treeState.value.root) return null;
  return findNodeById(treeState.value.root, treeState.value.selectedNodeId);
});

const isRoot = computed(() => {
  return selectedNode.value?.id === treeState.value.root?.id;
});

// Methods
const generateId = () => {
  return Math.random().toString(36).substr(2, 9);
};

const findNodeById = (node: XmlNode, id: string): XmlNode | null => {
  if (node.id === id) return node;
  for (const child of node.children) {
    const found = findNodeById(child, id);
    if (found) return found;
  }
  return null;
};

const parseXmlToTree = async (xmlContent: string): Promise<XmlNode | null> => {
  try {
    console.log('parseXmlToTree called with content:', xmlContent.substring(0, 200) + '...');
    const parseResult = xmlService.parseXml(xmlContent);
    
    if (!parseResult.success || !parseResult.document) {
      console.error('XML parsing failed:', parseResult.error);
      throw new Error(parseResult.error || 'Invalid XML');
    }
    
    console.log('XML parsed successfully, document:', parseResult.document);
    console.log('Document type:', typeof parseResult.document);
    console.log('Document constructor:', parseResult.document.constructor.name);
    console.log('Document element:', parseResult.document.documentElement);
    console.log('Document element type:', typeof parseResult.document.documentElement);
    
    // Check if we have a documentElement
    if (!parseResult.document.documentElement) {
      console.error('No documentElement found in parsed document');
      console.log('Document keys:', Object.keys(parseResult.document));
      console.log('Document properties:', Object.getOwnPropertyNames(parseResult.document));
      throw new Error('No root element found in XML document');
    }
    
    const treeNode = domNodeToXmlNode(parseResult.document.documentElement);
    console.log('Converted to tree node:', treeNode);
    return treeNode;
  } catch (error) {
    console.error('Error parsing XML:', error);
    return null;
  }
};

const domNodeToXmlNode = (domNode: Node, parentId?: string): XmlNode => {
  const id = generateId();
  const node: XmlNode = {
    id,
    type: getNodeType(domNode),
    children: [],
    parent: parentId,
    expanded: true
  };

  if (domNode.nodeType === Node.ELEMENT_NODE) {
    const element = domNode as Element;
    node.name = element.tagName;
    node.attributes = {};
    
    // Copy attributes
    for (let i = 0; i < element.attributes.length; i++) {
      const attr = element.attributes[i];
      node.attributes[attr.name] = attr.value;
    }
  } else if (domNode.nodeType === Node.TEXT_NODE || domNode.nodeType === Node.CDATA_SECTION_NODE) {
    node.value = domNode.textContent || '';
  } else if (domNode.nodeType === Node.COMMENT_NODE) {
    node.value = domNode.textContent || '';
  }

  // Process children
  for (let i = 0; i < domNode.childNodes.length; i++) {
    const child = domNode.childNodes[i];
    if (shouldIncludeNode(child)) {
      const childNode = domNodeToXmlNode(child, id);
      node.children.push(childNode);
    }
  }

  return node;
};

const getNodeType = (domNode: Node): XmlNode['type'] => {
  switch (domNode.nodeType) {
    case Node.ELEMENT_NODE:
      return 'element';
    case Node.TEXT_NODE:
      return 'text';
    case Node.COMMENT_NODE:
      return 'comment';
    case Node.CDATA_SECTION_NODE:
      return 'cdata';
    case Node.PROCESSING_INSTRUCTION_NODE:
      return 'processing-instruction';
    default:
      return 'text';
  }
};

const shouldIncludeNode = (domNode: Node): boolean => {
  // Include all node types except empty or whitespace-only text nodes
  if (domNode.nodeType === Node.TEXT_NODE) {
    const text = domNode.textContent || '';
    // Only include text nodes that have non-whitespace content
    return text.trim().length > 0;
  }
  // Include all other node types (elements, comments, CDATA, etc.)
  return true;
};

const canHaveChildren = (node: XmlNode): boolean => {
  // Only element nodes can have children in XML
  return node.type === 'element';
};

const treeToXml = (node: XmlNode, indent: number = 0): string => {
  console.log('treeToXml called with node:', node);
  
  const indentStr = '  '.repeat(indent);
  
  if (node.type === 'element') {
    let xml = `${indentStr}<${node.name}`;
    
    // Add attributes
    if (node.attributes) {
      for (const [key, value] of Object.entries(node.attributes)) {
        xml += ` ${key}="${value}"`;
      }
    }
    
    if (node.children.length === 0) {
      xml += ' />';
    } else {
      xml += '>';
      
      // Check if all children are text nodes (for inline formatting)
      const allTextChildren = node.children.every(child => child.type === 'text');
      
      if (allTextChildren) {
        // Inline formatting for text content
        for (const child of node.children) {
          xml += treeToXml(child, 0);
        }
        xml += `</${node.name}>`;
      } else {
        // Multi-line formatting for mixed content
        xml += '\n';
        for (const child of node.children) {
          xml += treeToXml(child, indent + 1);
        }
        xml += `${indentStr}</${node.name}>`;
      }
    }
    console.log('Generated XML for element:', xml);
    return xml;
  } else if (node.type === 'text') {
    const result = node.value || '';
    console.log('Generated XML for text:', result);
    return result;
  } else if (node.type === 'comment') {
    const result = `${indentStr}<!--${node.value}-->`;
    console.log('Generated XML for comment:', result);
    return result;
  } else if (node.type === 'cdata') {
    const result = `${indentStr}<![CDATA[${node.value}]]>`;
    console.log('Generated XML for cdata:', result);
    return result;
  }
  return '';
};

const selectNode = (nodeId: string) => {
  treeState.value.selectedNodeId = nodeId;
  const node = findNodeById(treeState.value.root!, nodeId);
  if (node) {
    editingNode.value = { ...node };
    // Initialize editingAttributeKeys with the actual attribute keys, not values
    editingAttributeKeys.value = {};
    if (node.attributes) {
      Object.keys(node.attributes).forEach(key => {
        editingAttributeKeys.value[key] = key;
      });
    } else if (node.type === 'element') {
      // Initialize empty attributes object for element nodes
      node.attributes = {};
    }
  }
};

const toggleNode = (nodeId: string) => {
  const node = findNodeById(treeState.value.root!, nodeId);
  if (node) {
    node.expanded = !node.expanded;
  }
};

const editNode = (nodeId: string, field: string, value: any) => {
  console.log('editNode called:', { nodeId, field, value });
  const node = findNodeById(treeState.value.root!, nodeId);
  if (!node) {
    console.log('Node not found:', nodeId);
    return;
  }
  
  console.log('Found node before update:', JSON.parse(JSON.stringify(node)));
  
  if (field === 'remove') {
    // Remove the node (for empty text nodes)
    console.log('Removing node');
    removeNode(nodeId);
  } else {
    // Update the node property
    console.log('Updating node property:', field, 'to', value);
    (node as any)[field] = value;
    console.log('Node after update:', JSON.parse(JSON.stringify(node)));
    
    // Force reactivity update
    treeState.value = { ...treeState.value };
    
    // Only call markModified - the watcher will handle emitXmlContent
    markModified();
  }
};

const addChildNode = (parentId: string, newNode: XmlNode) => {
  const parent = findNodeById(treeState.value.root!, parentId);
  if (parent) {
    newNode.parent = parentId;
    parent.children.push(newNode);
    markModified();
  }
};

const addElement = () => {
  if (!selectedNode.value || !canHaveChildren(selectedNode.value)) return;
  
  const newElement: XmlNode = {
    id: generateId(),
    type: 'element',
    name: 'newElement',
    children: [],
    parent: selectedNode.value.id
  };
  
  addChildNode(selectedNode.value.id, newElement);
  selectNode(newElement.id);
};

const addText = () => {
  if (!selectedNode.value || !canHaveChildren(selectedNode.value)) return;
  
  const newText: XmlNode = {
    id: generateId(),
    type: 'text',
    value: 'New text content',
    children: [],
    parent: selectedNode.value.id
  };
  
  addChildNode(selectedNode.value.id, newText);
  selectNode(newText.id);
};

const addComment = () => {
  if (!selectedNode.value || !canHaveChildren(selectedNode.value)) return;
  
  const newComment: XmlNode = {
    id: generateId(),
    type: 'comment',
    value: 'New comment',
    children: [],
    parent: selectedNode.value.id
  };
  
  addChildNode(selectedNode.value.id, newComment);
  selectNode(newComment.id);
};

const deleteNode = () => {
  if (!selectedNode.value || isRoot.value) return;
  
  const parent = findNodeById(treeState.value.root!, selectedNode.value.parent!);
  if (parent) {
    parent.children = parent.children.filter(child => child.id !== selectedNode.value!.id);
    treeState.value.selectedNodeId = null;
    editingNode.value = null;
    markModified();
  }
};

const updateNode = () => {
  if (!selectedNode.value || !editingNode.value) return;
  
  // For text nodes, don't allow whitespace-only values
  if (selectedNode.value.type === 'text' && 
      typeof editingNode.value.value === 'string' && 
      editingNode.value.value.trim() === '') {
    // Remove whitespace-only text nodes
    removeNode(selectedNode.value.id);
    return;
  }
  
  Object.assign(selectedNode.value, editingNode.value);
  markModified();
};

const updateAttributeKey = (oldKey: string, newKey: string) => {
  if (!selectedNode.value || !selectedNode.value.attributes) return;
  
  if (newKey !== oldKey && newKey.trim() !== '') {
    // Validate attribute name (XML attribute names must start with letter or underscore)
    if (!/^[a-zA-Z_][a-zA-Z0-9_.-]*$/.test(newKey)) {
      // Invalid attribute name, revert to original
      editingAttributeKeys.value[oldKey] = oldKey;
      return;
    }
    
    // Check if the new key already exists
    if (selectedNode.value.attributes.hasOwnProperty(newKey)) {
      // If the new key already exists, don't update to avoid conflicts
      editingAttributeKeys.value[oldKey] = oldKey; // Revert the display
      return;
    }
    
    const value = selectedNode.value.attributes[oldKey];
    delete selectedNode.value.attributes[oldKey];
    selectedNode.value.attributes[newKey] = value;
    
    // Update the editing keys mapping
    delete editingAttributeKeys.value[oldKey];
    editingAttributeKeys.value[newKey] = newKey;
    
    markModified();
  } else if (newKey.trim() === '') {
    // If the key is empty, revert to the original key
    editingAttributeKeys.value[oldKey] = oldKey;
  }
};

const updateAttributeValue = (key: string, value: string) => {
  if (!selectedNode.value || !selectedNode.value.attributes) return;
  
  selectedNode.value.attributes[key] = value;
  markModified();
};

const removeAttribute = (key: string) => {
  if (!selectedNode.value || !selectedNode.value.attributes) return;
  
  delete selectedNode.value.attributes[key];
  delete editingAttributeKeys.value[key];
  markModified();
};

const addAttribute = () => {
  if (!selectedNode.value) return;
  
  if (!selectedNode.value.attributes) {
    selectedNode.value.attributes = {};
  }
  
  // Generate a unique key for the new attribute
  let counter = 1;
  let newKey = 'newAttribute';
  while (selectedNode.value.attributes.hasOwnProperty(newKey)) {
    newKey = `newAttribute${counter}`;
    counter++;
  }
  
  selectedNode.value.attributes[newKey] = '';
  editingAttributeKeys.value[newKey] = newKey;
  markModified();
};

const isValidAttributeName = (name: string): boolean => {
  if (!name || name.trim() === '') return true; // Allow empty names during editing
  return /^[a-zA-Z_][a-zA-Z0-9_.-]*$/.test(name);
};

const expandAll = () => {
  const expandNode = (node: XmlNode) => {
    node.expanded = true;
    node.children.forEach(expandNode);
  };
  
  if (treeState.value.root) {
    expandNode(treeState.value.root);
  }
};

const collapseAll = () => {
  const collapseNode = (node: XmlNode) => {
    node.expanded = false;
    node.children.forEach(collapseNode);
  };
  
  if (treeState.value.root) {
    collapseNode(treeState.value.root);
  }
};

const createRootElement = () => {
  const rootElement: XmlNode = {
    id: generateId(),
    type: 'element',
    name: 'root',
    children: [],
    expanded: true
  };
  
  treeState.value.root = rootElement;
  selectNode(rootElement.id);
  markModified();
};

const markModified = () => {
  treeState.value.modified = true;
  emit('tree-modified', true);
};

const emitXmlContent = () => {
  if (treeState.value.root && !isUpdatingFromExternal && !isEmittingContent) {
    const xmlContent = '<?xml version="1.0" encoding="UTF-8"?>\n' + treeToXml(treeState.value.root);
    console.log('Emitting XML content update:', xmlContent.substring(0, 100) + '...');
    
    // Temporarily disable the watcher
    shouldWatchContent.value = false;
    isEmittingContent = true;
    
    emit('update:xmlContent', xmlContent);
    emit('tree-modified', true);
    
    // Re-enable the watcher after the update cycle completes
    nextTick(() => {
      isEmittingContent = false;
      shouldWatchContent.value = true;
    });
  }
};

// Watch for tree changes and emit updates
watch(() => treeState.value.modified, (modified) => {
  if (modified && !isEmittingContent) {
    emitXmlContent();
  }
});

// Sync tree state with external XML content when it changes
let isUpdatingFromExternal = false;
let isEmittingContent = false;

// Create a ref to control the watcher
const shouldWatchContent = ref(true);

watch(() => props.xmlContent, async (newContent) => {
  console.log('XML content watcher triggered with:', newContent?.substring(0, 200) + '...');
  console.log('isUpdatingFromExternal:', isUpdatingFromExternal);
  console.log('isEmittingContent:', isEmittingContent);
  console.log('shouldWatchContent:', shouldWatchContent.value);
  
  if (!shouldWatchContent.value || isUpdatingFromExternal || isEmittingContent) {
    console.log('Skipping watcher update - conditions not met');
    return;
  }
  
  if (newContent && newContent.trim()) {
    const currentXml = treeState.value.root ? '<?xml version="1.0" encoding="UTF-8"?>\n' + treeToXml(treeState.value.root) : '';
    console.log('Current XML from tree:', currentXml.substring(0, 200) + '...');
    console.log('New XML content:', newContent.substring(0, 200) + '...');
    console.log('Are they different?', currentXml !== newContent);
    
    if (currentXml !== newContent) {
      console.log('Updating tree from external content');
      isUpdatingFromExternal = true;
      const parsed = await parseXmlToTree(newContent);
      if (parsed) {
        treeState.value.root = parsed;
        treeState.value.modified = false;
        console.log('Tree updated successfully');
      } else {
        console.error('Failed to parse XML into tree');
      }
      // Use nextTick to ensure the flag is reset after the current update cycle
      await nextTick();
      isUpdatingFromExternal = false;
    } else {
      console.log('Content is the same, no update needed');
    }
  } else if (!newContent || newContent.trim() === '') {
    console.log('Clearing tree state');
    treeState.value.root = null;
    treeState.value.selectedNodeId = null;
    treeState.value.modified = false;
  }
}, { immediate: true, flush: 'post' });

// Initialize
onMounted(async () => {
  if (props.xmlContent && props.xmlContent.trim()) {
    const parsed = await parseXmlToTree(props.xmlContent);
    if (parsed) {
      treeState.value.root = parsed;
    }
  }
});
</script>

<style scoped>
.xml-tree-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
}

.tree-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.toolbar-left, .toolbar-right {
  display: flex;
  gap: 0.5rem;
}

.tree-container {
  flex: 1;
  overflow: auto;
  padding: 1rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #6c757d;
}

.tree-content {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
}

.properties-panel {
  border-top: 1px solid #dee2e6;
  padding: 1rem;
  background: #f8f9fa;
  max-height: 300px;
  overflow-y: auto;
}

.properties-panel h4 {
  margin: 0 0 1rem 0;
  color: #2c3e50;
}

.property-group {
  margin-bottom: 1rem;
}

.property-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.25rem;
  color: #2c3e50;
}

.node-type {
  background: #e9ecef;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 500;
}

.property-input, .property-textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.875rem;
}

.property-input:focus, .property-textarea:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.attributes-list {
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 0.5rem;
  background: white;
  max-height: 200px;
  overflow-y: auto;
}

.attribute-item {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  align-items: center;
}

.attribute-item:last-child {
  margin-bottom: 0;
}

.attribute-key, .attribute-value {
  flex: 1;
  padding: 0.375rem 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.875rem;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.attribute-key:focus, .attribute-value:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.attribute-key.invalid {
  border-color: #e74c3c;
  background-color: #fdf2f2;
}

.attribute-key.invalid:focus {
  border-color: #e74c3c;
  box-shadow: 0 0 0 2px rgba(231, 76, 60, 0.2);
}

.btn-remove-attr {
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-remove-attr:hover {
  background: #c0392b;
}

.btn {
  padding: 0.5rem 1rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  background: white;
  color: #2c3e50;
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  background: #f8f9fa;
  border-color: #3498db;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
}

.btn-danger {
  background: #e74c3c;
  color: white;
  border-color: #e74c3c;
}

.btn-danger:hover:not(:disabled) {
  background: #c0392b;
  border-color: #c0392b;
}
</style>
