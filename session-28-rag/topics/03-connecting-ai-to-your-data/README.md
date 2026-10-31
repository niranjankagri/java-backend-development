# Topic 3: Connecting AI to codebases, documentation and internal data

RAG works on any text you have. The work is mostly in the first step: getting the right text in, in
pieces of the right size, with enough information to cite it.

## Our data: the README files of this course

[`CourseAssistant.loadChunks`](../../src/main/java/jbdl/session28/rag/CourseAssistant.java) walks the
repository, reads every `.md` file and skips what is not documentation:

```java
private static boolean isSkipped(String relative) {
    return relative.startsWith(".") || relative.contains("/.") || relative.contains("target/")
            || relative.startsWith("out/") || relative.startsWith("session-28-");
}
```

Build output (`target/`) would add copies; Session 28's own folder is left out because its READMEs
discuss the very questions of the experiments, with the answers. Deciding **what not to index** is
part of the design.

## Chunking

[`MarkdownChunker`](../../src/main/java/jbdl/session28/rag/MarkdownChunker.java) makes one chunk per
heading (`#`, `##`, `###`), and splits sections over 2,000 characters at blank lines. A `#` inside a
code block is a shell comment, not a heading. Every chunk keeps its file and heading, so the answer
can cite `session-15-redis-caching/topics/04-local-redis-server/README.md > What the real Redis
taught us`.

The course became **833 chunks**. Choosing the size is a trade-off:

| Chunks too small | Chunks too large |
|---|---|
| the answer is split across chunks; the context around a sentence is lost | one chunk covers many topics; its vector is a blur; fewer fit into the prompt |

Splitting at headings uses the structure the author already gave the text. For code, the same idea
splits at classes and methods.

## Other data you can connect

| Source | Chunk by | Watch out for |
|---|---|---|
| Code | class, method | generated code, vendored libraries; the code is the truth, the docs may be older |
| API specs (OpenAPI) | endpoint | versions |
| Tickets, wikis | page, section | outdated pages (topic 5), permissions |
| Chat, email | thread | personal data, secrets |
| Databases | row or record, as text | what each user is allowed to see |

Two rules for company data:

- **Permissions:** the assistant must not show a user a chunk the user could not open. Store who may
  read each chunk, and filter **before** the search results go into the prompt.
- **Secrets and personal data:** what is indexed can end up in an answer. Do not index `.env` files,
  credentials or customer data you would not paste into a chat.

## Keeping it fresh

The index is a copy. When a README changes, its chunks must be indexed again, or the assistant
answers from the old text. Our index is rebuilt on every run (reading, chunking and indexing the
833 chunks plus one search took 1.4 seconds); a real system re-indexes changed files when they
change, for example after every merge.
