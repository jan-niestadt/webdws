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
  <div class="form-node" @contextmenu.prevent="showContextMenu">
    <!-- Context Menu -->
    <div v-if="showMenu" class="context-menu" :style="menuStyle" @click.stop>
      <div class="context-menu-item" @click="deleteNode">
        <span class="context-menu-icon">🗑️</span>
        Delete
      </div>
    </div>

    <!-- Element Section -->
    <div v-if="node.type === 'element'" class="form-section">
      <!-- Simple Element (single input field) -->
      <div v-if="isSimpleElement" class="form-field-inline">
        <label class="field-label-inline">{{ getElementDisplayName() }}</label>
        <div class="field-input-container">
          <!-- Boolean field as checkbox -->
          <div v-if="isBooleanField && !isEditing" class="checkbox-container">
            <input 
              type="checkbox"
              :checked="getBooleanValue()"
              @change="handleBooleanChange"
              class="form-checkbox"
            />
            <span class="checkbox-label">{{ getBooleanValue() ? 'Yes' : 'No' }}</span>
          </div>
          <!-- Regular input field -->
          <input 
            v-else-if="!isEditing"
            :value="getSimpleElementValue()"
            @click="startInlineEdit"
            class="form-input field-input"
            :placeholder="getPlaceholder()"
            readonly
          />
          <input 
            v-else
            v-model="editingValue"
            @blur="finishInlineEdit"
            @keydown.enter.prevent="finishInlineEdit"
            @keydown.escape.prevent="cancelInlineEdit"
            class="form-input field-input"
            ref="editInput"
            :placeholder="getPlaceholder()"
          />
        </div>
      </div>

      <!-- Complex Element (with children) -->
      <div v-else class="form-element-section">
        <div class="element-header" @click="handleNodeClick">
          <span class="element-icon">📄</span>
          <span class="element-name">{{ getElementDisplayName() }}</span>
          <button 
            v-if="node.children.length > 0"
            @click.stop="toggleNode"
            class="btn-toggle"
            :class="{ 'expanded': node.expanded }"
          >
            {{ node.expanded ? '▼' : '▶' }}
          </button>
        </div>
        <div v-if="node.expanded" class="form-children">
          <XmlTreeNode
            v-for="child in node.children"
            :key="child.id"
            :node="child"
            :selected-id="selectedId"
            :schema-info="schemaInfo"
            @select="$emit('select', $event)"
            @toggle="$emit('toggle', $event)"
            @edit="$emit('edit', $event)"
            @delete="$emit('delete', $event)"
            @add-element="$emit('add-element', $event)"
            @add-attribute="$emit('add-attribute', $event)"
          />
          
                  <!-- Add buttons for repeatable child elements and optional attributes -->
                  <div v-if="addButtons.length > 0" class="add-element-button-container">
                    <button 
                      v-for="button in addButtons" 
                      :key="`${button.type}-${button.name}`"
                      @click="addChild(button)" 
                      class="btn-add-element"
                    >
                      <span class="btn-icon">{{ button.type === 'attribute' ? '🏷️' : '➕' }}</span>
                      Add {{ button.displayName }}
                    </button>
                  </div>
        </div>
      </div>
    </div>

    <!-- Attribute Field -->
    <div v-else-if="node.type === 'attribute'" class="form-field-inline">
      <label class="field-label-inline">{{ getAttributeDisplayName() }}</label>
      <div class="field-input-container">
        <!-- Boolean attribute as checkbox -->
        <div v-if="isBooleanAttribute && !isEditing" class="checkbox-container">
          <input 
            type="checkbox"
            :checked="getBooleanAttributeValue()"
            @change="handleBooleanAttributeChange"
            class="form-checkbox"
          />
          <span class="checkbox-label">{{ getBooleanAttributeValue() ? 'Yes' : 'No' }}</span>
        </div>
        <!-- Regular attribute input -->
        <input 
          v-else-if="!isEditing"
          :value="node.value"
          @click="startInlineEdit"
          class="form-input field-input"
          :placeholder="getAttributePlaceholder()"
          readonly
        />
        <div v-else class="field-input-container" @click.stop>
          <input 
            v-model="editingValue"
            @blur="finishAttributeEdit"
            @keydown.enter.prevent="finishAttributeEdit"
            @keydown.escape.prevent="cancelInlineEdit"
            @input="validateCurrentValue"
            class="form-input field-input"
            :class="{ 'invalid': validationError }"
            ref="valueInput"
            :placeholder="getAttributePlaceholder()"
          />
          <div v-if="validationError" class="field-error-container">
            <span class="field-error-icon">⚠️</span>
            <span class="field-error">{{ validationError }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Text Content Field -->
    <div v-else-if="node.type === 'text'" class="form-field-inline">
      <label class="field-label-inline">Text Content</label>
      <div class="field-input-container">
        <textarea 
          v-if="!isEditing"
          :value="node.value"
          @click="startInlineEdit"
          class="form-textarea field-input readonly"
          placeholder="Enter text content..."
          readonly
        />
        <textarea 
          v-else
          v-model="editingValue"
          @blur="finishInlineEdit"
          @keydown.escape.prevent="cancelInlineEdit"
          class="form-textarea field-input"
          ref="editInput"
          placeholder="Enter text content..."
        />
      </div>
    </div>

    <!-- Comment Field -->
    <div v-else-if="node.type === 'comment'" class="form-field-inline">
      <label class="field-label-inline">Comment</label>
      <div class="field-input-container">
        <textarea 
          v-if="!isEditing"
          :value="node.value"
          @click="startInlineEdit"
          class="form-textarea field-input readonly"
          placeholder="Enter comment..."
          readonly
        />
        <textarea 
          v-else
          v-model="editingValue"
          @blur="finishInlineEdit"
          @keydown.escape.prevent="cancelInlineEdit"
          class="form-textarea field-input"
          ref="editInput"
          placeholder="Enter comment..."
        />
      </div>
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
  'delete': [nodeId: string];
  'add-element': [data: { parentId: string; elementName: string }];
  'add-attribute': [data: { parentId: string; attributeName: string }];
}>();

// Reactive state for inline editing
const isEditing = ref(false);
const editingValue = ref('');
const validationError = ref<string | null>(null);
const editInput = ref<HTMLInputElement | null>(null);
const valueInput = ref<HTMLInputElement | null>(null);

// Context menu state
const showMenu = ref(false);
const menuStyle = ref({});

// Watch for external changes to node value
watch(() => props.node.value, (newValue) => {
  if (!isEditing.value) {
    // Only update editingValue if we're not currently editing
    editingValue.value = newValue || '';
  }
});


// Computed
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

// Get the add buttons that should be shown for this element
const addButtons = computed(() => {
  if (props.node.type !== 'element' || !props.schemaInfo) return [];
  
  // Find the schema element definition for this element
  const schemaElement = findSchemaElement(props.node.name || '', props.schemaInfo.elements);
  if (!schemaElement) return [];
  
  const buttons = [];
  
  // Explicitly reference children to ensure reactivity
  const children = props.node.children;
  
  
  // Check child elements
  if (schemaElement.children) {
    for (const childSchema of schemaElement.children) {
      // Count existing children of this type
      const existingCount = children.filter(child => 
        child.type === 'element' && child.name === childSchema.name
      ).length;
      
      // Show add button if:
      // 1. It's a repeatable element (maxOccurs > 1 or unbounded) and we haven't reached the limit
      // 2. It's a required element (minOccurs > 0) and we don't have the minimum required
      const isRepeatable = childSchema.maxOccurs === 'unbounded' || parseInt(childSchema.maxOccurs) > 1;
      const isRequired = childSchema.minOccurs > 0;
      const needsMore = isRepeatable ? 
        (childSchema.maxOccurs === 'unbounded' || existingCount < parseInt(childSchema.maxOccurs)) :
        (isRequired && existingCount < childSchema.minOccurs);
      
      if (needsMore) {
        buttons.push({
          type: 'element',
          name: childSchema.name,
          displayName: childSchema.name.charAt(0).toUpperCase() + childSchema.name.slice(1)
        });
      }
    }
  }
  
  // Check attributes (both required and optional)
  if (schemaElement.attributes) {
    for (const attrSchema of schemaElement.attributes) {
      // Check if this attribute already exists
      const existingAttribute = children.find(child => 
        child.type === 'attribute' && child.name === attrSchema.name
      );
      
      // Show add button if the attribute doesn't exist (regardless of whether it's required or optional)
      if (!existingAttribute) {
        buttons.push({
          type: 'attribute',
          name: attrSchema.name,
          displayName: attrSchema.name.charAt(0).toUpperCase() + attrSchema.name.slice(1)
        });
      }
    }
  }
  return buttons;
});

const isBooleanField = computed(() => {
  if (props.node.type !== 'element' || !props.schemaInfo) return false;
  
  const schemaElement = findSchemaElement(props.node.name || '', props.schemaInfo.elements);
  if (!schemaElement) return false;
  
  return schemaElement.type && schemaElement.type.toLowerCase().includes('boolean');
});

const isBooleanAttribute = computed(() => {
  if (props.node.type !== 'attribute' || !props.schemaInfo) return false;
  
  // Find the parent element's schema
  const parentElement = findParentElementSchema();
  if (!parentElement) return false;
  
  // Find the attribute in the parent's attributes
  const attribute = parentElement.attributes?.find((attr: any) => attr.name === props.node.name);
  if (!attribute) return false;
  
  return attribute.type && attribute.type.toLowerCase().includes('boolean');
});

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

// Helper function to find parent element schema
const findParentElementSchema = (): any => {
  if (!props.schemaInfo) return null;
  
  // This is a simplified approach - we need to find the parent element's schema
  // For now, we'll look for the root element that contains this attribute
  const rootElement = props.schemaInfo.elements[0];
  if (!rootElement) return null;
  
  // Check if this attribute can be found in the root element's attributes
  const findInChildren = (element: any): any => {
    if (element.attributes) {
      for (const attr of element.attributes) {
        if (attr.name === props.node.name) {
          return element;
        }
      }
    }
    if (element.children) {
      for (const child of element.children) {
        const found = findInChildren(child);
        if (found) return found;
      }
    }
    return null;
  };
  
  return findInChildren(rootElement);
};



// Form display helper methods
const getElementDisplayName = (): string => {
  if (props.node.type !== 'element') return '';
  
  // Convert element name to a more user-friendly display name
  const name = props.node.name || '';
  return name
    .replace(/([A-Z])/g, ' $1') // Add space before capital letters
    .replace(/^./, str => str.toUpperCase()) // Capitalize first letter
    .replace(/_/g, ' ') // Replace underscores with spaces
    .trim();
};

const getAttributeDisplayName = (): string => {
  if (props.node.type !== 'attribute') return '';
  
  const name = props.node.name || '';
  return name
    .replace(/([A-Z])/g, ' $1') // Add space before capital letters
    .replace(/^./, str => str.toUpperCase()) // Capitalize first letter
    .replace(/_/g, ' ') // Replace underscores with spaces
    .trim();
};


const getPlaceholder = (): string => {
  if (props.node.type !== 'element') return '';
  
  const schemaElement = findSchemaElement(props.node.name || '', props.schemaInfo?.elements || []);
  if (schemaElement?.type) {
    const type = schemaElement.type.toLowerCase();
    if (type.includes('string')) return 'Enter text...';
    if (type.includes('int') || type.includes('number')) return 'Enter number...';
    if (type.includes('date')) return 'YYYY-MM-DD';
    if (type.includes('boolean')) return 'true or false';
    if (type.includes('email')) return 'user@example.com';
  }
  return 'Enter value...';
};

const getAttributePlaceholder = (): string => {
  if (props.node.type !== 'attribute') return '';
  
  const type = getAttributeType().toLowerCase();
  if (type.includes('string')) return 'Enter text...';
  if (type.includes('int') || type.includes('number')) return 'Enter number...';
  if (type.includes('date')) return 'YYYY-MM-DD';
  if (type.includes('boolean')) return 'true or false';
  if (type.includes('email')) return 'user@example.com';
  return 'Enter value...';
};

// Methods
const handleNodeClick = () => {
  emit('select', props.node.id);
};

const toggleNode = () => {
  emit('toggle', props.node.id);
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

// Context menu methods
const showContextMenu = (event: MouseEvent) => {
  event.preventDefault();
  showMenu.value = true;
  menuStyle.value = {
    position: 'fixed',
    left: `${event.clientX}px`,
    top: `${event.clientY}px`,
    zIndex: 1000
  };
  
  // Hide menu when clicking elsewhere
  const hideMenu = () => {
    showMenu.value = false;
    document.removeEventListener('click', hideMenu);
  };
  
  setTimeout(() => {
    document.addEventListener('click', hideMenu);
  }, 0);
};

const deleteNode = () => {
  emit('delete', props.node.id);
  showMenu.value = false;
};

// Add child element or attribute method
const addChild = (button: { type: string; name: string }) => {
  if (button.type === 'element') {
    emit('add-element', {
      parentId: props.node.id,
      elementName: button.name
    });
  } else if (button.type === 'attribute') {
    emit('add-attribute', {
      parentId: props.node.id,
      attributeName: button.name
    });
  }
};

// Boolean field methods
const getBooleanValue = (): boolean => {
  const value = getSimpleElementValue().toLowerCase();
  return value === 'true' || value === '1';
};

const handleBooleanChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const newValue = target.checked ? 'true' : 'false';
  
  // Update the text child with the new boolean value
  let textChild = props.node.children.find(child => child.type === 'text');
  if (!textChild) {
    // Create text child if it doesn't exist
    textChild = {
      id: Math.random().toString(36).substr(2, 9),
      type: 'text',
      value: newValue,
      parent: props.node.id,
      children: []
    };
    props.node.children.push(textChild);
  } else {
    textChild.value = newValue;
  }
  
  // Emit the change
  emit('edit', { nodeId: props.node.id, field: 'simpleElementValue', value: newValue });
};

// Boolean attribute methods
const getBooleanAttributeValue = (): boolean => {
  const value = props.node.value?.toLowerCase() || '';
  return value === 'true' || value === '1';
};

const handleBooleanAttributeChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const newValue = target.checked ? 'true' : 'false';
  
  // Emit the change for the attribute
  emit('edit', { nodeId: props.node.id, field: 'value', value: newValue });
};
</script>

<style scoped>
.form-node {
  margin-bottom: 0.5rem;
  position: relative;
}

.context-menu {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 0.25rem 0;
  min-width: 120px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  font-size: 0.9rem;
  color: #2c3e50;
  transition: background-color 0.2s;
}

.context-menu-item:hover {
  background: #f8f9fa;
}

.context-menu-icon {
  font-size: 0.8rem;
}

.add-element-button-container {
  padding: 0.5rem;
  text-align: center;
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
}

.btn-add-element {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add-element:hover {
  background: #2980b9;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.btn-icon {
  font-size: 0.8rem;
}

.checkbox-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.checkbox-container:hover {
  border-color: #3498db;
  background: #f8f9fa;
}

.form-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #3498db;
}

.checkbox-label {
  font-size: 0.9rem;
  color: #2c3e50;
  user-select: none;
}

.form-section {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.form-field-inline {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid #f1f3f4;
}

.form-field-inline:last-child {
  border-bottom: none;
}

.field-label-inline {
  font-weight: 500;
  color: #2c3e50;
  font-size: 0.9rem;
  min-width: 120px;
  flex-shrink: 0;
}

.field-input-container {
  position: relative;
  flex: 1;
}

.field-input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: all 0.2s;
  background: white;
}

.field-input:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.1);
}

.field-input.readonly {
  background: white;
  cursor: pointer;
  border: 1px solid #e9ecef;
}

.field-input.readonly:hover {
  border-color: #3498db;
  background: white;
}


.form-textarea {
  min-height: 40px;
  max-height: 40px;
  height: 40px;
  resize: none;
  font-family: inherit;
  overflow: hidden;
}

.form-element-section {
  border-bottom: 1px solid #f1f3f4;
}

.element-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: #f8f9fa;
  cursor: pointer;
  transition: background-color 0.2s;
}

.element-header:hover {
  background: #e9ecef;
}

.element-icon {
  font-size: 1rem;
}

.element-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 0.95rem;
  flex: 1;
}

.btn-toggle {
  background: none;
  border: none;
  font-size: 0.8rem;
  color: #6c757d;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 3px;
  transition: all 0.2s;
}

.btn-toggle:hover {
  background: rgba(0, 0, 0, 0.1);
  color: #2c3e50;
}

.btn-toggle.expanded {
  color: #3498db;
}

.field-error-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  background: #fff5f5;
  border: 1px solid #fecaca;
  border-radius: 4px;
}

.field-error-icon {
  font-size: 0.9rem;
  flex-shrink: 0;
}

.field-error {
  font-size: 0.85rem;
  color: #dc3545;
  font-weight: 500;
  line-height: 1.4;
}

.field-input.invalid {
  border-color: #dc3545;
  background-color: #fff5f5;
}

.field-input.invalid:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 3px rgba(220, 53, 69, 0.1);
}

.form-children {
  padding: 0.5rem;
  background: #fafbfc;
  border-top: 1px solid #f1f3f4;
}

.form-children .form-node {
  margin-bottom: 0.25rem;
}

.form-children .form-node:last-child {
  margin-bottom: 0;
}

/* Responsive design */
@media (max-width: 768px) {
  .section-header {
    padding: 0.75rem 1rem;
  }
  
  .form-field {
    padding: 1rem;
  }
  
  .form-children {
    padding: 0.75rem 1rem;
  }
  
  .section-name {
    font-size: 1rem;
  }
}

/* Focus states for accessibility */
.field-input:focus-visible {
  outline: 2px solid #3498db;
  outline-offset: 2px;
}

.btn-toggle:focus-visible {
  outline: 2px solid #3498db;
  outline-offset: 2px;
}

/* Animation for smooth transitions */
.form-section {
  transition: box-shadow 0.2s ease;
}

.form-section:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}
</style>
