# 🚀 Wrapper API – Resume AI & RAG-Based Document Assistant

## 📌 Overview

Wrapper API is a Spring Boot application that provides:

1. Resume–Job Description matching using AI
2. Retrieval-Augmented Generation (RAG) for PDF-based question answering
3. LLM integration (Llama-based)
4. Vector search (in-memory + Pinecone-ready architecture)

The project demonstrates clean architecture, modular RAG implementation, and AI-powered document processing.

---

# 🧠 Core Features

## 1️⃣ Resume Matching API
- Accepts resume + job description
- Uses AI to evaluate match quality
- Returns structured match response

## 2️⃣ RAG (Retrieval-Augmented Generation)
- Upload PDF
- Ask questions about uploaded document
- Retrieves most relevant chunks
- Generates AI-based contextual answer

---

# 🏗️ Architecture Overview

The application follows layered architecture:

Controller Layer
⬇
Service Layer
⬇
RAG Layer (Embedding + Vector Search + Retrieval)
⬇
LLM Client
⬇
Response

---

# 📂 Project Structure & Purpose

## 🔹 `WrapperApiApplication`
Main Spring Boot entry point.

---

# 📁 Controller Layer

## 🔹 `MatchController`
Handles resume matching endpoints.

Purpose:
- Accept ResumeMatchRequest
- Call matching service
- Return ResumeMatchResponse

---

## 🔹 `RagController`
Handles:
- `POST /upload`
- `POST /ask`

Responsible for RAG operations.

---

## 🔹 `LlamaController`
Handles direct interaction with LLM endpoints.

---

# 📁 Model Layer

## 🔹 `ResumeMatchRequest`
Request DTO for resume matching.

## 🔹 `ResumeMatchResponse`
Structured AI response for match result.

---

# 📁 Service Layer

## 🔹 `AiMatcherService`
Interface for resume matching logic.

## 🔹 `ResumeMatchService` (impl)
Implements matching workflow using AI.

---

# 📁 RAG Module (`rag` package)

This is the most architecturally important part.

---

## 🔹 `PdfService`
- Extracts text from PDF
- Converts document into raw text

---

## 🔹 `TextChunkService`
- Splits large text into smaller chunks
- Improves retrieval precision

---

## 🔹 `EmbeddingService`
- Converts text into embedding vectors
- Currently supports mock embedding (float[768])
- Designed for real embedding API integration

---

## 🔹 `VectorStoreService`
In-memory vector database.

Stores:
- Text chunk
- Embedding vector

Implements:
- Cosine similarity search
- Top-K retrieval

Time Complexity:
O(n log n) per query (brute-force search)

---

## 🔹 `VectorDatabaseClient`
Abstraction layer for vector database.

Currently:
- Can connect to in-memory store
- Pinecone-ready structure

This allows easy future migration to external vector DB.

---

## 🔹 `PineconeClient`
Prepared client for Pinecone integration.

Enables:
- Scalable vector storage
- Approximate nearest neighbor search

---

## 🔹 `RetrieverService`
Handles:
1. Convert query → embedding
2. Search vector database
3. Return top-K relevant chunks

Separates retrieval logic from storage logic.

---

## 🔹 `RagService`
Orchestrates full RAG workflow:

Upload Flow:
- Extract
- Chunk
- Embed
- Store

Ask Flow:
- Retrieve relevant chunks
- Build prompt
- Call LLM
- Return AI answer

This is the core business orchestration layer.

---

## 🔹 `LlamaClient`
Responsible for communicating with LLM (Llama-based).

Handles:
- Prompt submission
- Response parsing

---

# 🧮 Retrieval Logic

The system uses **Cosine Similarity** to compare embeddings.

Similarity Formula:

cosine(v1, v2) = dot(v1, v2) / (||v1|| * ||v2||)

Top-K highest similarity chunks are selected for context generation.

---

# ⚙️ Current Implementation Mode

✔ In-Memory Vector Store
✔ Mock Embeddings (for learning/demo)
✔ Modular RAG architecture
✔ Pinecone-ready abstraction

---

# ⚠️ Limitations

- In-memory storage (lost on restart)
- No document ID isolation (unless implemented)
- Brute-force search
- No similarity threshold filtering
- No persistent storage

---

# 🚀 Future Improvements

- Integrate real embedding model (OpenAI / HuggingFace)
- Enable full Pinecone vector DB integration
- Add metadata filtering
- Implement similarity threshold
- Add persistent database
- Add authentication & multi-user support

---

# 🛠️ Tech Stack

- Java 17+
- Spring Boot
- REST APIs
- Cosine Similarity
- LLM (Llama-based)
- Pinecone (optional)
- Maven

---

# ▶️ Running the Application

1. Clone the repository
2. Configure application properties
3. Run:
   mvn spring-boot:run

4. Test APIs using Postman

---

# 📌 Design Philosophy

This project is designed to:

- Demonstrate end-to-end RAG architecture
- Show clean separation of concerns
- Provide AI-powered document analysis
- Be scalable toward production-ready vector DB integration

---
