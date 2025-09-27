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
        <div class="add-element-container">
          <button 
            @click="addElement" 
            class="btn btn-sm" 
            :disabled="!selectedNode || !canHaveChildren(selectedNode) || (selectedNode && getAllowedChildElements(selectedNode).length === 0)"
            :title="!selectedNode ? 'Select a node first' : !canHaveChildren(selectedNode) ? 'Only element nodes can have children' : (selectedNode && getAllowedChildElements(selectedNode).length === 0) ? 'No more elements can be added (max occurrence reached)' : 'Add new element'"
          >
            Add Element
          </button>
          <div v-if="showElementDropdown" class="element-dropdown">
            <div class="dropdown-header">Select element to add:</div>
            <div 
              v-for="(element, index) in allowedChildElements" 
              :key="index"
              @click="addSelectedElement(element)"
              class="dropdown-item"
            >
              <div class="element-info">
                <span class="element-name">{{ element.name }}</span>
                <span v-if="element.minOccurs > 0" class="required-indicator">*</span>
                <span class="current-count">({{ countChildElements(selectedNode!, element.name) }}/{{ element.maxOccurs === 'unbounded' ? '∞' : element.maxOccurs }})</span>
                <span v-if="hasRequiredContent(element)" class="auto-create-indicator">⚡</span>
              </div>
              <div class="element-details">
                <span class="element-type">{{ element.type }}</span>
                <span class="occurrence-info">{{ element.minOccurs }}..{{ element.maxOccurs }}</span>
              </div>
              <div v-if="hasRequiredContent(element)" class="auto-create-info">
                <span class="auto-create-text">Auto-creates required content</span>
              </div>
            </div>
            <div v-if="allowedChildElements.length === 0" class="dropdown-item disabled">
              <div class="element-info">
                <span class="no-elements">No more elements can be added</span>
              </div>
              <div class="element-details">
                <span class="reason">All elements have reached their maximum occurrence limit</span>
              </div>
            </div>
            <div @click="hideElementDropdown" class="dropdown-item cancel">Cancel</div>
          </div>
        </div>
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
        <div class="add-attribute-container">
          <button 
            @click="addAttribute" 
            class="btn btn-sm" 
            :disabled="!selectedNode || selectedNode.type !== 'element' || (selectedNode && getAllowedAttributes(selectedNode).length === 0)"
            :title="!selectedNode ? 'Select a node first' : selectedNode.type !== 'element' ? 'Only element nodes can have attributes' : (selectedNode && getAllowedAttributes(selectedNode).length === 0) ? 'No more attributes can be added' : 'Add attribute'"
          >
            Add Attribute
          </button>
          <div v-if="showAttributeDropdown" class="attribute-dropdown">
            <div class="dropdown-header">Select attribute to add:</div>
            <div 
              v-for="(attr, index) in allowedAttributes" 
              :key="index"
              @click="addSelectedAttribute(attr)"
              class="dropdown-item"
            >
              <div class="attribute-info">
                <span class="attribute-name">{{ attr.name }}</span>
                <span v-if="attr.use === 'required'" class="required-indicator">*</span>
                <span class="attribute-type">{{ attr.type }}</span>
              </div>
              <div v-if="attr.defaultValue" class="attribute-details">
                <span class="default-value">Default: {{ attr.defaultValue }}</span>
              </div>
            </div>
            <div v-if="allowedAttributes.length === 0" class="dropdown-item disabled">
              <div class="attribute-info">
                <span class="no-attributes">No more attributes can be added</span>
              </div>
              <div class="attribute-details">
                <span class="reason">All allowed attributes already exist</span>
              </div>
            </div>
            <div @click="hideAttributeDropdown" class="dropdown-item cancel">Cancel</div>
          </div>
        </div>
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


  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import type { XmlNode, XmlTreeState } from '@/types/xml';
import XmlTreeNode from './XmlTreeNode.vue';
import XPathQuery from './XPathQuery.vue';
import { xmlService } from '@/services/xmlService';
import type { SchemaInfo, SchemaElement, SchemaAttribute } from '@/services/api';

// Props
const props = defineProps<{
  initialXmlContent: string;
  schemaInfo?: SchemaInfo | null;
  rootElement?: SchemaElement | null;
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


// Panel visibility state
const showXPathPanel = ref(false);

// Element dropdown state
const showElementDropdown = ref(false);
const allowedChildElements = ref<SchemaElement[]>([]);

// Attribute dropdown state
const showAttributeDropdown = ref(false);
const allowedAttributes = ref<SchemaAttribute[]>([]);

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
    const parseResult = xmlService.parseXml(xmlContent);
    
    if (!parseResult.success || !parseResult.document) {
      console.error('XML parsing failed:', parseResult.error);
      throw new Error(parseResult.error || 'Invalid XML');
    }
    
    
    // Check if we have a documentElement
    if (!parseResult.document.documentElement) {
      console.error('No documentElement found in parsed document');
      throw new Error('No root element found in XML document');
    }
    
    const treeNode = domNodeToXmlNode(parseResult.document.documentElement);
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
    
    // Convert attributes to attribute nodes
    for (let i = 0; i < element.attributes.length; i++) {
      const attr = element.attributes[i];
      const attributeNode: XmlNode = {
        id: generateId(),
        type: 'attribute',
        name: attr.name,
        value: attr.value,
        children: [],
        parent: id
      };
      node.children.push(attributeNode);
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
  
  const indentStr = '  '.repeat(indent);
  
  if (node.type === 'element') {
    let xml = `${indentStr}<${node.name}`;
    
    // Add attributes from attribute nodes only
    
    // Add attributes from attribute nodes
    const attributeNodes = node.children.filter(child => child.type === 'attribute');
    for (const attrNode of attributeNodes) {
      if (attrNode.name && attrNode.value !== undefined) {
        xml += ` ${attrNode.name}="${attrNode.value}"`;
      }
    }
    
    // Filter out attribute nodes from children for content processing
    const contentChildren = node.children.filter(child => child.type !== 'attribute');
    
    if (contentChildren.length === 0) {
      xml += ' />';
    } else {
      xml += '>';
      
      // Check if all children are text nodes (for inline formatting)
      const allTextChildren = contentChildren.every(child => child.type === 'text');
      
      if (allTextChildren) {
        // Inline formatting for text content
        for (const child of contentChildren) {
          xml += treeToXml(child, 0);
        }
        xml += `</${node.name}>`;
      } else {
        // Multi-line formatting for mixed content
        xml += '\n';
        for (const child of contentChildren) {
          xml += treeToXml(child, indent + 1);
        }
        xml += `${indentStr}</${node.name}>`;
      }
    }
    return xml;
  } else if (node.type === 'text') {
    const result = node.value || '';
    return result;
  } else if (node.type === 'comment') {
    const result = `${indentStr}<!--${node.value}-->`;
    return result;
  } else if (node.type === 'cdata') {
    const result = `${indentStr}<![CDATA[${node.value}]]>`;
    return result;
  } else if (node.type === 'attribute') {
    // Attribute nodes are handled in their parent element, not as standalone nodes
    return '';
  }
  return '';
};

const selectNode = (nodeId: string) => {
  treeState.value.selectedNodeId = nodeId;
};

const toggleNode = (nodeId: string) => {
  const node = findNodeById(treeState.value.root!, nodeId);
  if (node) {
    node.expanded = !node.expanded;
  }
};

const editNode = (editData: { nodeId: string; field: string; value: any }) => {
  const { nodeId, field, value } = editData;
  const node = findNodeById(treeState.value.root!, nodeId);
  if (!node) {
    return;
  }
  
  if (field === 'remove') {
    // Remove the node (for empty text nodes)
    removeNode(nodeId);
  } else if (field === 'value') {
    // Validate value based on node type and schema
    let validationResult = { valid: true };
    
    if (node.type === 'attribute') {
      // Find the schema attribute definition
      const parentElement = findNodeById(treeState.value.root!, node.parent!);
      if (parentElement && props.schemaInfo) {
        const schemaElement = findSchemaElement(parentElement.name || '', props.schemaInfo.elements);
        if (schemaElement && schemaElement.attributes) {
          const schemaAttr = schemaElement.attributes.find(attr => attr.name === node.name);
          if (schemaAttr) {
            validationResult = validateValue(value, schemaAttr.type);
          }
        }
      }
    } else if (node.type === 'text') {
      // Find the schema element definition for text content
      const parentElement = findNodeById(treeState.value.root!, node.parent!);
      if (parentElement && props.schemaInfo) {
        const schemaElement = findSchemaElement(parentElement.name || '', props.schemaInfo.elements);
        if (schemaElement) {
          validationResult = validateValue(value, schemaElement.type);
        }
      }
    }
    
    if (!validationResult.valid) {
      // Show validation error (you could emit an event or show a toast)
      console.warn('Validation error:', (validationResult as any).error);
      // For now, we'll still allow the edit but log the warning
      // In a real app, you might want to prevent the edit or show an error message
    }
    
    // Update the node property
    (node as any)[field] = value;
  } else {
    // Update the node property
    (node as any)[field] = value;
  }
  
  // Force reactivity update
  treeState.value = { ...treeState.value };
  
  // Only call markModified - the watcher will handle emitXmlContent
  markModified();
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
  
  // Get allowed child elements based on schema
  const allowedElements = getAllowedChildElements(selectedNode.value);
  
  if (allowedElements.length === 0) {
    // No schema restrictions, use default element name
    const elementName = 'element';
    const newElement: XmlNode = {
      id: generateId(),
      type: 'element',
      name: elementName,
      children: [],
      parent: selectedNode.value.id
    };
    addChildNode(selectedNode.value.id, newElement);
    selectNode(newElement.id);
  } else if (allowedElements.length === 1) {
    // Only one allowed element, use it directly with auto-creation
    addSelectedElement(allowedElements[0]);
  } else if (allowedElements.length > 1) {
    // Multiple allowed elements, show dropdown
    allowedChildElements.value = allowedElements;
    showElementDropdown.value = true;
  }
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

const addAttribute = () => {
  if (!selectedNode.value || selectedNode.value.type !== 'element') return;
  
  // Get allowed attributes based on schema
  const allowedAttrs = getAllowedAttributes(selectedNode.value);
  
  if (allowedAttrs.length === 0) {
    // No schema restrictions, create a default attribute
    const newAttribute: XmlNode = {
      id: generateId(),
      type: 'attribute',
      name: 'newAttribute',
      value: '',
      children: [],
      parent: selectedNode.value.id
    };
    addChildNode(selectedNode.value.id, newAttribute);
    selectNode(newAttribute.id);
  } else if (allowedAttrs.length === 1) {
    // Only one allowed attribute, use it directly
    addSelectedAttribute(allowedAttrs[0]);
  } else if (allowedAttrs.length > 1) {
    // Multiple allowed attributes, show dropdown
    allowedAttributes.value = allowedAttrs;
    showAttributeDropdown.value = true;
  }
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
    
    emit('xml-changed', xmlContent);
    emit('tree-modified', true);
  }
};

// Note: emitXmlContent is now called directly in markModified()

// Method to update tree content from external source
const updateTreeContent = async (xmlContent: string) => {
  if (xmlContent && xmlContent.trim()) {
    const parsed = await parseXmlToTree(xmlContent);
    if (parsed) {
      treeState.value.root = parsed;
      treeState.value.modified = false;
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
    const parsed = await parseXmlToTree(newContent);
    if (parsed) {
      treeState.value.root = parsed;
      treeState.value.modified = false;
    } else {
      console.error('Failed to parse updated XML content');
    }
  }
}, { immediate: true });

// Initialize tree with initial XML content
onMounted(async () => {
  if (props.initialXmlContent && props.initialXmlContent.trim()) {
    const parsed = await parseXmlToTree(props.initialXmlContent);
    if (parsed) {
      treeState.value.root = parsed;
    } else {
      console.error('Failed to parse initial XML content');
    }
  }
});

// Schema-aware helper functions
const getAllowedChildElements = (parentNode: XmlNode): SchemaElement[] => {
  if (!props.schemaInfo || !props.rootElement) {
    return []; // No schema restrictions
  }
  
  // Find the schema element definition for the parent node
  const findSchemaElement = (elementName: string, schemaElements: SchemaElement[]): SchemaElement | null => {
    for (const element of schemaElements) {
      if (element.name === elementName) {
        return element;
      }
      if (element.children) {
        const found = findSchemaElement(elementName, element.children);
        if (found) return found;
      }
    }
    return null;
  };
  
  const parentSchemaElement = findSchemaElement(parentNode.name || '', props.schemaInfo.elements);
  if (!parentSchemaElement || !parentSchemaElement.children) {
    return []; // No allowed children defined
  }
  
  // Filter out elements that have reached their maximum occurrence limit
  const allowedElements = parentSchemaElement.children.filter(schemaElement => {
    const currentCount = countChildElements(parentNode, schemaElement.name);
    const maxOccurs = schemaElement.maxOccurs;
    
    if (maxOccurs === 'unbounded') {
      return true; // No limit
    }
    
    const maxCount = parseInt(maxOccurs);
    return currentCount < maxCount;
  });
  
  return allowedElements;
};

const addSelectedElement = (element: SchemaElement) => {
  if (!selectedNode.value) return;
  
  // Use unified function to create complete element with all required content
  const newElement = createElementWithRequiredContent(element, selectedNode.value.id);
  
  addChildNode(selectedNode.value.id, newElement);
  selectNode(newElement.id);
  hideElementDropdown();
};

const hideElementDropdown = () => {
  showElementDropdown.value = false;
  allowedChildElements.value = [];
};

const countChildElements = (parentNode: XmlNode, elementName: string): number => {
  if (!parentNode.children) return 0;
  
  return parentNode.children.filter(child => 
    child.type === 'element' && child.name === elementName
  ).length;
};

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

const hasRequiredContent = (element: SchemaElement): boolean => {
  // Check for required attributes
  if (element.attributes) {
    const hasRequiredAttrs = element.attributes.some(attr => attr.use === 'required');
    if (hasRequiredAttrs) return true;
  }
  
  // Check for required child elements
  if (element.children) {
    const hasRequiredChildren = element.children.some(child => child.minOccurs > 0);
    if (hasRequiredChildren) return true;
  }
  
  return false;
};

const getAllowedAttributes = (elementNode: XmlNode): SchemaAttribute[] => {
  if (!props.schemaInfo || !props.rootElement) {
    return []; // No schema restrictions
  }
  
  // Find the schema element definition for the current element
  const findSchemaElement = (elementName: string, schemaElements: SchemaElement[]): SchemaElement | null => {
    for (const element of schemaElements) {
      if (element.name === elementName) {
        return element;
      }
      if (element.children) {
        const found = findSchemaElement(elementName, element.children);
        if (found) return found;
      }
    }
    return null;
  };
  
  const schemaElement = findSchemaElement(elementNode.name || '', props.schemaInfo.elements);
  if (!schemaElement || !schemaElement.attributes) {
    return []; // No attributes defined for this element
  }
  
  // Get existing attribute names
  const existingAttributeNames = new Set(
    elementNode.children
      .filter(child => child.type === 'attribute')
      .map(attr => attr.name)
      .filter(Boolean)
  );
  
  // Filter out attributes that already exist
  return schemaElement.attributes.filter(attr => !existingAttributeNames.has(attr.name));
};

const addSelectedAttribute = (attr: SchemaAttribute) => {
  if (!selectedNode.value) return;
  
  const newAttribute: XmlNode = {
    id: generateId(),
    type: 'attribute',
    name: attr.name,
    value: getDefaultAttributeValue(attr),
    children: [],
    parent: selectedNode.value.id
  };
  
  addChildNode(selectedNode.value.id, newAttribute);
  selectNode(newAttribute.id);
  hideAttributeDropdown();
};

const hideAttributeDropdown = () => {
  showAttributeDropdown.value = false;
  allowedAttributes.value = [];
};

const getDefaultTextContent = (element: SchemaElement): string => {
  // Use default value if specified in schema
  if (element.defaultValue) {
    return element.defaultValue;
  }
  
  // Use fixed value if specified in schema
  if (element.fixedValue) {
    return element.fixedValue;
  }
  
  // Generate type-based defaults for text content
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
  
  // For date fields, provide type-based defaults only if no schema default
  if (type.includes('date')) {
    return new Date().toISOString().split('T')[0];
  }
  
  if (type.includes('year')) {
    return new Date().getFullYear().toString();
  }
  
  // For strings and other types, return empty string
  return '';
};

// Unified function to create a complete element with all required content
const createElementWithRequiredContent = (schemaElement: SchemaElement, parentId: string): XmlNode => {
  const element: XmlNode = {
    id: generateId(),
    type: 'element',
    name: schemaElement.name,
    value: '',
    parent: parentId,
    children: []
  };
  
  // Add required attributes
  if (schemaElement.attributes) {
    for (const attr of schemaElement.attributes) {
      if (attr.use === 'required') {
        const attrNode: XmlNode = {
          id: generateId(),
          type: 'attribute',
          name: attr.name,
          value: getDefaultAttributeValue(attr),
          parent: element.id,
          children: []
        };
        element.children.push(attrNode);
      }
    }
  }
  
  // Add text content for simple types
  if (schemaElement.type && !schemaElement.children) {
    const textContent = getDefaultTextContent(schemaElement);
    const textNode: XmlNode = {
      id: generateId(),
      type: 'text',
      value: textContent,
      parent: element.id,
      children: []
    };
    element.children.push(textNode);
  }
  
  // Add required child elements
  if (schemaElement.children) {
    for (const childSchema of schemaElement.children) {
      if (childSchema.minOccurs > 0) {
        for (let i = 0; i < childSchema.minOccurs; i++) {
          const childElement = createElementWithRequiredContent(childSchema, element.id);
          element.children.push(childElement);
        }
      }
    }
  }
  
  return element;
};


// Type validation functions
// Helper function to find schema element
const findSchemaElement = (elementName: string, schemaElements: SchemaElement[]): SchemaElement | null => {
  for (const element of schemaElements) {
    if (element.name === elementName) {
      return element;
    }
    if (element.children) {
      const found = findSchemaElement(elementName, element.children);
      if (found) return found;
    }
  }
  return null;
};

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

// Click outside handler to close dropdowns
const handleClickOutside = (event: Event) => {
  const target = event.target as HTMLElement;
  if (!target.closest('.add-element-container')) {
    hideElementDropdown();
  }
  if (!target.closest('.add-attribute-container')) {
    hideAttributeDropdown();
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
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

.add-element-container {
  position: relative;
  display: inline-block;
}

.element-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 1000;
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  min-width: 200px;
  max-width: 300px;
  margin-top: 2px;
}

.dropdown-header {
  padding: 0.5rem 0.75rem;
  background: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  font-size: 0.875rem;
  font-weight: 600;
  color: #495057;
}

.dropdown-item {
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background: #f8f9fa;
}

.dropdown-item.cancel {
  border-top: 1px solid #dee2e6;
  color: #6c757d;
  font-style: italic;
  text-align: center;
}

.dropdown-item.disabled {
  cursor: not-allowed;
  opacity: 0.6;
  background: #f8f9fa;
}

.dropdown-item.disabled:hover {
  background: #f8f9fa;
}

.no-elements {
  color: #6c757d;
  font-style: italic;
}

.reason {
  font-size: 0.75rem;
  color: #6c757d;
  font-style: italic;
}

.element-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.element-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
  color: #6c757d;
}

.element-name {
  font-weight: 600;
  color: #007bff;
  font-size: 0.875rem;
}

.required-indicator {
  color: #dc3545;
  font-weight: bold;
  font-size: 0.875rem;
}

.element-type {
  font-size: 0.75rem;
  color: #6c757d;
}

.occurrence-info {
  font-size: 0.75rem;
  color: #6c757d;
  font-family: monospace;
}

.current-count {
  font-size: 0.75rem;
  color: #6c757d;
  font-family: monospace;
  margin-left: 0.5rem;
}

.auto-create-indicator {
  font-size: 0.8rem;
  color: #28a745;
  margin-left: 0.5rem;
  font-weight: bold;
}

.auto-create-info {
  margin-top: 0.25rem;
  padding-left: 1rem;
}

.auto-create-text {
  font-size: 0.7rem;
  color: #28a745;
  font-style: italic;
}

.add-attribute-container {
  position: relative;
  display: inline-block;
}

.attribute-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 1000;
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  min-width: 250px;
  max-height: 300px;
  overflow-y: auto;
}

.attribute-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.attribute-name {
  font-weight: bold;
  color: #2c3e50;
}

.attribute-type {
  font-size: 0.75rem;
  color: #6c757d;
  font-family: monospace;
}

.attribute-details {
  margin-top: 0.25rem;
  padding-left: 1rem;
}

.default-value {
  font-size: 0.7rem;
  color: #6c757d;
  font-style: italic;
}

.no-attributes {
  color: #6c757d;
  font-style: italic;
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
