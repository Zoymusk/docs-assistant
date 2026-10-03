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

## Evaluation

26 questions written from the Spring Boot "how-to" docs, each with the expected source page (`src/main/resources/eval.txt`). Run it at `/eval`.

| Retrieval | Hit@1 | Hit@3 | Hit@8 |
|---|---|---|---|
| Vector only (used by `/ask`) | 62% | 85% | 96% |
| Hybrid (vector + keyword, RRF) | 58% | 81% | 92% |

Hybrid search did not beat plain vector search on this set, including after down-weighting the keyword side, so `/ask` uses vector search with the top 8 chunks. Raising the chunk count from 4 to 8 fixed a wrong answer I saw by hand. The remaining top-1 misses are mostly cases where a `reference/` page ranks above the `how-to/` page, which is a reasonable answer that the test marks wrong.
