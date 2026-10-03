package com.example.docs_assistant;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HybridSearch {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;

    public HybridSearch(VectorStore vectorStore, JdbcTemplate jdbc) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
    }

    public List<Document> search(String q, int k) {
        List<Document> vec = vectorStore.similaritySearch(
            SearchRequest.builder().query(q).topK(20).build());
        List<Document> kw = keyword(q, 20);

        Map<String, Double> scores = new HashMap<>();
        Map<String, Document> byId = new HashMap<>();
        fuse(vec, scores, byId, 1.0);
        fuse(kw, scores, byId, 0.5);

        return scores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(k)
            .map(e -> byId.get(e.getKey()))
            .toList();
    }

    private void fuse(List<Document> docs, Map<String, Double> scores, Map<String, Document> byId, double w) {
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            scores.merge(d.getId(), w / (60 + i + 1), Double::sum);
            byId.putIfAbsent(d.getId(), d);
        }
    }

    private List<Document> keyword(String q, int n) {
        String tsq = Arrays.stream(q.toLowerCase().replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
            .filter(w -> w.length() > 2)
            .distinct()
            .collect(Collectors.joining(" | "));
        if (tsq.isBlank()) return List.of();
        return jdbc.query(
            "SELECT id::text AS id, content, metadata->>'source' AS source FROM vector_store "
          + "WHERE to_tsvector('english', content) @@ to_tsquery('english', ?) "
          + "ORDER BY ts_rank(to_tsvector('english', content), to_tsquery('english', ?)) DESC LIMIT ?",
            (rs, i) -> Document.builder()
                .id(rs.getString("id"))
                .text(rs.getString("content"))
                .metadata(Map.<String, Object>of("source", String.valueOf(rs.getString("source"))))
                .build(),
            tsq, tsq, n);
    }
}

