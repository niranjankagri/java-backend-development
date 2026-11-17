# Topic 4: When AI helps and where it fails

An LLM writes the most **plausible** continuation. Most of the time plausible is also correct. When it
is not, the answer looks just as confident. Everything in this topic follows from that.

## Where AI helps most

| Task | Why it works |
|---|---|
| Boilerplate: DTOs, mappers, configuration, test setup | seen millions of times, easy to check |
| Explaining code, an error message, a stack trace | reading is easier than writing; you can verify against the code |
| A first draft of tests | it lists cases you forgot; you check each one |
| Translating: SQL to JPQL, a loop to a stream, Java to Kotlin | the meaning is given, only the form changes |
| Searching a big codebase ("where is the booking price computed?") | agents grep faster than you |
| Small, clearly specified functions (topic 6) | the spec leaves little room for guessing |

## Where it fails

| Failure | Example | What protects you |
|---|---|---|
| **Hallucination**: invented facts that look real | a method that does not exist in your library version, a config property with a plausible name | the compiler, the docs, running it |
| **Out-of-date knowledge** | code for Spring Boot 2 when you use 4 (`javax.*` instead of `jakarta.*`) | tell it the versions (topic 5: `AGENTS.md`) |
| **Missing context** | it does not know your conventions, your other services, last week's decision | give the context; instructions file |
| **Wrong guess about a cause** | a fix for the symptom you described, not the bug | reproduce first, then fix, then test |
| **Concurrency, security, money** | code that works in a test with one thread and fails under load | tests that prove it under the real condition |
| **Looks done, is not** | "all tests pass" when the tests were not run | run them yourself, or read the output |

## Real examples from this course

This course was prepared with an AI agent (Claude Code), which merged its work only after the build
and tests passed. Its mistakes were caught by checks, not by reading:

- **A wrong first guess (Session 15).** A Redis cache test failed now and then. The first guess
  (that clearing the cache was deferred) was tested and turned out wrong; the real cause was that
  Spring Data Redis 4 writes cache entries **asynchronously**. The README there tells both parts.
- **A flaky test (Session 19).** A load-balancing test expected exactly 2 and 2 of 4 orders on two
  instances. It failed about one run in three: health checks can list the instances in a different
  order, and round robin then picks the same one twice. Only running it many times showed it.
- **An invented reference (a Session 22 draft).** A draft README said the transactional outbox
  pattern was explained in Session 20. It was not. It was found in review and the reference removed.
- **Proof instead of trust (Session 22).** "Is the pessimistic lock really needed?" A test answered
  it: without the lock, 10 parallel bookings of the same nights all succeeded.
- **An honest "not verified" (topic 6).** Codex could not compile its class in its sandbox, and said
  so instead of claiming it worked. That is the behaviour you want; the course build checked it.

## The rule

Use AI where **you can check the result quickly**: a compiler, tests, a run, the official docs. The
less you can check, the less you should rely on it. Never accept code you could not explain to a
colleague, and never paste secrets (passwords, API keys, customer data) into an AI tool.

```
             easy to check                      hard to check
           +-------------------------------+-------------------------------+
low risk   | use AI freely                 | use AI, review carefully       |
           | (boilerplate, tests, docs)    | (a refactoring without tests)  |
           +-------------------------------+-------------------------------+
high risk  | use AI, verify every result   | write it yourself, or add       |
           | (SQL migrations, security)    | the checks first (locks, money) |
           +-------------------------------+-------------------------------+
```
