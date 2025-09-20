# WebDWS - Web-based XML Editor

A modern web-based XML editor built with Vue 3, Spring Boot, and eXist-db, all containerized with Docker.

## Architecture

- **Frontend**: Vue 3 + TypeScript with Monaco Editor for XML editing
- **Backend**: Spring Boot REST API with Java 17
- **Database**: PostgreSQL for metadata + eXist-db for XML storage
- **Containerization**: Docker and Docker Compose

## Features

- 🎨 Modern Vue 3 frontend with TypeScript
- 📝 Monaco Editor with XML syntax highlighting
- 🔍 Real-time XML validation
- 💾 Save/load XML documents
- 🗂️ Document management (list, create, update, delete)
- 🐳 Fully containerized with Docker
- 🗄️ Hybrid storage: PostgreSQL for metadata, eXist-db for XML content

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Git (to clone the repository)

### Running the Application

1. **Clone and navigate to the project:**
   ```bash
   git clone <repository-url>
   cd webdws
   ```

2. **Start all services with Docker Compose:**
   ```bash
   docker-compose up --build
   ```

3. **Access the application:**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - eXist-db: http://localhost:8081
   - PostgreSQL: localhost:5432

### First Time Setup

The application will automatically:
- Create the PostgreSQL database and tables
- Initialize the eXist-db collection
- Start all services in the correct order

## Services

### Frontend (Vue 3 + TypeScript)
- **Port**: 3000
- **Features**: 
  - Monaco Editor with XML syntax highlighting
  - Document management interface
  - Real-time validation
  - Responsive design

### Backend (Spring Boot)
- **Port**: 8080
- **Features**:
  - RESTful API for XML document operations
  - XML validation
  - Integration with both PostgreSQL and eXist-db
  - CORS enabled for frontend communication

### PostgreSQL
- **Port**: 5432
- **Purpose**: Stores document metadata (name, timestamps, eXist-db references)
- **Database**: webdws
- **Credentials**: webdws/webdws

### eXist-db
- **Port**: 8081
- **Purpose**: Stores actual XML document content
- **Collection**: /db/webdws
- **Credentials**: admin/<empty>>

## API Endpoints

### Documents
- `GET /api/xml/documents` - List all documents (with pagination)
- `GET /api/xml/documents/{id}` - Get specific document
- `POST /api/xml/documents` - Create new document
- `PUT /api/xml/documents/{id}` - Update document
- `DELETE /api/xml/documents/{id}` - Delete document

### Validation
- `POST /api/xml/validate` - Validate XML content

### Health
- `GET /api/xml/health` - Health check

## Development

### Frontend Development

```bash
cd frontend
npm install
npm run dev
```

### Backend Development

```bash
cd backend
./mvnw spring-boot:run
```

### Database Management

**PostgreSQL:**
```bash
docker-compose exec postgres psql -U webdws -d webdws
```

**eXist-db:**
- Web interface: http://localhost:8081
- Username: admin
- Password: 

## Configuration

### Environment Variables

**Backend:**
- `EXIST_DB_URL`: eXist-db connection URL
- `EXIST_DB_USER`: eXist-db username
- `EXIST_DB_PASSWORD`: eXist-db password

**Frontend:**
- `VITE_API_BASE_URL`: Backend API URL

### Docker Compose

The `docker-compose.yml` file configures:
- Service dependencies
- Network communication
- Volume persistence
- Port mappings
- Environment variables

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 3000, 8080, 8081, and 5432 are available
2. **Database connection**: Wait for PostgreSQL and eXist-db to fully start before accessing the application
3. **CORS issues**: The backend is configured to allow all origins in development

### Logs

View logs for specific services:
```bash
docker-compose logs frontend
docker-compose logs backend
docker-compose logs postgres
docker-compose logs exist-db
```

### Reset Everything

To start fresh:
```bash
docker-compose down -v
docker-compose up --build
```

## Project Structure

```
webdws/
├── docker-compose.yml          # Docker Compose configuration
├── frontend/                   # Vue 3 frontend
│   ├── src/
│   │   ├── components/         # Vue components
│   │   ├── views/             # Page views
│   │   ├── services/          # API services
│   │   ├── types/             # TypeScript types
│   │   └── main.ts            # App entry point
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
├── backend/                    # Spring Boot backend
│   ├── src/main/java/com/webdws/
│   │   ├── controller/        # REST controllers
│   │   ├── service/           # Business logic
│   │   ├── repository/        # Data access
│   │   ├── model/             # Entity models
│   │   ├── dto/               # Data transfer objects
│   │   └── config/            # Configuration
│   ├── src/main/resources/
│   │   └── application.yml     # Application configuration
│   ├── pom.xml
│   └── Dockerfile
└── README.md
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with Docker Compose
5. Submit a pull request

## License

This project is licensed under the MIT License.
