package com.example.docs_assistant;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {

    private final VectorStore vectorStore;

    public TestController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam String q) {
        return vectorStore.similaritySearch(q).stream()
            .map(d -> d.getMetadata().get("source") + " :: " + d.getText())
            .toList();
    }

    @GetMapping("/ingest-docs")
    public String ingestDocs() throws IOException {
        Path root = Paths.get("C:/Users/ADMIN/Downloads/spring-boot/documentation/spring-boot-docs/src/docs/antora/modules");
        List<Document> docs = new ArrayList<>();
        for (String module : List.of("reference", "how-to")) {
            Path pages = root.resolve(module).resolve("pages");
            try (Stream<Path> files = Files.walk(pages)) {
                for (Path p : files.filter(f -> f.toString().endsWith(".adoc")).toList()) {
                    String source = module + "/" + pages.relativize(p).toString().replace("\\", "/");
                    docs.add(new Document(Files.readString(p), Map.of("source", source)));
                }
            }
        }
        List<Document> chunks = new TokenTextSplitter().apply(docs);
        for (int i = 0; i < chunks.size(); i += 100) {
            vectorStore.add(chunks.subList(i, Math.min(i + 100, chunks.size())));
        }
        return "files: " + docs.size() + ", chunks: " + chunks.size();
    }
}