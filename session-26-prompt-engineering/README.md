# Session 26: Prompt Engineering & Vibe Coding

How you ask decides what you get. This session measures it: the same model, the same task, different
prompts, scored by tests the model never saw. Every prompt in this session was built by code
([`DeveloperPrompt`](src/main/java/jbdl/session26/prompt/DeveloperPrompt.java)), sent for real, and
is stored next to its results.

| # | Topic | README |
|---|---|---|
| 1 | Vibe coding: a prototype from one sentence | [01-vibe-coding](topics/01-vibe-coding/README.md) |
| 2 | Anatomy of a developer prompt: role, context, task, constraints | [02-anatomy-of-a-prompt](topics/02-anatomy-of-a-prompt/README.md) |
| 3 | From zero-shot to few-shot | [03-zero-shot-to-few-shot](topics/03-zero-shot-to-few-shot/README.md) |
| 4 | Chain-of-thought prompting | [04-chain-of-thought](topics/04-chain-of-thought/README.md) |
| 5 | Iterating on prompts; finding and fixing hallucinations | [05-iterate-and-fix-hallucinations](topics/05-iterate-and-fix-hallucinations/README.md) |

## The results in one table

All runs used Claude Haiku 5.5 through Claude Code 2.1.316 (`claude -p --model haiku`): 66 runs,
$0.21 in total at API prices.

| Experiment | Result |
|---|---|
| Vibe coding (topic 1) | a working tool in 28 s; but "weekend" meant Saturday and Sunday, a wrong date format crashed it although its summary said it would not, and a price of 0 and a 10-year stay were accepted |
| Zero-shot vague, A (topic 3) | 6, 6, 6 of 12 hidden tests |
| Zero-shot with role, context, constraints, B | 6, 6, 7 of 12 |
| Few-shot, C: B + 5 examples | 9, 12, 12 of 12 (run 1 read "at most 37 characters" from an example) |
| Few-shot + the rules written down, D (topic 5) | 12, 12, 12 of 12 |
| Chain of thought (topic 4) | right 20 of 20, with or without "step by step"; the model already thinks before a "number only" answer (359 of 365 output tokens were thinking) |
| Question it cannot know (topic 5) | "I don't know" 12 of 12; right 6 of 6 with the two lines given as context |
| False premises (topic 5) | spotted 15 of 15; but all 9 answers about the annotation named `spring-boot-starter-aop`, which Spring Boot 4 does not have, even when told the version |

## What works after this session

- [`DeveloperPrompt`](src/main/java/jbdl/session26/prompt/DeveloperPrompt.java) builds a prompt from
  role, context, task, constraints, examples and answer format.
  [`SlugPrompts`](src/main/java/jbdl/session26/prompt/SlugPrompts.java),
  [`PricePrompts`](src/main/java/jbdl/session26/prompt/PricePrompts.java) and
  [`HallucinationPrompts`](src/main/java/jbdl/session26/prompt/HallucinationPrompts.java) are the
  prompts of the experiments; a test checks they still render the stored prompt files exactly.
- [`ListingSlug`](src/main/java/jbdl/session26/slug/ListingSlug.java), written by the model for
  prompt C (run 2) and copied unchanged, passes all 12 hidden cases.
- [`prototype/PriceQuote.java`](topics/01-vibe-coding/prototype/PriceQuote.java): the vibe-coded
  prototype, unchanged (not part of the build, on purpose).
- 22 tests in the module.

## What you can do after this session

- Prototype quickly with natural language, and know what a prototype leaves out.
- Write a prompt with role, context, task, constraints and answer format.
- Escalate from zero-shot to few-shot, and turn what the examples imply into written rules.
- Decide when to ask for step-by-step reasoning.
- Spot a hallucination by checking against the real source, and fix it with context, not wording.

## Run it

```bash
./mvnw -pl session-26-prompt-engineering verify
java -cp session-26-prompt-engineering/target/classes jbdl.session26.prompt.SlugPrompts C
java session-26-prompt-engineering/topics/01-vibe-coding/prototype/PriceQuote.java 100 2026-11-05 2026-11-10
```
