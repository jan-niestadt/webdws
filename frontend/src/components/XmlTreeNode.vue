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
        'collapsed': !node.expanded && node.children.length > 0
      }"
      @click="handleNodeClick"
    >
      <div class="node-content">
        <span 
          v-if="node.children.length > 0" 
          class="expand-toggle"
          @click.stop="toggleNode"
        >
          {{ node.expanded ? '▼' : '▶' }}
        </span>
        <span v-else class="expand-spacer"></span>
        
        <span class="node-icon" :class="node.type">
          {{ getNodeIcon(node.type) }}
        </span>
        
        <span class="node-name" v-if="node.type === 'element'">
          &lt;{{ node.name }}{{ node.children.length === 0 ? ' /' : '' }}&gt;
        </span>
        
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
          @click.stop
        >
          <input 
            v-model="editingName"
            @blur="finishAttributeEdit"
            @keydown.enter.prevent="finishAttributeEdit"
            @keydown.escape.prevent="cancelInlineEdit"
            class="attribute-name-input"
            ref="nameInput"
            placeholder="attribute name"
            :title="'Press Enter to save, Escape to cancel'"
          />
          <span class="attribute-equals">=</span>
          <input 
            v-model="editingValue"
            @blur="finishAttributeEdit"
            @keydown.enter.prevent="finishAttributeEdit"
            @keydown.escape.prevent="cancelInlineEdit"
            class="attribute-value-input"
            ref="valueInput"
            placeholder="attribute value"
            :title="'Press Enter to save, Escape to cancel'"
          />
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
    
    <div v-if="node.expanded && node.children.length > 0" class="node-children">
      <XmlTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :selected-id="selectedId"
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
const editingName = ref('');
const editInput = ref<HTMLInputElement | null>(null);
const nameInput = ref<HTMLInputElement | null>(null);
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


// Inline editing methods
const startInlineEdit = async () => {
  if (!['text', 'comment', 'cdata', 'attribute'].includes(props.node.type)) return;
  
  isEditing.value = true;
  editingValue.value = props.node.value || '';
  
  if (props.node.type === 'attribute') {
    editingName.value = props.node.name || '';
  }
  
  await nextTick();
  const inputToFocus = props.node.type === 'attribute' ? nameInput.value : editInput.value;
  if (inputToFocus) {
    inputToFocus.focus();
    inputToFocus.select();
  }
};

const finishInlineEdit = () => {
  if (!isEditing.value) return;
  
  const newValue = editingValue.value.trim();
  
  // For text nodes, remove if empty; for comments and CDATA, keep even if empty
  if (props.node.type === 'text' && newValue === '') {
    emit('edit', { nodeId: props.node.id, field: 'remove', value: null });
  } else {
    emit('edit', { nodeId: props.node.id, field: 'value', value: newValue });
  }
  
  isEditing.value = false;
  editingValue.value = '';
};

const finishAttributeEdit = () => {
  if (!isEditing.value || props.node.type !== 'attribute') return;
  
  const newName = editingName.value.trim();
  const newValue = editingValue.value.trim();
  
  if (newName !== props.node.name || newValue !== props.node.value) {
    emit('edit', {
      nodeId: props.node.id,
      field: 'attribute',
      value: { name: newName, value: newValue }
    });
  }
  
  isEditing.value = false;
  editingName.value = '';
  editingValue.value = '';
};

const cancelInlineEdit = () => {
  isEditing.value = false;
  editingValue.value = '';
  editingName.value = '';
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

.attribute-name-input,
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

.attribute-name-input {
  min-width: 80px;
  font-weight: bold;
}

.attribute-value-input {
  min-width: 100px;
}

.attribute-equals {
  color: #6c757d;
  font-weight: bold;
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

/* Hover effects */
.node-line:hover .node-name {
  color: #3498db;
}

.node-line:hover .node-value {
  color: #2c3e50;
}
</style>
