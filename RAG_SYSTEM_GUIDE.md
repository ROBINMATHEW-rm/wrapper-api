# RAG System - Complete Guide

## 📚 Table of Contents
1. [What is This System?](#what-is-this-system)
2. [How Does It Work?](#how-does-it-work)
3. [Key Features](#key-features)
4. [API Endpoints](#api-endpoints)
5. [Technical Architecture](#technical-architecture)
6. [Setup & Configuration](#setup--configuration)
7. [Usage Examples](#usage-examples)
8. [Troubleshooting](#troubleshooting)

---

## What is This System?

### For Non-Technical Users 👥

Imagine you have a **smart filing cabinet** that can:
- Store thousands of PDF documents
- Instantly find relevant information from those documents
- Answer your questions using the information it finds
- Remember everything you've uploaded

This system is like having a **super-smart assistant** that has read all your documents and can answer questions about them in seconds.

### For Technical Users 💻

This is a **RAG (Retrieval-Augmented Generation)** system that:
- Extracts text from PDF documents
- Converts text into vector embeddings using Groq's embedding model
- Stores embeddings in PostgreSQL with pgvector extension
- Performs semantic search to find relevant content
- Uses Groq's LLM to generate contextual answers

---

## How Does It Work?

### Simple Explanation 🎯

**Step 1: Upload Documents**
- You upload a PDF file
- The system reads the PDF and breaks it into small chunks (like paragraphs)
- Each chunk is converted into a special code (called an "embedding")
- These codes are stored in a database

**Step 2: Ask Questions**
- You type a question
- The system converts your question into the same type of code
- It finds the chunks that are most similar to your question
- It reads those chunks and generates an answer

**Step 3: Get Answers**
- The system combines the relevant information
- Uses AI to write a clear, accurate answer
- Returns the answer to you

### Technical Flow 🔧

```
PDF Upload → Text Extraction → Chunking → Embedding Generation → Vector Storage
                                                                         ↓
User Query → Query Embedding → Similarity Search → Context Retrieval → LLM Generation → Answer
```

---

## Key Features

### ✅ What This System Can Do

1. **Multi-Document Support**
   - Upload multiple PDF files
   - Each document is tracked separately
   - Search across all documents or specific ones

2. **Smart Chunking**
   - Breaks documents at sentence boundaries
   - Maintains context and readability
   - Configurable chunk size (default: 500 characters)

3. **Semantic Search**
   - Finds relevant content based on meaning, not just keywords
   - Uses cosine similarity for accurate matching
   - Configurable similarity threshold (default: 0.3)

4. **Real Embeddings**
   - Uses Groq's `nomic-embed-text` model (768 dimensions)
   - Production-ready vector representations
   - Fast and accurate similarity matching

5. **Production Features**
   - Error handling and validation
   - Document metadata tracking
   - Chunk counting and statistics
   - Database persistence with PostgreSQL + pgvector

---

## API Endpoints

### 1. Upload PDF Document
**Endpoint:** `POST /api/rag/upload`

**What it does:** Uploads a PDF and stores it in the system

**Request:**
```bash
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@document.pdf"
```

**Response:**
```json
{
  "message": "Document uploaded successfully",
  "documentId": "abc-123-def-456",
  "chunksStored": 25
}
```

---

### 2. Ask a Question
**Endpoint:** `POST /api/rag/ask`

**What it does:** Answers your question using uploaded documents

**Request:**
```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the main topic of the document?",
    "documentId": "abc-123-def-456",
    "topK": 5,
    "threshold": 0.3
  }'
```

**Parameters:**
- `question` (required): Your question
- `documentId` (optional): Search only in this document
- `topK` (optional): Number of relevant chunks to retrieve (default: 5)
- `threshold` (optional): Minimum similarity score (default: 0.3)

**Response:**
```json
{
  "answer": "The main topic of the document is...",
  "sources": [
    "Relevant chunk 1...",
    "Relevant chunk 2..."
  ]
}
```

---

### 3. Simple Question (Legacy)
**Endpoint:** `POST /api/rag/ask-simple`

**What it does:** Simpler version with just a question

**Request:**
```bash
curl -X POST http://localhost:8080/api/rag/ask-simple \
  -H "Content-Type: text/plain" \
  -d "What is the main topic?"
```

---

### 4. List All Documents
**Endpoint:** `GET /api/rag/documents`

**What it does:** Shows all uploaded documents

**Request:**
```bash
curl http://localhost:8080/api/rag/documents
```

**Response:**
```json
{
  "documents": [
    {
      "documentId": "abc-123",
      "filename": "report.pdf",
      "chunkCount": 25,
      "uploadedAt": "2026-02-20T10:30:00"
    }
  ]
}
```

---

### 5. Delete Document
**Endpoint:** `DELETE /api/rag/documents/{documentId}`

**What it does:** Removes a document and all its data

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/rag/documents/abc-123
```

---

### 6. Clear All Data
**Endpoint:** `DELETE /api/rag/clear`

**What it does:** Deletes everything (use with caution!)

**Request:**
```bash
curl -X DELETE http://localhost:8080/api/rag/clear
```

---

## Technical Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Spring Boot Application                  │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ RagController│  │ RagService   │  │ PdfService   │      │
│  │ (REST API)   │→ │ (Orchestr.)  │→ │ (Extract)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                  ↓                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Embedding    │  │ VectorStore  │  │ Retriever    │      │
│  │ Service      │  │ Service      │  │ Service      │      │
│  │ (Groq API)   │  │ (PostgreSQL) │  │ (Search)     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         ↓                  ↓                  ↓              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ LlamaClient  │  │ JPA Repos    │  │ TextChunk    │      │
│  │ (Groq LLM)   │  │ (Database)   │  │ Service      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL + pgvector (Docker)                  │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ documents table  │  │ vector_chunks    │                │
│  │ - document_id    │  │ - content        │                │
│  │ - filename       │  │ - embedding      │                │
│  │ - chunk_count    │  │ - chunk_index    │                │
│  └──────────────────┘  └──────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### Database Schema

**documents table:**
```sql
CREATE TABLE documents (
    document_id VARCHAR(36) PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    chunk_count INTEGER
);
```

**vector_chunks table:**
```sql
CREATE TABLE vector_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(36) REFERENCES documents(document_id),
    content TEXT NOT NULL,
    embedding vector(768) NOT NULL,
    chunk_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_document_id ON vector_chunks(document_id);
CREATE INDEX idx_chunk_index ON vector_chunks(chunk_index);
```

### Key Technologies

1. **Spring Boot 3.5.7** - Application framework
2. **PostgreSQL 16** - Database
3. **pgvector** - Vector similarity search extension
4. **Groq API** - Embeddings and LLM
5. **Apache PDFBox** - PDF text extraction
6. **Hibernate/JPA** - Database ORM
7. **Docker** - PostgreSQL containerization

---

## Setup & Configuration

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker Desktop
- Groq API Key

### Environment Variables

```properties
GROQ_API_KEY=your_groq_api_key_here
GROQ_API_URL=https://api.groq.com/openai/v1
```

### Database Configuration

The system uses PostgreSQL with pgvector running in Docker:

```bash
docker run -d --name rag-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=rag_db \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

### Application Properties

Located in `src/main/resources/application.properties`:

```properties
# Server
server.port=8080
server.servlet.context-path=/api

# Groq API
groq.api.key=${GROQ_API_KEY}
groq.api.url=${GROQ_API_URL}

# File Upload
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/rag_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## Usage Examples

### Example 1: Upload and Query

```bash
# 1. Upload a PDF
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@company-report.pdf"

# Response: {"documentId": "abc-123", "chunksStored": 30}

# 2. Ask a question
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What were the Q4 revenue figures?",
    "documentId": "abc-123"
  }'
```

### Example 2: Search Across All Documents

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the key findings?",
    "topK": 10
  }'
```

### Example 3: Adjust Similarity Threshold

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Explain the methodology",
    "threshold": 0.5,
    "topK": 3
  }'
```

---

## Troubleshooting

### Common Issues

**1. Database Connection Failed**
```
Error: password authentication failed for user "postgres"
```
**Solution:** Stop local PostgreSQL service and use Docker container only.

**2. Port Already in Use**
```
Error: Port 5432 is already in use
```
**Solution:** Stop local PostgreSQL or use different port for Docker.

**3. Groq API Error**
```
Error: 401 Unauthorized
```
**Solution:** Check GROQ_API_KEY environment variable is set correctly.

**4. PDF Upload Failed**
```
Error: File size exceeds maximum
```
**Solution:** Check `spring.servlet.multipart.max-file-size` setting.

### Performance Tips

1. **Chunk Size:** Adjust in `TextChunkService` (default: 500 chars)
2. **Similarity Threshold:** Lower = more results, higher = more precise
3. **Top K:** More chunks = better context but slower
4. **Database Index:** Create HNSW index for faster searches:
   ```sql
   CREATE INDEX ON vector_chunks USING hnsw (embedding vector_cosine_ops);
   ```

---

## What's Next?

### Recommended Improvements

1. **Add Authentication** - Secure the API endpoints
2. **Add Caching** - Cache embeddings and search results
3. **Add Analytics** - Track usage and performance
4. **Add UI** - Build a web interface
5. **Add More File Types** - Support DOCX, TXT, etc.
6. **Add Batch Processing** - Upload multiple files at once

---

## Summary

This RAG system provides a production-ready solution for:
- ✅ Document storage and retrieval
- ✅ Semantic search with vector embeddings
- ✅ AI-powered question answering
- ✅ Multi-document support
- ✅ RESTful API interface

The system is now fully operational with PostgreSQL + pgvector for vector storage and Groq API for embeddings and LLM generation.
