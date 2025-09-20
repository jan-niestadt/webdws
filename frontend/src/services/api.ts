import axios from 'axios';
import type { XmlDocument, XmlDocumentList, SaveXmlRequest, ApiResponse } from '@/types/xml';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const xmlApi = {
  // Get all XML documents
  async getDocuments(): Promise<XmlDocumentList> {
    const response = await api.get<ApiResponse<XmlDocumentList>>('/api/xml/documents');
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.error || 'Failed to fetch documents');
  },

  // Get a specific XML document by ID
  async getDocument(id: string): Promise<XmlDocument> {
    const response = await api.get<ApiResponse<XmlDocument>>(`/api/xml/documents/${id}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.error || 'Failed to fetch document');
  },

  // Save a new XML document
  async saveDocument(document: SaveXmlRequest): Promise<XmlDocument> {
    const response = await api.post<ApiResponse<XmlDocument>>('/api/xml/documents', document);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.error || 'Failed to save document');
  },

  // Update an existing XML document
  async updateDocument(id: string, document: SaveXmlRequest): Promise<XmlDocument> {
    const response = await api.put<ApiResponse<XmlDocument>>(`/api/xml/documents/${id}`, document);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.error || 'Failed to update document');
  },

  // Delete an XML document
  async deleteDocument(id: string): Promise<void> {
    const response = await api.delete<ApiResponse<void>>(`/api/xml/documents/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.error || 'Failed to delete document');
    }
  },

  // Validate XML content
  async validateXml(content: string): Promise<{ valid: boolean; error?: string }> {
    try {
      const response = await api.post<ApiResponse<{ valid: boolean; error?: string }>>('/api/xml/validate', { content });
      if (response.data.success && response.data.data) {
        return response.data.data;
      }
      throw new Error(response.data.error || 'Validation failed');
    } catch (error) {
      return { valid: false, error: 'Network error during validation' };
    }
  }
};
