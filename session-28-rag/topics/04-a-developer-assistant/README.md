# Topic 4: Building a developer assistant with RAG

[`CourseAssistant`](../../src/main/java/jbdl/session28/rag/CourseAssistant.java) answers questions about
this course. It indexes the repository's Markdown files (topic 3), finds the 4 best chunks (topic 2)
and builds the prompt ([`RagPrompt`](../../src/main/java/jbdl/session28/rag/RagPrompt.java)). The
model is then called with `claude -p`; the Java code needs no API key.

```bash
./mvnw -q -pl session-28-rag package
java -cp session-28-rag/target/classes jbdl.session28.rag.CourseAssistant . \
    "Why did the Redis test in Session 15 fail sometimes?" \
  | claude -p --model haiku --tools "" --strict-mcp-config --system-prompt "You are a helpful assistant."
```

The assistant prints what it found on stderr:

```
833 chunks indexed. Best matches:
  0.396  session-15-redis-caching/README.md > Run it
  0.380  session-15-redis-caching/topics/04-local-redis-server/README.md > Run the application against it
  0.309  session-15-redis-caching/topics/04-local-redis-server/README.md > Interview questions
  0.286  session-15-redis-caching/topics/04-local-redis-server/README.md > What the real Redis taught us
```

The prompt it built is in [`example-prompt.txt`](example-prompt.txt). The answer:

```
The RedisTest failed sometimes because Spring Data Redis 4 can write cache entries asynchronously [4].
A second rateToInr("USD") call made right after the first could miss the cached value, and an eviction
could run before the write finished, after which the write re-created the key [4]. The fix was switching
to immediateWrites() in RedisCacheConfig, after which the module passed 5 runs out of 5 [4].
```

Every fact is from source 4, and it is right (compare with
[Session 15, topic 4](../../../session-15-redis-caching/topics/04-local-redis-server/README.md)).

## The experiment: 9 questions, with and without RAG

Claude Haiku 5.5, no tools, no MCP servers, system prompt "You are a helpful assistant.", one run per
question and mode (18 runs, $0.01).

| Question | Without RAG | With RAG |
|---|---|---|
| Which port does the Session 20 order-service run on? | "I don't know" | **8200** [2]; and: "Source [3] lists port 8190 for order-service, but that is the Session 19 example, so it does not apply here." |
| Which Docker image does the Kafka end-to-end test start? | "I can't tell from here" | "The sources do not say." (retrieval missed, topic 5) |
| Why did the Redis test in Session 15 fail sometimes? | general causes of flaky Redis tests | **asynchronous cache writes**, the fix (above) |
| Which starter replaced `spring-boot-starter-aop` in Spring Boot 4? | **`spring-boot-starter-aspectj`** (general knowledge) | **`spring-boot-starter-aspectj`** [1] [3] |
| What refund does a guest get when cancelling 1 day before check-in? | "I don't have a specific cancellation policy" | **only the cleaning fee** [2] |
| How much money does a guest get back if they cancel the day before arrival? | the policies of real platforms | **only the cleaning fee** [3] |
| How does the Airbnb backend stop two guests from booking the same nights? | how the **real** Airbnb might do it | the overlap check and 409 [4], and that Session 22 closes the gap for the same moment [2]; the lock itself was not in the chunks found |
| What does the smoke test check after a deployment? | "I can't see a smoke test" | **ready, register, list, search, book, the same nights again must be 409** [2] [3] |
| On which Java versions does CI run the tests? | "I can't tell from here" | **17 and 25** [1] |

Without RAG: 1 of 9. With RAG: 7 of 9 right, and the 2 others honest about what was missing. Note
the port answer: two chunks named an order-service port, and the model kept them apart because each
chunk carried its source.

## Making retrieval better

Retrieval decides the result: the model cannot use what was not found.
[`RetrievalEval`](../../src/main/java/jbdl/session28/rag/RetrievalEval.java) measures it without a
model: a question is a hit when one of the top 4 chunks contains a text only the answer has.

```
$ java -cp session-28-rag/target/classes jbdl.session28.rag.RetrievalEval .
Plain words:
  hit   Which port does the Session 20 order-service run on?
  MISS  Which Docker image does the Kafka end-to-end test start?
  MISS  Why did the Redis test in Session 15 fail sometimes?
  hit   Which Spring Boot starter replaced spring-boot-starter-aop in Spring Boot 4?
  hit   What refund does a guest get when cancelling 1 day before check-in?
  MISS  How much money does a guest get back if they cancel the day before arrival?
  MISS  How does the Airbnb backend stop two guests from booking the same nights?
  hit   What does the smoke test check after a deployment?
  hit   On which Java versions does CI run the tests?
  5 of 9 questions found the chunk with the answer
With stemming:
  ...
  hit   Why did the Redis test in Session 15 fail sometimes?
  ...
  hit   How much money does a guest get back if they cancel the day before arrival?
  ...
  7 of 9 questions found the chunk with the answer
```

One small change, a **stemmer** that cuts "-ed", "-ing", "-s" (`fail`/`failed`, `cancel`/`cancelling`),
took retrieval from 5 to 7 of 9. It is now the default. Measure retrieval on its own like this
before blaming the model, and before and after every change.
