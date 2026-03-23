package com.enterprise_wrapper_api.wrapper_api.rag.repository;

import com.enterprise_wrapper_api.wrapper_api.rag.entity.VectorChunk;
import com.pgvector.PGvector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorChunkRepository extends JpaRepository<VectorChunk, Long> {
    
    List<VectorChunk> findByDocument_DocumentId(String documentId);
    
    @Query("SELECT COUNT(v) FROM VectorChunk v WHERE v.document.documentId = :documentId")
    int countByDocumentId(@Param("documentId") String documentId);
    
    @Query("SELECT v FROM VectorChunk v WHERE v.document.documentId = :documentId")
    List<VectorChunk> findAllByDocumentId(@Param("documentId") String documentId);
    
    void deleteByDocument_DocumentId(String documentId);
    
    // Native query for vector similarity search using pgvector
    @Query(value = "SELECT * FROM vector_chunks " +
                   "ORDER BY embedding <=> CAST(:queryEmbedding AS vector) " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<VectorChunk> findNearestNeighbors(@Param("queryEmbedding") String queryEmbedding, 
                                           @Param("limit") int limit);
    
    // Vector similarity search for specific document
    @Query(value = "SELECT * FROM vector_chunks " +
                   "WHERE document_id = :documentId " +
                   "ORDER BY embedding <=> CAST(:queryEmbedding AS vector) " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<VectorChunk> findNearestNeighborsByDocument(@Param("queryEmbedding") String queryEmbedding,
                                                     @Param("documentId") String documentId,
                                                     @Param("limit") int limit);
}
