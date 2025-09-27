<!--
  XML Tree Node - Individual Node Component for Tree Editor
  
  This component represents a single XML node in the tree view and provides:
  - Visual representation of different node types (element, text, comment, etc.)
  - Interactive node selection and expansion/collapse
  - Inline editing of node properties (name, value, attributes)
  - Context-aware UI based on node type and selection state
  - Event emission for parent component communication
-->
<template>
  <div class="xml-tree-node">
    <div 
      class="node-line"
      :class="{ 
        'selected': isSelected,
        'expanded': node.expanded,
        'collapsed': !node.expanded && node.children.length > 0 && !isSimpleElement
      }"
      @click="handleNodeClick"
    >
      <div class="node-content">
        <span 
          v-if="node.children.length > 0 && !isSimpleElement" 
          class="expand-toggle"
          @click.stop="toggleNode"
        >
          {{ node.expanded ? '▼' : '▶' }}
        </span>
        <span v-else class="expand-spacer"></span>
        
        <span class="node-icon" :class="node.type">
          {{ getNodeIcon(node.type) }}
        </span>
        
        <span 
          v-if="node.type === 'element' && !isSimpleElement" 
          class="node-name"
        >
          &lt;{{ node.name }}{{ node.children.length === 0 ? ' /' : '' }}&gt;
        </span>
        
        <span 
          v-else-if="node.type === 'element' && isSimpleElement && !isEditing" 
          class="node-value editable simple-element"
          @click.stop="startInlineEdit"
          :title="'Click to edit: ' + getSimpleElementValue()"
        >
          &lt;{{ node.name }}&gt;{{ getSimpleElementValue() }}&lt;/{{ node.name }}&gt;
        </span>
        
        <div 
          v-else-if="node.type === 'element' && isSimpleElement && isEditing"
          class="simple-element-edit"
          @click.stop
        >
          <span class="element-name">&lt;{{ node.name }}&gt;</span>
          <input 
            v-model="editingValue"
            @blur="finishInlineEdit"
            @keydown.enter.prevent="finishInlineEdit"
            @keydown.escape.prevent="cancelInlineEdit"
            class="element-value-input"
            ref="editInput"
            :title="'Press Enter to save, Escape to cancel'"
          />
          <span class="element-name">&lt;/{{ node.name }}&gt;</span>
        </div>
        
        <span 
          v-else-if="node.type === 'attribute' && !isEditing" 
          class="node-value editable"
          @click.stop="startInlineEdit"
          :title="getAttributeTitle()"
        >
          {{ node.name }}="{{ node.value }}"
        </span>
        
        <div 
          v-else-if="node.type === 'attribute' && isEditing"
          class="attribute-edit"
          :class="{ 'validation-error': validationError }"
          @click.stop
        >
          <span class="attribute-name-display">{{ node.name }}</span>
          <span class="attribute-equals">=</span>
          <input 
            v-model="editingValue"
            @blur="finishAttributeEdit"
            @keydown.enter.prevent="finishAttributeEdit"
            @keydown.escape.prevent="cancelInlineEdit"
            @input="validateCurrentValue"
            class="attribute-value-input"
            :class="{ 'invalid': validationError }"
            ref="valueInput"
            placeholder="attribute value"
            :title="validationError || 'Press Enter to save, Escape to cancel'"
          />
          <span v-if="validationError" class="validation-error-message">{{ validationError }}</span>
        </div>
        
        <span 
          v-else-if="node.type === 'text' && !isEditing" 
          class="node-value editable"
          @click.stop="startInlineEdit"
          :title="'Click to edit: ' + node.value"
        >
          "{{ node.value }}"
        </span>
        
        <input 
          v-else-if="node.type === 'text' && isEditing"
          v-model="editingValue"
          @blur="finishInlineEdit"
          @keydown.enter.prevent="finishInlineEdit"
          @keydown.escape.prevent="cancelInlineEdit"
          class="node-value-input"
          ref="editInput"
          :title="'Press Enter to save, Escape to cancel'"
        />
        
        <span 
          v-else-if="node.type === 'comment' && !isEditing" 
          class="node-value editable"
          @click.stop="startInlineEdit"
          :title="'Click to edit: ' + node.value"
        >
          &lt;!--{{ node.value }}--&gt;
        </span>
        
        <input 
          v-else-if="node.type === 'comment' && isEditing"
          v-model="editingValue"
          @blur="finishInlineEdit"
          @keydown.enter.prevent="finishInlineEdit"
          @keydown.escape.prevent="cancelInlineEdit"
          class="node-value-input"
          ref="editInput"
          :title="'Press Enter to save, Escape to cancel'"
        />
        
        <span 
          v-else-if="node.type === 'cdata' && !isEditing" 
          class="node-value editable"
          @click.stop="startInlineEdit"
          :title="'Click to edit: ' + node.value"
        >
          &lt;![CDATA[{{ node.value }}]]&gt;
        </span>
        
        <input 
          v-else-if="node.type === 'cdata' && isEditing"
          v-model="editingValue"
          @blur="finishInlineEdit"
          @keydown.enter.prevent="finishInlineEdit"
          @keydown.escape.prevent="cancelInlineEdit"
          class="node-value-input"
          ref="editInput"
          :title="'Press Enter to save, Escape to cancel'"
        />
        
        <span class="node-value" v-else>
          {{ node.value }}
        </span>
      </div>
    </div>
    
    <div v-if="node.expanded && node.children.length > 0 && !isSimpleElement" class="node-children">
      <XmlTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :selected-id="selectedId"
        :schema-info="schemaInfo"
        @select="$emit('select', $event)"
        @toggle="$emit('toggle', $event)"
        @edit="$emit('edit', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, nextTick, watch } from 'vue';
import type { XmlNode } from '@/types/xml';

// Props
const props = defineProps<{
  node: XmlNode;
  selectedId: string | null;
  schemaInfo?: any;
}>();

// Emits
const emit = defineEmits<{
  'select': [nodeId: string];
  'toggle': [nodeId: string];
  'edit': [editData: { nodeId: string; field: string; value: any }];
}>();

// Reactive state for inline editing
const isEditing = ref(false);
const editingValue = ref('');
const validationError = ref<string | null>(null);
const editInput = ref<HTMLInputElement | null>(null);
const valueInput = ref<HTMLInputElement | null>(null);

// Watch for external changes to node value
watch(() => props.node.value, (newValue) => {
  if (!isEditing.value) {
    // Only update editingValue if we're not currently editing
    editingValue.value = newValue || '';
  }
});

// Computed
const isSelected = computed(() => {
  return props.selectedId === props.node.id;
});

const isSimpleElement = computed(() => {
  if (props.node.type !== 'element') return false;
  
  // Check if all children are either text nodes or attribute nodes
  const nonAttributeChildren = props.node.children.filter(child => child.type !== 'attribute');
  const hasOnlyTextChild = nonAttributeChildren.length === 1 && nonAttributeChildren[0].type === 'text';
  
  // If it already has only text content, it's simple
  if (hasOnlyTextChild) return true;
  
  // If no children yet, check if it should be simple based on schema
  if (nonAttributeChildren.length === 0 && props.schemaInfo) {
    // Find the schema element definition
    const schemaElement = findSchemaElement(props.node.name || '', props.schemaInfo.elements);
    if (schemaElement) {
      // It's simple if it has a type but no children in the schema
      return schemaElement.type && !schemaElement.children;
    }
  }
  
  return false;
});

const getSimpleElementValue = (): string => {
  if (!isSimpleElement.value) return '';
  const textChild = props.node.children.find(child => child.type === 'text');
  return textChild?.value || '';
};

// Helper function to find schema element definition
const findSchemaElement = (elementName: string, elements: any[]): any => {
  for (const element of elements) {
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

// Methods
const handleNodeClick = () => {
  emit('select', props.node.id);
};

const toggleNode = () => {
  emit('toggle', props.node.id);
};

const getNodeIcon = (type: XmlNode['type']): string => {
  switch (type) {
    case 'element':
      return '📄';
    case 'text':
      return '📝';
    case 'comment':
      return '💬';
    case 'cdata':
      return '📋';
    case 'processing-instruction':
      return '⚙️';
    case 'attribute':
      return '🏷️';
    default:
      return '❓';
  }
};

const getAttributeTitle = (): string => {
  if (props.node.type === 'attribute') {
    return `Click to edit: ${props.node.name}="${props.node.value}"`;
  }
  return '';
};

// Validation function (simplified version for real-time feedback)
const validateCurrentValue = () => {
  if (props.node.type === 'attribute') {
    const value = editingValue.value;
    const type = getAttributeType(); // We'll need to get this from props or context
    
    // Basic validation for common types
    if (type && type.toLowerCase().includes('boolean')) {
      const lowerValue = value.toLowerCase();
      if (value && !['true', 'false', '1', '0'].includes(lowerValue)) {
        validationError.value = 'Boolean values must be "true", "false", "1", or "0"';
        return;
      }
    } else if (type && type.toLowerCase().includes('int') && !type.toLowerCase().includes('string')) {
      if (value && !/^-?\d+$/.test(value)) {
        validationError.value = 'Integer values must contain only digits';
        return;
      }
    } else if (type && (type.toLowerCase().includes('decimal') || type.toLowerCase().includes('float'))) {
      if (value && !/^-?\d+(\.\d+)?$/.test(value)) {
        validationError.value = 'Decimal values must be valid numbers';
        return;
      }
    } else if (type && type.toLowerCase().includes('date')) {
      if (value && !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        validationError.value = 'Date values must be in YYYY-MM-DD format';
        return;
      }
    } else if (type && type.toLowerCase().includes('year')) {
      if (value && !/^\d{4}$/.test(value)) {
        validationError.value = 'Year values must be 4-digit years';
        return;
      }
    }
    
    validationError.value = null;
  }
};

// Helper to get attribute type (simplified - in a real app you'd pass this as a prop)
const getAttributeType = (): string => {
  // This is a simplified version - in practice you'd get this from the schema
  // For now, we'll return a basic type based on common patterns
  const name = props.node.name?.toLowerCase() || '';
  if (name.includes('id')) return 'xs:ID';
  if (name.includes('available') || name.includes('enabled')) return 'xs:boolean';
  if (name.includes('count') || name.includes('number')) return 'xs:integer';
  if (name.includes('price') || name.includes('rate')) return 'xs:decimal';
  if (name.includes('date') || name.includes('time')) return 'xs:date';
  if (name.includes('year')) return 'xs:gYear';
  return 'xs:string';
};


// Inline editing methods
const startInlineEdit = async () => {
  if (!['text', 'comment', 'cdata', 'attribute'].includes(props.node.type) && !isSimpleElement.value) return;
  
  isEditing.value = true;
  
  if (isSimpleElement.value) {
    editingValue.value = getSimpleElementValue();
  } else {
    editingValue.value = props.node.value || '';
  }
  
  validationError.value = null;
  
  await nextTick();
  const inputToFocus = props.node.type === 'attribute' ? valueInput.value : editInput.value;
  if (inputToFocus) {
    inputToFocus.focus();
    inputToFocus.select();
  }
};

const finishInlineEdit = () => {
  if (!isEditing.value) return;
  
  const newValue = editingValue.value.trim();
  
  if (isSimpleElement.value) {
    // For simple elements, emit a special edit that creates text child if needed
    emit('edit', { nodeId: props.node.id, field: 'simpleElementValue', value: newValue });
  } else {
    // For text nodes, remove if empty; for comments and CDATA, keep even if empty
    if (props.node.type === 'text' && newValue === '') {
      emit('edit', { nodeId: props.node.id, field: 'remove', value: null });
    } else {
      emit('edit', { nodeId: props.node.id, field: 'value', value: newValue });
    }
  }
  
  isEditing.value = false;
  editingValue.value = '';
};

const finishAttributeEdit = () => {
  if (!isEditing.value || props.node.type !== 'attribute') return;
  
  // Don't save if there's a validation error
  if (validationError.value) {
    return;
  }
  
  const newValue = editingValue.value.trim();
  
  if (newValue !== props.node.value) {
    emit('edit', {
      nodeId: props.node.id,
      field: 'value',
      value: newValue
    });
  }
  
  isEditing.value = false;
  editingValue.value = '';
  validationError.value = null;
};

const cancelInlineEdit = () => {
  isEditing.value = false;
  editingValue.value = '';
  validationError.value = null;
};
</script>

<style scoped>
.xml-tree-node {
  user-select: none;
}

.node-line {
  display: flex;
  align-items: center;
  padding: 0.25rem 0;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
  position: relative;
}

.node-line:hover {
  background-color: #f8f9fa;
}

.node-line.selected {
  background-color: #e3f2fd;
  border-left: 3px solid #3498db;
  padding-left: 0.5rem;
}

.node-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 0;
}

.expand-toggle, .expand-spacer {
  width: 16px;
  text-align: center;
  font-size: 12px;
  color: #6c757d;
  cursor: pointer;
  flex-shrink: 0;
}

.expand-toggle:hover {
  color: #3498db;
}

.node-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.node-name {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  color: #2c3e50;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-value {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  color: #6c757d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-style: italic;
}

.node-value.editable {
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 3px;
  transition: background-color 0.2s;
}

.node-value.editable:hover {
  background-color: #f8f9fa;
  border: 1px dashed #dee2e6;
}

.node-value-input {
  background: white;
  border: 2px solid #3498db;
  border-radius: 3px;
  padding: 2px 4px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  color: #2c3e50;
  outline: none;
  min-width: 100px;
  max-width: 300px;
}

.node-value-input:focus {
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.attribute-edit {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #f8f9fa;
  border: 2px solid #3498db;
  border-radius: 3px;
  padding: 2px 4px;
}

.attribute-value-input {
  background: transparent;
  border: none;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  color: #2c3e50;
  outline: none;
  padding: 2px 4px;
  border-radius: 2px;
}

.attribute-name-display {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  font-weight: bold;
  color: #2c3e50;
  padding: 2px 4px;
}

.attribute-value-input {
  min-width: 100px;
}

.attribute-equals {
  color: #6c757d;
  font-weight: bold;
}

.validation-error {
  border-color: #dc3545 !important;
}

.attribute-value-input.invalid {
  border-color: #dc3545;
  background-color: #fff5f5;
}

.validation-error-message {
  display: block;
  font-size: 0.75rem;
  color: #dc3545;
  margin-top: 0.25rem;
  font-style: italic;
}

.node-children {
  margin-left: 20px;
  border-left: 1px solid #e9ecef;
  padding-left: 8px;
}

/* Node type specific styling */
.node-icon.element {
  color: #3498db;
}

.node-icon.text {
  color: #27ae60;
}

.node-icon.comment {
  color: #f39c12;
}

.node-icon.cdata {
  color: #9b59b6;
}

.node-icon.processing-instruction {
  color: #e74c3c;
}

/* Simple element styling */
.simple-element {
  color: #2c3e50;
  font-family: 'Courier New', monospace;
  background-color: #f8f9fa;
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid #e9ecef;
  cursor: pointer;
  transition: all 0.2s ease;
}

.simple-element:hover {
  background-color: #e9ecef;
  border-color: #3498db;
}

.simple-element-edit {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  background-color: #fff;
  border: 2px solid #3498db;
  border-radius: 3px;
  padding: 2px 4px;
  font-family: 'Courier New', monospace;
}

.element-name {
  color: #3498db;
  font-weight: bold;
}

.element-value-input {
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: inherit;
  color: #2c3e50;
  min-width: 50px;
  max-width: 200px;
}

.element-value-input:focus {
  background-color: #f8f9fa;
  border-radius: 2px;
}

/* Hover effects */
.node-line:hover .node-name {
  color: #3498db;
}

.node-line:hover .node-value {
  color: #2c3e50;
}
</style>
