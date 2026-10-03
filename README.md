# Docs Assistant

A RAG (retrieval-augmented generation) app that answers Spring Boot questions using the official Spring Boot docs, and shows which doc pages each answer came from.

## How it works

1. **Ingest**: loads 117 Spring Boot doc pages (AsciiDoc), splits them into chunks, and stores embeddings in Postgres (pgvector)
2. **Retrieve**: on each question, finds the 8 most similar chunks
3. **Answer**: sends those chunks to an LLM, which answers using only that context
4. **Cite**: returns the source file names with the answer

## Stack

Java 21, Spring Boot, Spring AI, PostgreSQL + pgvector (Docker), local embeddings (all-MiniLM-L6-v2), Groq (gpt-oss-120b)

## Run it

1. Start pgvector: `docker run -d --name pgvector -e POSTGRES_PASSWORD=postgres -p 5432:5432 pgvector/pgvector:pg16`
2. Set your Groq key: `setx GROQ_API_KEY "your-key"`
3. Run: `./mvnw spring-boot:run`
4. Load the docs once: open `http://localhost:8080/ingest-docs`
5. Ask: `http://localhost:8080/ask?q=how do I change the server port`

## Next

- Hybrid search (vector + keyword) to cut irrelevant sources
- Test set to measure retrieval accuracy
- Live deployment
