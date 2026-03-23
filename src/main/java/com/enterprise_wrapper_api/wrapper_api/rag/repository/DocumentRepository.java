package com.enterprise_wrapper_api.wrapper_api.rag.repository;

import com.enterprise_wrapper_api.wrapper_api.rag.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    
    Optional<Document> findByDocumentId(String documentId);
    
    boolean existsByDocumentId(String documentId);
    
    void deleteByDocumentId(String documentId);
}
