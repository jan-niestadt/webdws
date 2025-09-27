import type { SchemaElement } from '@/services/api';

/**
 * Get the type-based default value for a given type
 */
const getTypeBasedDefault = (type: string): string => {
  const lowerType = type.toLowerCase();

  if (lowerType.includes('boolean')) {
    return 'false';
  }

  if (lowerType.includes('int') && !lowerType.includes('string')) {
    return '0';
  }

  if (lowerType.includes('decimal') || lowerType.includes('float') || lowerType.includes('double')) {
    return '0';
  }

  if (lowerType.includes('date')) {
    return new Date().toISOString().split('T')[0];
  }

  if (lowerType.includes('year')) {
    return new Date().getFullYear().toString();
  }

  // For other types, return empty string
  return '';
};

/**
 * Get the default value for an attribute based on schema defaults and type
 */
export const getDefaultAttributeValue = (attr: { type: string; defaultValue?: string; fixedValue?: string }): string => {
  // Use default value if specified in schema
  if (attr.defaultValue) {
    return attr.defaultValue;
  }
  
  // Use fixed value if specified in schema
  if (attr.fixedValue) {
    return attr.fixedValue;
  }
  
  // Generate type-based defaults
  return getTypeBasedDefault(attr.type);
};

/**
 * Get the default text content for an element based on schema defaults and type
 */
export const getDefaultTextContent = (element: SchemaElement): string => {
  // Use default value if specified in schema
  if (element.defaultValue) {
    return element.defaultValue;
  }
  
  // Use fixed value if specified in schema
  if (element.fixedValue) {
    return element.fixedValue;
  }
  
  // Generate type-based defaults for text content
  return getTypeBasedDefault(element.type);
};
