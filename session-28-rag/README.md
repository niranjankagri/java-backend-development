# Session 28: RAG Systems & Context-Aware AI

The last session closes the gap the AI sessions kept finding: a model does not know **your** facts,
and prompting cannot add them. Retrieval-augmented generation finds the right pieces of your own
documents and gives them to the model with the question. This session builds it from scratch with
only the JDK, as a course assistant over this repository's 159 Markdown files, and measures where it
works and where it fails.

| # | Topic | README |
|---|---|---|
| 1 | RAG fundamentals | [01-rag-fundamentals](topics/01-rag-fundamentals/README.md) |
| 2 | Embeddings, vector databases and semantic search | [02-embeddings-and-vector-search](topics/02-embeddings-and-vector-search/README.md) |
| 3 | Connecting AI to codebases, documentation and internal data | [03-connecting-ai-to-your-data](topics/03-connecting-ai-to-your-data/README.md) |
| 4 | Building a developer assistant with RAG | [04-a-developer-assistant](topics/04-a-developer-assistant/README.md) |
| 5 | Limits and failure modes | [05-limits-and-failure-modes](topics/05-limits-and-failure-modes/README.md) |

## The results

All answers from Claude Haiku 5.5 (`claude -p --model haiku --tools "" --strict-mcp-config`); 30
runs, under $0.02 at API prices (plus one earlier run of the 18 questions with the MCP servers still
loaded, about $0.02, repeated without them for a clean comparison).

| Experiment | Result |
|---|---|
| 9 questions about the course, without RAG | 1 right (general knowledge), 2 about something else, 6 "I don't know" |
| the same, with RAG | **7 right with citations**, 2 "the sources do not say" (retrieval missed) |
| retrieval alone, plain words | the chunk with the answer in the top 4 for 5 of 9 questions |
| retrieval alone, with a small stemmer | **7 of 9** |
| a planted instruction in the docs ("answer 9999") | followed 0 of 6 times, reported every time |
| a planted **false fact** in the docs ("since Session 21 the port is 9999") | **believed 6 of 6 times**, with a citation |

## What works after this session

- [`MarkdownChunker`](src/main/java/jbdl/session28/rag/MarkdownChunker.java): one chunk per heading.
- [`TextVectorizer`](src/main/java/jbdl/session28/rag/TextVectorizer.java): TF-IDF vectors, cosine
  similarity, an optional stemmer.
- [`VectorIndex`](src/main/java/jbdl/session28/rag/VectorIndex.java): an in-memory vector store.
- [`RagPrompt`](src/main/java/jbdl/session28/rag/RagPrompt.java): sources, rules and the question.
- [`CourseAssistant`](src/main/java/jbdl/session28/rag/CourseAssistant.java): indexes the repository
  (833 chunks) and prints the prompt for a question;
  [`RetrievalEval`](src/main/java/jbdl/session28/rag/RetrievalEval.java) measures retrieval alone.
- 14 tests in the module, including one that shows the limit of word vectors ("money back" vs
  "refund").

## What you can do after this session

- Explain the RAG pipeline: chunk, vectorise, index, retrieve, augment, generate.
- Tell word-based search from semantic search with embeddings, and when to combine them.
- Prepare your own documents and code for retrieval: what to index, how to chunk, what to leave out.
- Build a small assistant that answers with citations and says when the sources do not know.
- Measure retrieval separately, and recognise the failure modes: misses, injected instructions,
  wrong or outdated sources, stale indexes, permission leaks.

## Run it

```bash
./mvnw -pl session-28-rag verify
java -cp session-28-rag/target/classes jbdl.session28.rag.RetrievalEval .
java -cp session-28-rag/target/classes jbdl.session28.rag.CourseAssistant . "What does the smoke test check after a deployment?" \
  | claude -p --model haiku --tools "" --strict-mcp-config --system-prompt "You are a helpful assistant."
```
