/**
 * XML Type Definitions - TypeScript interfaces for XML data structures
 * 
 * This file defines all the TypeScript interfaces used throughout the application:
 * - Backend API data structures (documents, requests, responses)
 * - Frontend tree editor data structures (nodes, state)
 * - Service layer interfaces (parsing, validation, XPath results)
 */

// Backend API Data Structures
export interface XmlDocument {
  id: string;
  name: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface XmlDocumentList {
  documents: XmlDocument[];
  total: number;
}

export interface SaveXmlRequest {
  name: string;
  content: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

// Frontend Tree Editor Data Structures
export interface XmlNode {
  id: string;
  type: 'element' | 'text' | 'comment' | 'cdata' | 'processing-instruction';
  name?: string;
  value?: string;
  attributes?: { [key: string]: string };
  children: XmlNode[];
  parent?: string;
  expanded?: boolean;
}

export interface XmlTreeState {
  root: XmlNode | null;
  selectedNodeId: string | null;
  modified: boolean;
}
