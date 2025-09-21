/**
 * XML Service - Core XML processing functionality using SaxonJS
 * 
 * This service provides:
 * - XML parsing and validation using SaxonJS for XPath 3.1 support
 * - XPath evaluation with both basic and advanced features
 * - XML serialization and formatting
 * - Fallback to native browser APIs when SaxonJS is unavailable
 * 
 * SaxonJS is loaded globally via CDN for XPath 3.1 and XSLT 3.0 support
 */
declare const SaxonJS: any;

export interface XmlParseResult {
  success: boolean;
  document?: Document;
  error?: string;
}

export interface XmlValidationResult {
  valid: boolean;
  error?: string;
}

export interface XPathResult {
  success: boolean;
  result?: any;
  error?: string;
}

export interface SchemaValidationResult {
  valid: boolean;
  errors?: string[];
  warnings?: string[];
}

export interface SchemaInfo {
  targetNamespace?: string;
  elements: SchemaElement[];
}

export interface SchemaElement {
  name: string;
  namespace?: string;
  type: 'element' | 'attribute';
  required: boolean;
  children?: SchemaElement[];
  attributes?: SchemaElement[];
}

export interface AllowedContent {
  elements: AllowedElement[];
  attributes: AllowedAttribute[];
  canHaveText: boolean;
  canHaveComments: boolean;
}

export interface AllowedElement {
  name: string;
  namespace?: string;
  minOccurs: number;
  maxOccurs: number | 'unbounded';
  currentCount: number;
  canAdd: boolean;
}

export interface AllowedAttribute {
  name: string;
  namespace?: string;
  required: boolean;
  present: boolean;
  canAdd: boolean;
}

export class XmlService {
  private static instance: XmlService;

  public static getInstance(): XmlService {
    if (!XmlService.instance) {
      XmlService.instance = new XmlService();
    }
    return XmlService.instance;
  }

  /**
   * Parse XML string to DOM Document using native DOMParser
   * 
   * Uses the browser's native DOMParser for reliable XML parsing.
   * SaxonJS is used for XPath evaluation, not basic XML parsing.
   * This approach provides better compatibility and performance for basic operations.
   */
  public parseXml(xmlString: string): XmlParseResult {
    try {
      console.log('XML Service: Parsing XML with native DOMParser');
      console.log('XML Service: Input XML:', xmlString.substring(0, 200) + '...');
      
      // Use native DOMParser for reliable XML parsing
      const parser = new DOMParser();
      const doc = parser.parseFromString(xmlString, 'text/xml');
      
      if (doc.documentElement.nodeName === 'parsererror') {
        return {
          success: false,
          error: 'XML parsing error'
        };
      }

      console.log('XML Service: Successfully parsed with DOMParser');
      return {
        success: true,
        document: doc
      };
    } catch (error) {
      console.error('XML Service: Parsing error:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown parsing error'
      };
    }
  }

  /**
   * Validate XML string using SaxonJS
   */
  public validateXml(xmlString: string): XmlValidationResult {
    try {
      const parseResult = this.parseXml(xmlString);
      
      if (!parseResult.success) {
        return {
          valid: false,
          error: parseResult.error
        };
      }

      // SaxonJS provides better validation than basic parsing
      // Additional schema validation can be added here
      return {
        valid: true
      };
    } catch (error) {
      return {
        valid: false,
        error: error instanceof Error ? error.message : 'Validation error'
      };
    }
  }

  /**
   * Serialize DOM Document to XML string using SaxonJS
   */
  public serializeXml(document: Document): string {
    try {
      if (typeof SaxonJS === 'undefined') {
        console.warn('SaxonJS is not available, using native XMLSerializer');
        return new XMLSerializer().serializeToString(document);
      }

      return SaxonJS.serialize(document, {
        method: 'xml',
        indent: true,
        'omit-xml-declaration': false
      });
    } catch (error) {
      console.warn('SaxonJS serialization failed, falling back to native serializer:', error);
      // Fallback to native serializer if SaxonJS fails
      return new XMLSerializer().serializeToString(document);
    }
  }

  /**
   * Format/pretty print XML
   */
  public formatXml(xmlString: string): string {
    try {
      const parseResult = this.parseXml(xmlString);
      if (!parseResult.success || !parseResult.document) {
        return xmlString;
      }

      // Simple formatting - can be enhanced with SaxonJS
      const serializer = new XMLSerializer();
      const formatted = serializer.serializeToString(parseResult.document);
      
      // Basic indentation
      return this.indentXml(formatted);
    } catch (error) {
      return xmlString;
    }
  }

  /**
   * Basic XML indentation
   */
  private indentXml(xml: string): string {
    const PADDING = '  ';
    const reg = /(>)(<)(\/*)/g;
    let formatted = xml.replace(reg, '$1\n$2$3');
    let pad = 0;

    formatted = formatted.split('\n').map((node) => {
      let indent = 0;
      if (node.match(/.+<\/\w[^>]*>$/)) {
        indent = 0;
      } else if (node.match(/^<\/\w/) && pad > 0) {
        pad -= 1;
      } else if (node.match(/^<\w[^>]*[^\/]>.*$/)) {
        indent = 1;
      } else {
        indent = 0;
      }

      const padding = PADDING.repeat(pad);
      pad += indent;
      return padding + node;
    }).join('\n');

    return formatted;
  }

  /**
   * Convert DOM Node to a simple tree structure
   */
  public domToTree(domNode: Node): any {
    const result: any = {
      type: this.getNodeType(domNode),
      children: []
    };

    if (domNode.nodeType === Node.ELEMENT_NODE) {
      const element = domNode as Element;
      result.name = element.tagName;
      result.attributes = {};
      
      // Copy attributes
      for (let i = 0; i < element.attributes.length; i++) {
        const attr = element.attributes[i];
        result.attributes[attr.name] = attr.value;
      }
    } else if (domNode.nodeType === Node.TEXT_NODE || domNode.nodeType === Node.CDATA_SECTION_NODE) {
      result.value = domNode.textContent || '';
    } else if (domNode.nodeType === Node.COMMENT_NODE) {
      result.value = domNode.textContent || '';
    }

    // Process children
    for (let i = 0; i < domNode.childNodes.length; i++) {
      const child = domNode.childNodes[i];
      if (this.shouldIncludeNode(child)) {
        const childTree = this.domToTree(child);
        childTree.parent = result;
        result.children.push(childTree);
      }
    }

    return result;
  }

  private getNodeType(domNode: Node): string {
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
  }

  private shouldIncludeNode(domNode: Node): boolean {
    // Include text nodes that have non-whitespace content
    if (domNode.nodeType === Node.TEXT_NODE) {
      return domNode.textContent?.trim() !== '';
    }
    // Include all other node types
    return true;
  }

  /**
   * Execute XPath expression using SaxonJS
   * 
   * Evaluates XPath expressions on DOM nodes using SaxonJS for full XPath 3.1 support.
   * Handles both basic XPath expressions and advanced XPath 3.1 features.
   */
  public evaluateXPath(xpathExpression: string, contextNode: Node, namespaces?: { [prefix: string]: string }): XPathResult {
    try {
      if (typeof SaxonJS === 'undefined') {
        return {
          success: false,
          error: 'SaxonJS is not available. XPath evaluation requires SaxonJS.'
        };
      }

      console.log('Evaluating XPath expression on node with SaxonJS:', xpathExpression);
      console.log('Context node:', contextNode);
      console.log('Context node type:', contextNode.nodeType);
      console.log('Context node name:', contextNode.nodeName);

      // Ensure we have a proper document context
      const document = contextNode.ownerDocument || contextNode as Document;
      console.log('Document:', document);
      console.log('Document element:', document.documentElement);

      // Use SaxonJS XPath evaluation with proper context
      const result = SaxonJS.XPath.evaluate(xpathExpression, document, {
        namespaces: namespaces || {}
      });

      console.log('XPath evaluation successful with SaxonJS');
      return {
        success: true,
        result: result
      };
    } catch (error) {
      console.error('SaxonJS XPath evaluation failed:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'XPath evaluation error'
      };
    }
  }

  /**
   * Execute XPath expression on XML string using SaxonJS
   * 
   * Parses XML string with SaxonJS and evaluates XPath expressions.
   * This is the main method used by the XPath query interface.
   * Handles async parsing and result processing for display.
   */
  public async evaluateXPathOnString(xpathExpression: string, xmlString: string, namespaces?: { [prefix: string]: string }): Promise<XPathResult> {
    try {
      if (typeof SaxonJS === 'undefined') {
        return {
          success: false,
          error: 'SaxonJS is not available. XPath evaluation requires SaxonJS.'
        };
      }

      console.log('Evaluating XPath expression with SaxonJS:', xpathExpression);
      console.log('XML content length:', xmlString.length);

      // Parse XML using SaxonJS to get proper document structure
      const doc = await SaxonJS.getResource({ text: xmlString, type: 'xml' });
      
      if (!doc) {
        return {
          success: false,
          error: 'Failed to parse XML with SaxonJS'
        };
      }

      console.log('SaxonJS parsed document:', doc);
      console.log('Document type:', typeof doc);
      console.log('Document constructor:', doc.constructor.name);

      // Use SaxonJS XPath evaluation
      const result = SaxonJS.XPath.evaluate(xpathExpression, doc, {
        namespaces: namespaces || {}
      });

      console.log('XPath evaluation successful with SaxonJS');
      console.log('Raw result:', result);
      console.log('Result type:', typeof result);
      //console.log('Result constructor:', result.constructor.name);

      // Convert SaxonJS result to a displayable format
      const processedResult = this.processSaxonJSResult(result);
      console.log('Processed result:', processedResult);

      return {
        success: true,
        result: processedResult
      };
    } catch (error) {
      console.error('SaxonJS XPath evaluation failed:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'XPath evaluation error'
      };
    }
  }

  /**
   * Process SaxonJS result to make it displayable
   * 
   * Converts SaxonJS internal node objects to plain JavaScript objects
   * that can be serialized to JSON and displayed in the UI.
   * Handles both single nodes and node sequences.
   */
  private processSaxonJSResult(result: any): any {
    if (result === null || result === undefined) {
      return result;
    }

    // If it's an array-like object (SaxonJS sequence)
    if (Array.isArray(result) || (typeof result === 'object' && result.length !== undefined)) {
      const processedArray = [];
      for (let i = 0; i < result.length; i++) {
        processedArray.push(this.processSaxonJSNode(result[i]));
      }
      return processedArray;
    }

    // If it's a single node or value
    return this.processSaxonJSNode(result);
  }

  /**
   * Process a single SaxonJS node or value
   * 
   * Extracts meaningful information from SaxonJS node objects including:
   * - Node type, name, and value
   * - Attributes for element nodes
   * - Child nodes recursively
   * - Handles primitive values (strings, numbers, booleans)
   */
  private processSaxonJSNode(node: any): any {
    if (node === null || node === undefined) {
      return node;
    }

    // If it's a primitive value (string, number, boolean)
    if (typeof node !== 'object') {
      return node;
    }

    // If it's a SaxonJS node object
    if (node.nodeType !== undefined) {
      const nodeInfo: any = {
        nodeType: node.nodeType,
        nodeName: node.nodeName || node.localName || 'unknown',
        nodeValue: node.nodeValue || node.textContent || '',
        textContent: node.textContent || node.nodeValue || ''
      };

      // Add attributes if it's an element node
      if (node.nodeType === 1 && node.attributes) {
        nodeInfo.attributes = {};
        for (let i = 0; i < node.attributes.length; i++) {
          const attr = node.attributes[i];
          nodeInfo.attributes[attr.nodeName || attr.name] = attr.nodeValue || attr.value;
        }
      }

      // Add children if present
      if (node.childNodes && node.childNodes.length > 0) {
        nodeInfo.children = [];
        for (let i = 0; i < node.childNodes.length; i++) {
          nodeInfo.children.push(this.processSaxonJSNode(node.childNodes[i]));
        }
      }

      return nodeInfo;
    }

    // If it's some other object, try to extract useful information
    const processed: any = {};
    for (const key in node) {
      if (node.hasOwnProperty(key)) {
        const value = node[key];
        if (typeof value === 'object' && value !== null) {
          processed[key] = this.processSaxonJSNode(value);
        } else {
          processed[key] = value;
        }
      }
    }

    return processed;
  }


  /**
   * Transform XML using XSLT 3.0 with SaxonJS
   */
  public transformXml(xmlString: string, xsltString: string): { success: boolean; result?: string; error?: string } {
    try {
      const result = SaxonJS.transform({
        stylesheetText: xsltString,
        sourceText: xmlString,
        destination: 'serialized'
      });
      
      return {
        success: true,
        result: result.principalResult
      };
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : 'XSLT transformation error'
      };
    }
  }

  /**
   * Load and parse XML Schema using SaxonJS
   */
  public async loadSchema(schemaUrl: string): Promise<{ success: boolean; schema?: any; error?: string }> {
    try {
      if (typeof SaxonJS === 'undefined') {
        return { success: false, error: 'SaxonJS is not available. Schema loading requires SaxonJS.' };
      }

      const schema = await SaxonJS.getResource({ text: schemaUrl, type: 'xml' });
      if (!schema) {
        return { success: false, error: 'Failed to load schema from URL' };
      }

      return { success: true, schema };
    } catch (error) {
      console.error('Schema loading failed:', error);
      return { success: false, error: error instanceof Error ? error.message : 'Schema loading error' };
    }
  }

  /**
   * Load schema from content string using SaxonJS
   */
  public async loadSchemaFromContent(schemaContent: string): Promise<{ success: boolean; schema?: any; error?: string }> {
    try {
      if (typeof SaxonJS === 'undefined') {
        return { success: false, error: 'SaxonJS is not available. Schema loading requires SaxonJS.' };
      }

      const schema = await SaxonJS.getResource({ text: schemaContent, type: 'xml' });
      if (!schema) {
        return { success: false, error: 'Failed to parse schema content' };
      }

      return { success: true, schema };
    } catch (error) {
      console.error('Schema loading failed:', error);
      return { success: false, error: error instanceof Error ? error.message : 'Schema loading error' };
    }
  }

  /**
   * Validate XML against a schema using SaxonJS
   */
  public async validateAgainstSchema(xmlString: string, schemaContent: string): Promise<SchemaValidationResult> {
    try {
      if (typeof SaxonJS === 'undefined') {
        return { valid: false, errors: ['SaxonJS is not available. Schema validation requires SaxonJS.'] };
      }

      // Load the schema
      const schemaResult = await this.loadSchemaFromContent(schemaContent);
      if (!schemaResult.success || !schemaResult.schema) {
        return { valid: false, errors: [schemaResult.error || 'Failed to load schema'] };
      }

      // Parse the XML
      const xmlDoc = await SaxonJS.getResource({ text: xmlString, type: 'xml' });
      if (!xmlDoc) {
        return { valid: false, errors: ['Failed to parse XML document'] };
      }

      // Parse the schema to extract element definitions
      const schemaDoc = await SaxonJS.getResource({ text: schemaContent, type: 'xml' });
      if (!schemaDoc) {
        return { valid: false, errors: ['Failed to parse schema document'] };
      }

      // Use SaxonJS for schema validation
      try {
        // For now, we'll do basic validation since SaxonJS schema validation
        // requires more complex setup. In a production environment, you would:
        // 1. Compile the schema using SaxonJS
        // 2. Use SaxonJS's schema validation features
        // 3. Parse validation errors and provide detailed feedback
        
        // Basic XML structure validation
        const errors: string[] = [];
        
        // Check for well-formed XML
        if (!xmlDoc.documentElement) {
          errors.push('Document must have a root element');
        }
        
        // Check namespace declarations if schema has target namespace
        const targetNamespace = schemaDoc.documentElement?.getAttribute('targetNamespace');
        if (targetNamespace && xmlDoc.documentElement) {
          const rootElement = xmlDoc.documentElement;
          const rootNamespace = rootElement.namespaceURI;
          if (rootNamespace !== targetNamespace) {
            errors.push(`Root element namespace '${rootNamespace || 'none'}' does not match schema target namespace '${targetNamespace}'`);
          }
        }
        
        // Check root element name against schema
        const rootElementName = xmlDoc.documentElement?.localName;
        const schemaRootElement = schemaDoc.querySelector('element[name]');
        const expectedRootName = schemaRootElement?.getAttribute('name');
        
        if (expectedRootName && rootElementName !== expectedRootName) {
          errors.push(`Root element '${rootElementName}' is not allowed. Expected '${expectedRootName}' according to schema`);
        }
        
        // Check for required elements based on schema
        const schemaElements = schemaDoc.querySelectorAll('element');
        for (const schemaElement of schemaElements) {
          const elementName = schemaElement.getAttribute('name');
          const minOccurs = parseInt(schemaElement.getAttribute('minOccurs') || '1');
          const maxOccurs = schemaElement.getAttribute('maxOccurs') || '1';
          
          if (elementName && minOccurs > 0) {
            const actualElements = xmlDoc.querySelectorAll(elementName);
            if (actualElements.length < minOccurs) {
              errors.push(`Element '${elementName}' must appear at least ${minOccurs} time(s), found ${actualElements.length}`);
            }
            
            if (maxOccurs !== 'unbounded') {
              const maxOccursNum = parseInt(maxOccurs);
              if (actualElements.length > maxOccursNum) {
                errors.push(`Element '${elementName}' can appear at most ${maxOccursNum} time(s), found ${actualElements.length}`);
              }
            }
          }
        }
        
        // Check for required attributes
        const schemaAttributes = schemaDoc.querySelectorAll('attribute');
        for (const schemaAttr of schemaAttributes) {
          const attrName = schemaAttr.getAttribute('name');
          const use = schemaAttr.getAttribute('use');
          
          if (attrName && use === 'required') {
            const elementsWithAttr = xmlDoc.querySelectorAll(`[${attrName}]`);
            if (elementsWithAttr.length === 0) {
              errors.push(`Required attribute '${attrName}' is missing from all elements`);
            }
          }
        }
        
        // Check for elements that are not defined in schema
        const allElements = xmlDoc.querySelectorAll('*');
        const allowedElementNames = Array.from(schemaElements).map((el: any) => el.getAttribute('name')).filter(Boolean);
        
        for (const element of allElements) {
          const elementName = element.localName;
          if (allowedElementNames.length > 0 && !allowedElementNames.includes(elementName)) {
            errors.push(`Element '${elementName}' is not defined in the schema`);
          }
        }
        
        // Check for attributes that are not defined in schema
        const allAttributes = xmlDoc.querySelectorAll('*[attribute]');
        const allowedAttributeNames = Array.from(schemaAttributes).map((attr: any) => attr.getAttribute('name')).filter(Boolean);
        
        for (const element of allAttributes) {
          const elementAttrs = element.attributes;
          for (const attr of elementAttrs) {
            const attrName = attr.localName;
            if (allowedAttributeNames.length > 0 && !allowedAttributeNames.includes(attrName)) {
              errors.push(`Attribute '${attrName}' is not defined in the schema for element '${element.localName}'`);
            }
          }
        }

        return {
          valid: errors.length === 0,
          errors: errors.length > 0 ? errors : undefined
        };
      } catch (validationError) {
        return {
          valid: false,
          errors: [validationError instanceof Error ? validationError.message : 'Schema validation failed']
        };
      }
    } catch (error) {
      console.error('Schema validation failed:', error);
      return {
        valid: false,
        errors: [error instanceof Error ? error.message : 'Schema validation error']
      };
    }
  }

  /**
   * Parse schema and extract element information
   */
  public async parseSchemaInfo(schemaUrl: string): Promise<{ success: boolean; schemaInfo?: SchemaInfo; error?: string }> {
    try {
      if (typeof SaxonJS === 'undefined') {
        return { success: false, error: 'SaxonJS is not available. Schema parsing requires SaxonJS.' };
      }

      const schemaResult = await this.loadSchema(schemaUrl);
      if (!schemaResult.success || !schemaResult.schema) {
        return { success: false, error: schemaResult.error || 'Failed to load schema' };
      }

      // Parse the schema to extract element information
      const schemaInfo: SchemaInfo = {
        elements: []
      };

      // Extract target namespace
      const targetNamespace = schemaResult.schema.documentElement?.getAttribute('targetNamespace');
      if (targetNamespace) {
        schemaInfo.targetNamespace = targetNamespace;
      }

      // Extract elements (simplified parsing)
      const elements = schemaResult.schema.querySelectorAll('element');
      for (const element of elements) {
        const name = element.getAttribute('name');
        if (name) {
          schemaInfo.elements.push({
            name: name,
            namespace: targetNamespace,
            type: 'element',
            required: true,
            children: [],
            attributes: []
          });
        }
      }

      return { success: true, schemaInfo };
    } catch (error) {
      console.error('Schema parsing failed:', error);
      return { success: false, error: error instanceof Error ? error.message : 'Schema parsing error' };
    }
  }

  /**
   * Determine allowed content at a specific position in the XML document using SaxonJS
   * This is the key method that analyzes the schema to determine what can be added
   */
  public async getAllowedContentAtPosition(
    xmlString: string, 
    schemaContent: string
  ): Promise<{ success: boolean; allowedContent?: AllowedContent; error?: string }> {
    try {
      if (typeof SaxonJS === 'undefined') {
        return { success: false, error: 'SaxonJS is not available. Schema analysis requires SaxonJS.' };
      }

      // Load schema and XML
      const schemaResult = await this.loadSchemaFromContent(schemaContent);
      if (!schemaResult.success || !schemaResult.schema) {
        return { success: false, error: schemaResult.error || 'Failed to load schema' };
      }

      const xmlDoc = await SaxonJS.getResource({ text: xmlString, type: 'xml' });
      if (!xmlDoc) {
        return { success: false, error: 'Failed to parse XML document' };
      }

      // Use SaxonJS to analyze the schema and determine allowed content
      // This is a simplified implementation - in practice, you'd use SaxonJS's
      // schema validation features to analyze the content model
      const allowedContent: AllowedContent = {
        elements: [],
        attributes: [],
        canHaveText: false,
        canHaveComments: true
      };

      // Parse the schema to extract element definitions
      const schemaDoc = await SaxonJS.getResource({ text: schemaContent, type: 'xml' });
      if (schemaDoc) {
        // Find the target element definition in the schema
        const elements = schemaDoc.querySelectorAll('element');
        for (const element of elements) {
          const name = element.getAttribute('name');
          const minOccurs = parseInt(element.getAttribute('minOccurs') || '1');
          const maxOccurs = element.getAttribute('maxOccurs') || '1';
          
          if (name) {
            // Count current occurrences in the target node
            const currentCount = this.countElementOccurrences(xmlDoc, name);
            const maxOccursNum = maxOccurs === 'unbounded' ? Infinity : parseInt(maxOccurs);
            
            allowedContent.elements.push({
              name: name,
              namespace: schemaDoc.documentElement?.getAttribute('targetNamespace') || undefined,
              minOccurs: minOccurs,
              maxOccurs: maxOccurs === 'unbounded' ? 'unbounded' : maxOccursNum,
              currentCount: currentCount,
              canAdd: currentCount < maxOccursNum
            });
          }
        }

        // Extract attributes
        const attributes = schemaDoc.querySelectorAll('attribute');
        for (const attr of attributes) {
          const name = attr.getAttribute('name');
          const use = attr.getAttribute('use') || 'optional';
          
          if (name) {
            const present = this.hasAttribute(xmlDoc, name);
            allowedContent.attributes.push({
              name: name,
              namespace: schemaDoc.documentElement?.getAttribute('targetNamespace') || undefined,
              required: use === 'required',
              present: present,
              canAdd: !present
            });
          }
        }
      }

      return { success: true, allowedContent };
    } catch (error) {
      console.error('Schema analysis failed:', error);
      return { success: false, error: error instanceof Error ? error.message : 'Schema analysis error' };
    }
  }

  /**
   * Count occurrences of an element within a specific node
   */
  private countElementOccurrences(xmlDoc: any, elementName: string): number {
    try {
      // This is a simplified implementation
      // In practice, you'd use XPath to count elements more accurately
      const xpath = `count(.//${elementName})`;
      const result = SaxonJS.XPath.evaluate(xpath, xmlDoc);
      return typeof result === 'number' ? result : 0;
    } catch (error) {
      console.warn('Error counting element occurrences:', error);
      return 0;
    }
  }

  /**
   * Check if a specific attribute exists on a node
   */
  private hasAttribute(xmlDoc: any, attributeName: string): boolean {
    try {
      // This is a simplified implementation
      // In practice, you'd use XPath to check attributes more accurately
      const xpath = `exists(.//@${attributeName})`;
      const result = SaxonJS.XPath.evaluate(xpath, xmlDoc);
      return result === true;
    } catch (error) {
      console.warn('Error checking attribute:', error);
      return false;
    }
  }
}

// Export singleton instance
export const xmlService = XmlService.getInstance();
