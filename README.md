# 📄 Spring Boot RAG Application (In-Memory Vector Store)

## 📌 Overview

This project is a Retrieval-Augmented Generation (RAG) application built using Spring Boot.

The system allows users to:

1. Upload a PDF document
2. Ask questions about the document
3. Receive AI-generated answers based only on the uploaded content

The application implements a simplified RAG pipeline using an in-memory vector store and cosine similarity search.

---

## 🧠 How It Works

The system follows a two-phase workflow:

### 1️⃣ Document Ingestion (Upload Phase)

- Extract text from uploaded PDF
- Split text into smaller chunks
- Convert each chunk into a numeric embedding vector
- Store embeddings in memory

### 2️⃣ Question Answering (Retrieval + Generation)

- Convert user question into embedding
- Compare question embedding with stored vectors
- Retrieve Top-K most similar chunks
- Build a prompt using retrieved context
- Send prompt to LLM
- Return generated answer

---

## 🏗️ Architecture

Controller Layer  
⬇  
Service Layer  
⬇  
Embedding + Vector Store  
⬇  
LLM

The project is structured using clean separation of concerns.

---

## 📂 Project Structure & Purpose

### 🔹 `RagController`
Handles REST endpoints:
- `POST /upload` – Upload PDF
- `POST /ask` – Ask question

Acts as entry point for client requests.

---

### 🔹 `PdfService`
- Extracts text from PDF
- Splits text into chunks

Responsible for document preprocessing.

---

### 🔹 `EmbeddingService`
- Converts text into embedding vectors
- Currently generates mock embeddings (random float[768])

In production, this should call a real embedding API.

---

### 🔹 `VectorStoreService`
- Stores chunk content + embeddings in memory
- Performs cosine similarity search
- Returns Top-K similar chunks

Implements a simple in-memory vector database.

---

### 🔹 `RetrieverService`
- Converts query to embedding
- Calls vector store search
- Returns relevant chunks

Separates retrieval logic from storage logic.

---

### 🔹 `RagService`
Orchestrates:
- Upload workflow
- Retrieval workflow
- Prompt construction
- LLM interaction

Contains main business logic.

---

## 🧮 Similarity Algorithm

The application uses **Cosine Similarity** to compare vectors:

Similarity = dot(v1, v2) / (||v1|| * ||v2||)

Search is implemented using brute-force comparison across all stored vectors.

Time Complexity per query:
O(n log n) (due to sorting)

---

## ⚙️ Current Limitations

- In-memory storage (data lost on restart)
- No document ID separation
- Mock embeddings (not semantic)
- No similarity threshold filtering
- Linear search (not optimized for large datasets)

---

## 🚀 Future Improvements

- Integrate real embedding model (OpenAI / HuggingFace)
- Replace in-memory store with vector database (Qdrant / Pinecone / Milvus)
- Add document metadata & document IDs
- Implement similarity threshold
- Add persistent storage
- Improve chunking with overlap

---

## 🛠️ Tech Stack

- Java 17+
- Spring Boot
- REST APIs
- Cosine Similarity (custom implementation)
- LLM integration

---

## ▶️ How to Run

1. Clone repository
2. Start Spring Boot application
3. Use Postman:
    - `POST /upload` → Upload PDF
    - `POST /ask` → Ask question

---

## 📌 Design Decision

This project intentionally uses an in-memory vector store to:

- Understand RAG internals
- Implement similarity search from scratch
- Learn system architecture before scaling

It is designed as a learning and architectural demonstration project.

---

## 👨‍💻 Author

Robin Mathew  
Software Developer
