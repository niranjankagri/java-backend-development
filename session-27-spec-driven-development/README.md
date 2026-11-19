# Session 27: Spec-Driven Development with AI

Session 26 showed that the model fills every gap in a prompt with a guess. **Spec-driven development**
closes the gaps before any code is generated: first a short written specification (goal, interface,
numbered rules, examples, what is out of scope), then the code, then tests that check each rule.

One feature runs through the whole session: **the refund when a booking is cancelled**. The same
model (Claude Haiku 5.5, `claude -p --model haiku --tools ""`) got it three ways, 5 times each, and
every answer was compiled with `-Xlint:all -Werror` and run against 17 hidden test cases.

| # | Topic | README |
|---|---|---|
| 1 | From prompts to structured specifications | [01-from-prompts-to-specs](topics/01-from-prompts-to-specs/README.md) |
| 2 | Writing a clear specification before generating code | [02-writing-a-spec](topics/02-writing-a-spec/README.md) |
| 3 | Few-shot with real codebase patterns | [03-codebase-patterns](topics/03-codebase-patterns/README.md) |
| 4 | Reliable code aligned with the requirements | [04-reliable-code-from-specs](topics/04-reliable-code-from-specs/README.md) |
| 5 | Less ambiguity, more determinism | [05-less-ambiguity-more-determinism](topics/05-less-ambiguity-more-determinism/README.md) |

## The results

| Input | Hidden tests passed (5 runs) | Runs agree (34 inputs) | Code style checks |
|---|---|---|---|
| P: a chat-style request ([PROMPT.md](topics/01-from-prompts-to-specs/PROMPT.md)) | 10, 9, 11, 10, 14 of 17 | 32 of 34 | 5 of 6 (4 runs), 6 of 6 (1 run) |
| S: a specification ([SPEC.md](topics/02-writing-a-spec/SPEC.md)) | **17, 17, 17, 17, 17** | **34 of 34** | 4 of 6 (all runs) |
| C: the specification + codebase patterns ([PATTERNS.md](topics/03-codebase-patterns/PATTERNS.md)) | **17, 17, 17, 17, 17** | **34 of 34** | **6 of 6** (one regex false alarm, see topic 3) |
| F: P's worst answer + its failing tests as feedback (3 runs) | 15, 16, 16 of 17 | 29 of 34 | 4 or 5 of 6 |

18 runs, $0.09 in total at API prices.

## What works after this session

- [`SPEC.md`](topics/02-writing-a-spec/SPEC.md): a complete specification (7 rules, 6 examples).
- [`StandardRefundPolicy`](src/main/java/jbdl/session27/refund/StandardRefundPolicy.java): generated
  from SPEC.md + PATTERNS.md (run C1), copied unchanged.
- [`StandardRefundPolicyTest`](src/test/java/jbdl/session27/refund/StandardRefundPolicyTest.java):
  the acceptance tests, grouped by rule (R1 to R7) plus the spec's examples: 21 tests.
- The types the specification names: [`Booking`](src/main/java/jbdl/session27/refund/Booking.java),
  [`CancelledBy`](src/main/java/jbdl/session27/refund/CancelledBy.java),
  [`RefundPolicy`](src/main/java/jbdl/session27/refund/RefundPolicy.java).

## What you can do after this session

- Turn a vague request into a specification with numbered, checkable rules and examples.
- Find the ambiguous words in a request before the AI guesses them.
- Give the model your codebase's patterns, so generated code looks like the rest of the project.
- Derive acceptance tests from the specification and trace each one to a rule.
- Tell consistent from correct: runs that agree can all be wrong.

## Run it

```bash
./mvnw -pl session-27-spec-driven-development verify
```
