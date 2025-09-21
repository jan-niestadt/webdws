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
        <!-- Schema-aware element addition -->
        <div v-if="schemaInfo && selectedNode && canHaveChildren(selectedNode)" class="element-dropdown">
          <select 
            @change="addElementFromSchema" 
            class="btn btn-sm"
            :disabled="!selectedNode || !canHaveChildren(selectedNode) || isLoadingAllowedContent"
          >
            <option value="">
              {{ isLoadingAllowedContent ? 'Loading...' : 'Add Element...' }}
            </option>
            <option 
              v-for="element in getAllowedElements()" 
              :key="element" 
              :value="element"
            >
              {{ element }}
            </option>
          </select>
        </div>
        <button 
          v-else
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
    <div class="collapsible-panel">
      <div class="panel-header" @click="showXPathPanel = !showXPathPanel">
        <h4>XPath 3.1 Query</h4>
        <span class="panel-toggle">{{ showXPathPanel ? '▼' : '▶' }}</span>
      </div>
      <div v-if="showXPathPanel" class="panel-content">
        <XPathQuery :xmlContent="getCurrentXmlContent()" />
      </div>
    </div>

    <!-- Node Properties Panel -->
    <div v-if="selectedNode && editingNode" class="collapsible-panel">
      <div class="panel-header" @click="showPropertiesPanel = !showPropertiesPanel">
        <h4>Node Properties</h4>
        <span class="panel-toggle">{{ showPropertiesPanel ? '▼' : '▶' }}</span>
      </div>
      <div v-if="showPropertiesPanel" class="panel-content">
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
              v-for="(_, key) in selectedNode.attributes" 
              :key="key"
              class="attribute-item"
            >
              <input 
                v-model="editingAttributeKeys[key as string]" 
                @blur="updateAttributeKey(key as string, editingAttributeKeys[key as string])"
                @keyup.enter="updateAttributeKey(key as string, editingAttributeKeys[key as string])"
                :class="['attribute-key', { 'invalid': !isValidAttributeName(editingAttributeKeys[key as string]) }]"
                placeholder="Attribute name"
                title="Attribute names must start with a letter or underscore and contain only letters, numbers, underscores, dots, and hyphens"
              />
              <input 
                v-model="selectedNode.attributes![key as string]" 
                @blur="updateAttributeValue(key as string, selectedNode.attributes![key as string])"
                @keyup.enter="updateAttributeValue(key as string, selectedNode.attributes![key as string])"
                class="attribute-value"
                placeholder="Attribute value"
              />
              <button @click="removeAttribute(key as string)" class="btn-remove-attr" title="Remove attribute">×</button>
            </div>
            <button @click="addAttribute" class="btn btn-sm">Add Attribute</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Schema-aware allowed content information -->
    <div v-if="allowedContent && selectedNode" class="collapsible-panel">
      <div class="panel-header" @click="showAllowedContentPanel = !showAllowedContentPanel">
        <h4>Allowed Content</h4>
        <span class="panel-toggle">{{ showAllowedContentPanel ? '▼' : '▶' }}</span>
      </div>
      <div v-if="showAllowedContentPanel" class="panel-content">
        <div v-if="allowedContent.elements.length > 0" class="content-group">
          <label>Elements:</label>
          <div class="allowed-elements">
            <div v-for="element in allowedContent.elements" :key="element.name" class="allowed-item">
              <span class="element-name">{{ element.name }}</span>
              <span class="occurrence-info">
                ({{ element.currentCount }}/{{ element.maxOccurs === 'unbounded' ? '∞' : element.maxOccurs }})
              </span>
              <span v-if="element.canAdd" class="can-add">✓</span>
              <span v-else class="cannot-add">✗</span>
            </div>
          </div>
        </div>
        <div v-if="allowedContent.attributes.length > 0" class="content-group">
          <label>Attributes:</label>
          <div class="allowed-attributes">
            <div v-for="attr in allowedContent.attributes" :key="attr.name" class="allowed-item">
              <span class="attribute-name">{{ attr.name }}</span>
              <span v-if="attr.required" class="required">*</span>
              <span v-if="attr.present" class="present">✓</span>
              <span v-else class="not-present">✗</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import type { XmlNode, XmlTreeState } from '@/types/xml';
import XmlTreeNode from './XmlTreeNode.vue';
import XPathQuery from './XPathQuery.vue';
import { xmlService, type SchemaInfo, type AllowedContent } from '@/services/xmlService';

// Props
const props = defineProps<{
  initialXmlContent: string;
  schemaInfo?: SchemaInfo | null;
  schemaContent?: string | null;
}>();

// Emits
const emit = defineEmits<{
  'xml-changed': [content: string];
  'tree-modified': [modified: boolean];
}>();

// State
const treeState = ref<XmlTreeState>({
  root: null,
  selectedNodeId: null,
  modified: false
});

const allowedContent = ref<AllowedContent | null>(null);
const isLoadingAllowedContent = ref(false);

// Panel visibility state
const showPropertiesPanel = ref(true);
const showAllowedContentPanel = ref(false);
const showXPathPanel = ref(false);

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

// Check if an element is allowed by the schema
const isElementAllowed = (elementName: string): boolean => {
  if (!allowedContent.value) {
    return true; // No schema restrictions
  }

  const element = allowedContent.value.elements.find(el => el.name === elementName);
  return element ? element.canAdd : false;
};

// Get allowed elements for the current context
const getAllowedElements = (): string[] => {
  if (!allowedContent.value) {
    return ['element']; // Default element name
  }

  return allowedContent.value.elements
    .filter(el => el.canAdd)
    .map(el => el.name);
};

// Update allowed content when selection changes
const updateAllowedContent = async () => {
  if (!props.schemaContent || !selectedNode.value) {
    allowedContent.value = null;
    return;
  }

  try {
    isLoadingAllowedContent.value = true;
    const currentXml = getCurrentXmlContent();
    const result = await xmlService.getAllowedContentAtPosition(
      currentXml,
      props.schemaContent
    );

    if (result.success && result.allowedContent) {
      allowedContent.value = result.allowedContent;
      console.log('Updated allowed content:', result.allowedContent);
    } else {
      console.warn('Failed to get allowed content:', result.error);
      allowedContent.value = null;
    }
  } catch (error) {
    console.error('Error updating allowed content:', error);
    allowedContent.value = null;
  } finally {
    isLoadingAllowedContent.value = false;
  }
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
  
  // Update allowed content when selection changes
  updateAllowedContent();
};

const toggleNode = (nodeId: string) => {
  const node = findNodeById(treeState.value.root!, nodeId);
  if (node) {
    node.expanded = !node.expanded;
  }
};

const editNode = (editData: { nodeId: string; field: string; value: any }) => {
  console.log('editNode called:', editData);
  const { nodeId, field, value } = editData;
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
  
  // Get allowed elements for the current context
  const allowedElements = getAllowedElements();
  if (allowedElements.length === 0) {
    console.warn('No elements allowed by schema for this context');
    return;
  }
  
  // Use the first allowed element name, or prompt user for selection
  const elementName = allowedElements[0];
  
  const newElement: XmlNode = {
    id: generateId(),
    type: 'element',
    name: elementName,
    children: [],
    parent: selectedNode.value.id
  };
  
  addChildNode(selectedNode.value.id, newElement);
  selectNode(newElement.id);
};

const addElementFromSchema = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  const elementName = target.value;
  
  if (!elementName || !selectedNode.value || !canHaveChildren(selectedNode.value)) {
    target.value = ''; // Reset dropdown
    return;
  }
  
  // Check if element is allowed by schema
  if (!isElementAllowed(elementName)) {
    console.warn(`Element '${elementName}' is not allowed by schema`);
    target.value = ''; // Reset dropdown
    return;
  }
  
  const newElement: XmlNode = {
    id: generateId(),
    type: 'element',
    name: elementName,
    children: [],
    parent: selectedNode.value.id
  };
  
  addChildNode(selectedNode.value.id, newElement);
  selectNode(newElement.id);
  target.value = ''; // Reset dropdown
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

const removeNode = (nodeId: string) => {
  const node = findNodeById(treeState.value.root!, nodeId);
  if (!node || node === treeState.value.root) return;
  
  const parent = findNodeById(treeState.value.root!, node.parent!);
  if (parent) {
    parent.children = parent.children.filter(child => child.id !== nodeId);
    if (treeState.value.selectedNodeId === nodeId) {
      treeState.value.selectedNodeId = null;
    }
    markModified();
  }
};

const deleteNode = () => {
  if (!selectedNode.value || isRoot.value) return;
  
  removeNode(selectedNode.value.id);
  editingNode.value = null;
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
  // Emit XML content immediately after marking as modified
  emitXmlContent();
};

// Get current XML content as string
const getCurrentXmlContent = (): string => {
  if (treeState.value.root) {
    return '<?xml version="1.0" encoding="UTF-8"?>\n' + treeToXml(treeState.value.root);
  }
  return '';
};

let emitCounter = 0;

const emitXmlContent = () => {
  if (treeState.value.root) {
    emitCounter++;
    const xmlContent = getCurrentXmlContent();
    console.log(`Emitting XML content update #${emitCounter}:`, xmlContent.substring(0, 100) + '...');
    
    emit('xml-changed', xmlContent);
    emit('tree-modified', true);
  }
};

// Note: emitXmlContent is now called directly in markModified()

// Method to update tree content from external source
const updateTreeContent = async (xmlContent: string) => {
  console.log('updateTreeContent called with:', xmlContent.substring(0, 100) + '...');
  if (xmlContent && xmlContent.trim()) {
    const parsed = await parseXmlToTree(xmlContent);
    if (parsed) {
      treeState.value.root = parsed;
      treeState.value.modified = false;
      console.log('Tree content updated successfully');
    } else {
      console.error('Failed to parse XML content for update');
    }
  }
};

// Expose methods to parent
defineExpose({
  getCurrentXmlContent,
  updateTreeContent
});

// Watch for changes to initial XML content
watch(() => props.initialXmlContent, async (newContent) => {
  if (newContent && newContent.trim()) {
    console.log('Initial XML content changed:', newContent.substring(0, 100) + '...');
    const parsed = await parseXmlToTree(newContent);
    if (parsed) {
      treeState.value.root = parsed;
      treeState.value.modified = false;
      console.log('Tree content updated from prop');
    } else {
      console.error('Failed to parse updated XML content');
    }
  }
}, { immediate: true });

// Initialize tree with initial XML content
onMounted(async () => {
  if (props.initialXmlContent && props.initialXmlContent.trim()) {
    console.log('Initializing tree with content:', props.initialXmlContent.substring(0, 100) + '...');
    const parsed = await parseXmlToTree(props.initialXmlContent);
    if (parsed) {
      treeState.value.root = parsed;
      console.log('Tree initialized successfully');
    } else {
      console.error('Failed to parse initial XML content');
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

.element-dropdown select {
  appearance: none;
  background: #007bff;
  color: white;
  border: 1px solid #007bff;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  cursor: pointer;
}

.element-dropdown select:hover:not(:disabled) {
  background: #0056b3;
  border-color: #0056b3;
}

.element-dropdown select:disabled {
  background: #6c757d;
  border-color: #6c757d;
  cursor: not-allowed;
}

.allowed-content-panel {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 1rem;
  margin: 1rem 0;
}

.allowed-content-panel h4 {
  margin: 0 0 0.5rem 0;
  color: #495057;
  font-size: 1rem;
}

.content-group {
  margin: 0.5rem 0;
}

.content-group label {
  display: block;
  font-weight: bold;
  color: #495057;
  margin-bottom: 0.25rem;
}

.allowed-elements, .allowed-attributes {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.allowed-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.5rem;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 3px;
  font-size: 0.875rem;
}

.element-name, .attribute-name {
  font-weight: bold;
  color: #007bff;
}

.occurrence-info {
  color: #6c757d;
  font-size: 0.75rem;
}

.can-add {
  color: #28a745;
  font-weight: bold;
}

.cannot-add {
  color: #dc3545;
  font-weight: bold;
}

.required {
  color: #dc3545;
  font-weight: bold;
}

.present {
  color: #28a745;
  font-weight: bold;
}

.not-present {
  color: #6c757d;
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
</style>
