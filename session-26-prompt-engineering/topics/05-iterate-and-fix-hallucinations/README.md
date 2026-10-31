# Topic 5: Iterating on prompts; finding and fixing hallucinations

A prompt is rarely right the first time. Treat it like code: run it, check the result against
something true, change one thing, run it again.

```
write prompt -> run (more than once) -> check against tests / docs / the real code -> find the cause -> change the prompt
```

## Iteration 1: write down what the examples only hinted at

Topic 3 ended with prompt C: 9, 12 and 12 of 12. The cause of the 9: run C1 read the 37-character
example as "at most 37 characters". The fix is not another example, but the rule itself. Prompt D
([`prompt-D.txt`](../03-zero-shot-to-few-shot/prompts/prompt-D.txt)) is C plus two
constraints:

```
- At most 40 characters. Never cut inside a word, unless the first word alone is longer than 40 characters: then cut it at 40
- "&" becomes the word "and"; if nothing is left, the slug is "listing"
```

| Prompt | Runs |
|---|---|
| C, examples only | 9, 12, 12 of 12 |
| D, examples + the rules | **12, 12, 12** of 12 |

One change, one cause, measured again. That is iterating.

## Hallucinations

A **hallucination** is a confident answer that is not true: an invented method, a property that
does not exist, a wrong version. The model is not lying; it writes what is likely, and something can
be likely without being true. Three experiments, all with Claude Haiku 5.5 and prompts from
[`HallucinationPrompts`](../../src/main/java/jbdl/session26/prompt/HallucinationPrompts.java).

### 1. A question it cannot know

"In niranjankagri/java-backend-development, on which port does the Session 20 order-service run, and
which Docker image does its Kafka end-to-end test start?" The truth: `8200` and
`apache/kafka-native:4.1.1`. Session 20 was not even on GitHub yet.

| Prompt | Result (6 runs each: 3 as the agent, 3 as the bare model) |
|---|---|
| [1](prompts/prompt-1.txt): the question | "I don't know", 6 of 6 (no guess) |
| [2](prompts/prompt-2.txt): + "if you cannot know something, answer I don't know" | "I don't know", 6 of 6 |
| [3](prompts/prompt-3.txt): + the two source lines as context | `8200` and `apache/kafka-native:4.1.1`, with file and line, 6 of 6 |

First as the Claude Code agent: it tried to look the answer up (`gh api`, WebFetch), was not allowed,
and said so. Then as the bare model (`--tools "" --system-prompt "You are a helpful assistant."`):

```
I can't answer this from here. I don't have a GitHub tool or any way to browse or clone
repositories in this session, so I can't read niranjankagri/java-backend-development.
```

No invented port in 18 runs. Current models have learned to say "I don't know" for things that are
obviously out of reach. And context (prompt 3) turned "I don't know" into the right answer.

### 2. A question with a false premise

Harder: ask about something that **sounds** real, as if it existed. Both were checked first against
the real jars of this course:

- [4a](prompts/prompt-4a.txt): "In Resilience4j 2.3, how do I use the `@CircuitBreakerFallback`
  annotation?" `resilience4j-annotations-2.3.0.jar` contains `Bulkhead`, `CircuitBreaker`,
  `RateLimiter`, `Retry`, `TimeLimiter` and `Timer`; there is no `CircuitBreakerFallback`.
- [4b](prompts/prompt-4b.txt): "What does `spring.kafka.consumer.idempotent-delivery=true` do in
  Spring Boot 4.1?" The property metadata of `spring-boot-kafka-4.1.2.jar` has no such property.

All 6 answers rejected the premise, e.g.:

```
There's no @CircuitBreakerFallback annotation in Resilience4j. The fallback is set with the
fallbackMethod attribute on @CircuitBreaker, which is the same approach in 2.x and 2.3.
```

Adding "first check that what I ask about really exists" ([5a](prompts/prompt-5a.txt),
[5b](prompts/prompt-5b.txt)) made the answers more careful ("as far as I know", "I can't check the
2.3 source from here"), and also correct, 6 of 6.

### 3. The hallucination that got through

Each annotation answer also listed the dependencies, and that is where it went wrong:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

The model assumed Spring Boot 3. In Spring Boot 4 this starter is gone: on Maven Central
`spring-boot-starter-aop` stops at `4.0.0-M2`, and Session 9 of this course uses its successor,
`spring-boot-starter-aspectj`. All 6 answers (4a and 5a) named the old starter.

Iteration: give the versions as context ([6a](prompts/prompt-6a.txt)):

```
Context:
The project uses Spring Boot 4.1.2, Resilience4j 2.3.0 and Java 17.
...
- Name dependencies exactly as they are called in these versions
```

Result, 3 runs: still `spring-boot-starter-aop` in all 3, now with a warning:

```
I'm not sure resilience4j-spring-boot3 is validated against Spring Boot 4.1.2, so check that combination.
```

**A prompt cannot add knowledge the model does not have.** Telling it the version made it careful,
not correct. What would fix it: giving it the fact (the Spring Boot 4 starter list as context, or a
tool that reads Maven Central). That is the idea of Session 28 (RAG).

## How to catch hallucinations

| Check | Catches |
|---|---|
| Compile it | invented classes, methods, wrong signatures |
| Run the tests | wrong behaviour |
| Look it up in the jar, the docs, Maven Central | invented annotations, properties, versions, artifacts |
| Ask "does this exist in version X?" | false premises (it helped here) |
| Give the real source as context | everything the model cannot know (it fixed experiment 1) |

The dangerous answers are not the obviously wrong ones, but the ones that are **mostly** right. The
Resilience4j code above was correct; only one dependency in it was out of date.
