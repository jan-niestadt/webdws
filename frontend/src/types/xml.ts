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
