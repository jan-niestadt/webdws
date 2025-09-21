<!--
  XPath Query Interface - Interactive XPath 3.1 Query Tool
  
  This component provides:
  - Interactive XPath expression input and execution
  - Support for both basic XPath and XPath 3.1 features
  - Example queries for common operations
  - Real-time result display with formatting
  - Integration with current XML document content
  - Error handling and result type detection
-->
<template>
  <div class="xpath-query">
    <div class="query-header">
      <h4>XPath 3.1 Query</h4>
      <div class="query-input-group">
        <input 
          v-model="xpathExpression" 
          @keyup.enter="executeQuery"
          class="query-input"
          placeholder="Enter XPath expression (e.g., //element[@id='test'])"
        />
        <button @click="executeQuery" class="btn btn-primary" :disabled="!xpathExpression.trim()">
          Execute
        </button>
      </div>
    </div>

    <div v-if="queryResult" class="query-result">
      <div class="result-header">
        <span class="result-status" :class="{ 'success': queryResult.success, 'error': !queryResult.success }">
          {{ queryResult.success ? '✓ Success' : '✗ Error' }}
        </span>
        <button @click="clearResult" class="btn btn-sm">Clear</button>
      </div>
      
      <div v-if="queryResult.success" class="result-content">
        <div class="result-type">
          <strong>Result Type:</strong> {{ getResultType(queryResult.result) }}
        </div>
        <div class="result-count" v-if="Array.isArray(queryResult.result)">
          <strong>Count:</strong> {{ queryResult.result.length }} items
        </div>
        <pre class="result-data">{{ formatResult(queryResult.result) }}</pre>
      </div>
      
      <div v-else class="error-content">
        <pre class="error-message">{{ queryResult.error }}</pre>
      </div>
    </div>

    <div class="query-examples">
      <h5>Example Queries:</h5>
      <div class="examples-list">
        <button 
          v-for="example in examples" 
          :key="example.name"
          @click="useExample(example)"
          class="example-btn"
        >
          {{ example.name }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { xmlService, type XPathResult } from '@/services/xmlService';

// Props
const props = defineProps<{
  xmlContent: string;
}>();

// State
const xpathExpression = ref('');
const queryResult = ref<XPathResult | null>(null);

// Example queries
const examples = [
  { name: 'All elements', expression: '//*' },
  { name: 'Root element', expression: '/*' },
  { name: 'Elements with attributes', expression: '//*[@*]' },
  { name: 'Text content', expression: '//text()' },
  { name: 'Element names', expression: '//*/name()' },
  { name: 'Count elements', expression: 'count(//*)' },
  { name: 'Element with specific name', expression: '//root' },
  { name: 'Elements with specific attribute', expression: '//*[@id]' },
  { name: 'XPath 3.1 - Array', expression: 'array { 1, 2, 3 }' },
  { name: 'XPath 3.1 - Map', expression: 'map { "key1": "value1", "key2": "value2" }' },
  { name: 'XPath 3.1 - String functions', expression: 'upper-case("hello world")' },
  { name: 'XPath 3.1 - Math functions', expression: 'math:sqrt(16)' }
];

// Methods
const executeQuery = async () => {
  if (!xpathExpression.value.trim() || !props.xmlContent.trim()) {
    return;
  }

  try {
    const result = await xmlService.evaluateXPathOnString(
      xpathExpression.value, 
      props.xmlContent
    );
    queryResult.value = result;
  } catch (error) {
    queryResult.value = {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error'
    };
  }
};

const useExample = (example: { name: string; expression: string }) => {
  xpathExpression.value = example.expression;
  executeQuery();
};

const clearResult = () => {
  queryResult.value = null;
};

const getResultType = (result: any): string => {
  if (result === null) return 'null';
  if (result === undefined) return 'undefined';
  if (Array.isArray(result)) return 'array';
  if (typeof result === 'object') return 'object';
  return typeof result;
};

const formatResult = (result: any): string => {
  if (result === null) return 'null';
  if (result === undefined) return 'undefined';
  if (Array.isArray(result)) {
    return JSON.stringify(result, null, 2);
  }
  if (typeof result === 'object') {
    return JSON.stringify(result, null, 2);
  }
  return String(result);
};
</script>

<style scoped>
.xpath-query {
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 1rem;
  background: white;
  margin-top: 1rem;
}

.query-header h4 {
  margin: 0 0 1rem 0;
  color: #2c3e50;
}

.query-input-group {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.query-input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.875rem;
}

.query-input:focus {
  outline: none;
  border-color: #3498db;
  box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.2);
}

.query-result {
  border: 1px solid #e9ecef;
  border-radius: 4px;
  background: #f8f9fa;
  margin-bottom: 1rem;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background: #e9ecef;
  border-bottom: 1px solid #dee2e6;
}

.result-status {
  font-weight: 500;
}

.result-status.success {
  color: #27ae60;
}

.result-status.error {
  color: #e74c3c;
}

.result-content, .error-content {
  padding: 1rem;
}

.result-type, .result-count {
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  color: #6c757d;
}

.result-data, .error-message {
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 0.75rem;
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.875rem;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}

.error-message {
  color: #e74c3c;
  background: #fdf2f2;
  border-color: #fecaca;
}

.query-examples h5 {
  margin: 0 0 0.5rem 0;
  color: #2c3e50;
  font-size: 0.875rem;
}

.examples-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.example-btn {
  padding: 0.25rem 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  background: white;
  color: #2c3e50;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.example-btn:hover {
  background: #f8f9fa;
  border-color: #3498db;
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

.btn-primary {
  background: #3498db;
  color: white;
  border-color: #3498db;
}

.btn-primary:hover:not(:disabled) {
  background: #2980b9;
  border-color: #2980b9;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
}
</style>
