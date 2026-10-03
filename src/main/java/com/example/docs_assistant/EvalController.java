package com.example.docs_assistant;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

@RestController
public class EvalController {

    private final VectorStore vectorStore;
    private final HybridSearch hybrid;

    public EvalController(VectorStore vectorStore, HybridSearch hybrid) {
        this.vectorStore = vectorStore;
        this.hybrid = hybrid;
    }

    @GetMapping(value = "/eval", produces = "text/plain;charset=UTF-8")
    public String eval(@RequestParam(defaultValue = "8") int k,
                       @RequestParam(defaultValue = "vector") String mode) throws Exception {
        String content = new String(new ClassPathResource("eval.txt").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<String> lines = content.lines().filter(l -> l.contains("|")).toList();

        StringBuilder out = new StringBuilder();
        int total = 0, h1 = 0, h3 = 0, hk = 0;

        for (String line : lines) {
            String[] parts = line.split("\\|");
            String q = parts[0].trim();
            String expected = parts[1].trim();

            List<Document> docs = mode.equals("hybrid")
                ? hybrid.search(q, k)
                : vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(k).build());

            int rank = 0;
            for (int i = 0; i < docs.size(); i++) {
                if (expected.equals(String.valueOf(docs.get(i).getMetadata().get("source")))) {
                    rank = i + 1;
                    break;
                }
            }

            total++;
            if (rank == 1) h1++;
            if (rank >= 1 && rank <= 3) h3++;
            if (rank >= 1) hk++;

            if (rank == 0) {
                out.append("MISS: ").append(q).append("\n   expected ").append(expected).append("\n   got: ");
                docs.stream().map(d -> String.valueOf(d.getMetadata().get("source"))).distinct()
                    .forEach(s -> out.append(s).append(" "));
                out.append("\n");
            }
        }

        String summary = String.format(
            "Mode: %s%nQuestions: %d%nHit@1: %d (%.0f%%)%nHit@3: %d (%.0f%%)%nHit@%d: %d (%.0f%%)%n%n",
            mode, total, h1, 100.0 * h1 / total, h3, 100.0 * h3 / total, k, hk, 100.0 * hk / total);
        return summary + out;
    }
}
