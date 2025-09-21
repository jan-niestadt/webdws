# XML Tree Editor Features

This document describes the new tree-based XML editor features added to the WebDWS application.

## Overview

The XML editor now supports two modes:
1. **Text Editor Mode** (Read-only) - View XML as formatted text
2. **Tree Editor Mode** (Editable) - Edit XML as a hierarchical tree structure

## Features

### Mode Toggle
- Toggle between Text and Tree editing modes using the buttons in the editor header
- Text mode shows the XML content in a read-only Monaco editor
- Tree mode provides interactive tree-based editing

### Tree Editor Capabilities

#### Node Management
- **Add Elements**: Create new XML elements as children of selected nodes
- **Add Text**: Add text content to elements
- **Add Comments**: Insert XML comments
- **Delete Nodes**: Remove nodes (except root element)
- **Edit Properties**: Modify element names, text content, and attributes

#### Tree Navigation
- **Expand/Collapse**: Click the arrow icons to expand or collapse node branches
- **Select Nodes**: Click on any node to select and edit its properties
- **Visual Indicators**: Different icons for elements, text, comments, and other node types

#### Properties Panel
When a node is selected, the properties panel shows:
- **Node Type**: Element, text, comment, etc.
- **Element Name**: Editable for element nodes
- **Text Value**: Editable for text and comment nodes
- **Attributes**: Add, edit, or remove element attributes

#### Tree Operations
- **Expand All**: Expand all tree nodes at once
- **Collapse All**: Collapse all tree nodes
- **Auto-sync**: Changes in tree mode automatically update the XML content

### Technical Implementation

#### Frontend
- **Vue 3** with TypeScript
- **SaxonJS** for XML parsing and validation
- **Monaco Editor** for text mode (read-only)
- **Custom Tree Components** for hierarchical editing

#### Backend
- **Spring Boot** REST API
- **eXist-db** for XML storage
- **PostgreSQL** for metadata
- **XML validation** using Java XML parsers

## Usage

1. **Start the application**:
   ```bash
   docker-compose up -d
   ```

2. **Access the editor**:
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080

3. **Switch modes**:
   - Click "Text Editor" to view XML as formatted text
   - Click "Tree Editor" to edit XML as a tree structure

4. **Tree editing**:
   - Select a node to edit its properties
   - Use toolbar buttons to add new nodes
   - Right-click or use context menus for additional options

## File Structure

```
frontend/src/
├── components/
│   ├── XmlTreeEditor.vue      # Main tree editor component
│   └── XmlTreeNode.vue        # Individual tree node component
├── services/
│   └── xmlService.ts          # XML parsing and validation service
├── types/
│   └── xml.ts                 # TypeScript interfaces
└── views/
    └── Editor.vue             # Updated editor with mode toggle
```

## Dependencies

- **saxon-js**: ^2.6.0 - XML processing and validation
- **monaco-editor**: ^0.45.0 - Text editor (read-only mode)
- **vue**: ^3.4.0 - Frontend framework
- **axios**: ^1.6.0 - HTTP client

## Future Enhancements

- Schema validation with XSD
- XPath query support
- Advanced XML transformations
- Drag-and-drop node reordering
- Undo/redo functionality
- Export to different XML formats
