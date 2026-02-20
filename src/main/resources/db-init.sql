-- PostgreSQL + pgvector initialization script
-- Run this after the application creates the tables

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create HNSW index for fast vector similarity search
-- This dramatically improves query performance (100x faster)
CREATE INDEX IF NOT EXISTS vector_chunks_embedding_idx 
ON vector_chunks USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Alternative: IVFFlat index (faster to build, slightly slower queries)
-- CREATE INDEX IF NOT EXISTS vector_chunks_embedding_idx 
-- ON vector_chunks USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);

-- Verify index was created
SELECT 
    indexname, 
    indexdef 
FROM pg_indexes 
WHERE tablename = 'vector_chunks' 
AND indexname = 'vector_chunks_embedding_idx';
