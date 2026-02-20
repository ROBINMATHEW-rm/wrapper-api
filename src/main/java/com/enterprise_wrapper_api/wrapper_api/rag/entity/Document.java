package com.enterprise_wrapper_api.wrapper_api.rag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @Column(name = "document_id", length = 36)
    private String documentId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VectorChunk> chunks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
