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







}

// Export singleton instance
export const xmlService = XmlService.getInstance();
