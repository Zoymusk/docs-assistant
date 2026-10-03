package com.example.docs_assistant;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

@RestController
public class AskController {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public AskController(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        this.chatClient = builder.build();
    }

    @GetMapping(value = "/ask", produces = "text/plain;charset=UTF-8")
    public String ask(@RequestParam String q) {
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder().query(q).topK(8).build());

        String context = docs.stream()
            .map(d -> "[" + d.getMetadata().get("source") + "]\n" + d.getText())
            .collect(Collectors.joining("\n\n---\n\n"));

        String answer = chatClient.prompt()
            .system("You answer questions about Spring Boot using ONLY the context provided. "
                  + "If the context does not contain the answer, say you don't know. Be concise.")
            .user("Context:\n" + context + "\n\nQuestion: " + q)
            .call()
            .content();

        String sources = docs.stream()
            .map(d -> String.valueOf(d.getMetadata().get("source")))
            .distinct()
            .collect(Collectors.joining("\n- ", "\n\nSources:\n- ", ""));

        return answer + sources;
    }
}
