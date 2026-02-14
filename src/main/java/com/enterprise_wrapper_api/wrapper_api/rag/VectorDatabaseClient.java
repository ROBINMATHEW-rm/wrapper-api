package com.enterprise_wrapper_api.wrapper_api.rag;
import java.util.List;

public interface VectorDatabaseClient {
    void save(String id, float[] vector, String text);
    List<String> search(float[] queryVector, int topK);
}
