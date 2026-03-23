# RAG API — Document Intelligence System

A production-ready **Retrieval-Augmented Generation (RAG)** API built with Spring Boot. Upload PDF documents and ask natural language questions — the system finds the most relevant content and generates accurate answers using AI.

---

## How It Works

```
PDF Upload → Text Extraction → Chunking → Embedding (Ollama) → PostgreSQL + pgvector
                                                                          ↓
User Question → Embedding → Similarity Search → Top Chunks → Groq LLaMA → Answer
```

1. You upload a PDF
2. Text is extracted and split into overlapping chunks
3. Each chunk is converted to a 768-dimensional vector using Ollama (`nomic-embed-text`)
4. Vectors are stored in PostgreSQL with the pgvector extension
5. When you ask a question, it's also embedded and compared against stored vectors using cosine similarity
6. The most relevant chunks are passed to Groq's LLaMA model to generate a natural language answer

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 3.5.7 |
| Language | Java 17 |
| Database | PostgreSQL 16 + pgvector |
| Embeddings | Ollama (`nomic-embed-text`, 768-dim) |
| LLM | Groq API (`llama-3.1-8b-instant`) |
| PDF Parsing | Apache PDFBox |
| Containerization | Docker |

---

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker Desktop
- Groq API Key (free at [console.groq.com](https://console.groq.com))

---

## Setup & Run

### 1. Start PostgreSQL with pgvector

```powershell
docker run -d --name rag-postgres `
  -e POSTGRES_PASSWORD=postgres `
  -e POSTGRES_DB=rag_db `
  -p 5432:5432 `
  pgvector/pgvector:pg16

docker exec -it rag-postgres psql -U postgres -d rag_db -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 2. Start Ollama with embedding model

```powershell
docker run -d --name ollama -p 11434:11434 ollama/ollama
docker exec -it ollama ollama pull nomic-embed-text
```

### 3. Set environment variables

```powershell
$env:GROQ_API_KEY = "your_groq_api_key_here"
```

### 4. Run the application

```powershell
mvn spring-boot:run
```

App starts at `http://localhost:8080`

---

## API Endpoints

Base URL: `http://localhost:8080/api/rag`

### Health Check
```
GET /health
```
Returns service status, total documents, and total chunks.

**Response:**
```json
{
  "status": "UP",
  "service": "RAG Service",
  "totalDocuments": 3,
  "totalChunks": 87
}
```

---

### Upload PDF
```
POST /upload
Content-Type: multipart/form-data
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | File | Yes | PDF file (max 100MB) |

**Response:**
```json
{
  "message": "PDF processed successfully",
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "report.pdf",
  "chunkCount": 32
}
```

---

### Ask a Question
```
POST /ask
```

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| question | String | Yes | — | Your question |
| documentId | String | No | null (all docs) | Search in specific document |
| topK | int | No | 3 | Number of chunks to retrieve |
| threshold | double | No | 0.3 | Minimum similarity score (0.0–1.0) |

**Example:**
```
POST /ask?question=What are the heart chambers?&topK=5&threshold=0.2
```

**Response:**
```json
{
  "question": "What are the heart chambers?",
  "answer": "The heart has four chambers: the left atrium, right atrium, left ventricle, and right ventricle...",
  "documentId": "all",
  "topK": 5,
  "threshold": 0.2
}
```

**Tips:**
- Lower `threshold` (e.g. `0.1`) = more results, less precise
- Higher `topK` (e.g. `8`) = more context = better answers
- Specify `documentId` to search only within one document

---

### Simple Ask (Legacy)
```
POST /ask-simple
Content-Type: text/plain
Body: your question here
```

---

### List Documents
```
GET /documents
```

**Response:**
```json
{
  "documents": ["uuid-1", "uuid-2"],
  "count": 2,
  "totalChunks": 87
}
```

---

### Get Document Info
```
GET /documents/{documentId}
```

**Response:**
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "chunkCount": 32
}
```

---

### Delete Document
```
DELETE /documents/{documentId}
```

Removes the document and all its vector chunks from the database.

---

### Clear All Data
```
DELETE /clear
```

Deletes all documents and vector chunks. Use with caution.

---

## Project Structure

```
src/main/java/com/enterprise_wrapper_api/wrapper_api/
├── WrapperApiApplication.java          # Spring Boot entry point
└── rag/
    ├── RagController.java              # Main REST API (upload, ask, list, delete)
    ├── LlamaController.java            # Legacy simple ask endpoint
    ├── RagService.java                 # Core orchestration logic
    ├── EmbeddingService.java           # Calls Ollama to generate vector embeddings
    ├── VectorStoreService.java         # Stores/searches vectors in PostgreSQL
    ├── RetrieverService.java           # Converts query to embedding + runs search
    ├── LlamaClient.java                # Calls Groq API for answer generation
    ├── PdfService.java                 # Extracts text and chunks PDFs
    ├── TextChunkService.java           # Simple word-based text splitter (utility)
    ├── LocalEmbeddingService.java      # Fallback local embedder (not used in prod)
    ├── entity/
    │   ├── Document.java               # JPA entity for document metadata
    │   └── VectorChunk.java            # JPA entity for text chunks + embeddings
    ├── repository/
    │   ├── DocumentRepository.java     # JPA queries for documents table
    │   └── VectorChunkRepository.java  # JPA + native pgvector similarity queries
    └── exception/
        ├── RagException.java           # Base application exception
        ├── DocumentNotFoundException.java  # Thrown when document ID not found
        ├── InvalidFileException.java   # Thrown for bad file uploads
        └── GlobalExceptionHandler.java # Maps exceptions to HTTP responses
```

---

## Code Walkthrough

### `RagController.java`
The REST layer. Handles all HTTP requests and delegates to `RagService`. Exposes endpoints for upload, ask, list, delete, clear, and health check.

### `RagService.java`
The brain of the application. Orchestrates the full pipeline:
- On upload: extract text → chunk → embed each chunk → store in DB
- On question: embed question → retrieve similar chunks → build prompt → call LLM → return answer

### `EmbeddingService.java`
Calls the local Ollama API (`http://localhost:11434/api/embeddings`) with the `nomic-embed-text` model. Converts any text string into a 768-dimensional float vector.

### `VectorStoreService.java`
Manages all database operations:
- Stores document metadata and vector chunks
- Runs cosine similarity search using pgvector's `<=>` operator
- Filters results by similarity threshold
- Handles CRUD for documents and chunks

### `RetrieverService.java`
Bridge between the question and the vector store. Embeds the query using `EmbeddingService`, then calls `VectorStoreService.search()` to find the most similar chunks.

### `LlamaClient.java`
HTTP client for Groq's API. Sends a prompt to `llama-3.1-8b-instant` and returns the generated text. Uses low temperature (0.2) for factual, consistent answers.

### `PdfService.java`
Handles PDF processing:
- Validates file (type, size, non-empty)
- Extracts raw text using Apache PDFBox
- Splits text into overlapping semantic chunks (1000 chars, 200 overlap) at sentence boundaries

### `TextChunkService.java`
Simple utility that splits text by word count. Used as a fallback or for basic chunking needs.

### `Document.java`
JPA entity mapped to the `documents` table. Stores document ID (UUID), filename, upload timestamp, and chunk count.

### `VectorChunk.java`
JPA entity mapped to the `vector_chunks` table. Stores the text content, its `vector(768)` embedding, chunk index, and a foreign key to the parent document.

### `DocumentRepository.java`
Spring Data JPA repository for the `documents` table. Provides find, exists, and delete by `documentId`.

### `VectorChunkRepository.java`
JPA repository with native SQL queries for pgvector similarity search using the `<=>` (cosine distance) operator. Supports both global search and per-document search.

### `GlobalExceptionHandler.java`
`@ControllerAdvice` that catches `RagException`, `DocumentNotFoundException`, `InvalidFileException`, and generic exceptions, returning structured JSON error responses with appropriate HTTP status codes.

---

## Database Schema

```sql
CREATE TABLE documents (
    document_id VARCHAR(36) PRIMARY KEY,
    filename    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    chunk_count INTEGER
);

CREATE TABLE vector_chunks (
    id          BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(36) REFERENCES documents(document_id),
    content     TEXT NOT NULL,
    embedding   vector(768) NOT NULL,
    chunk_index INTEGER NOT NULL,
    created_at  TIMESTAMP NOT NULL
);
```

---

## Configuration

`src/main/resources/application.properties`

```properties
server.port=8080
server.servlet.context-path=/api

# Groq LLM
groq.api.key=${GROQ_API_KEY}
groq.api.base-url=https://api.groq.com

# Ollama Embeddings
ollama.base-url=http://localhost:11434

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/rag_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# File Upload
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

---

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `Connection refused` on port 8080 | App not running | Run `mvn spring-boot:run` |
| `password authentication failed` | DB not running | Start Docker and `rag-postgres` container |
| `404 from POST /api/embeddings` | Ollama model not pulled | Run `docker exec -it ollama ollama pull nomic-embed-text` |
| `expected X dimensions, not Y` | Vector dimension mismatch | Drop and recreate `vector_chunks` table |
| `No relevant information found` | Low similarity or wrong document | Lower `threshold` or increase `topK` |
| `401 Unauthorized` from Groq | Missing API key | Set `$env:GROQ_API_KEY` |
