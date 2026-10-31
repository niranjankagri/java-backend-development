# Session 25: The AI Toolkit & Developer Setup

The first of four AI sessions. Before writing prompts (Session 26) or specifications (Session 27),
you need to know which kinds of AI tools exist, how to judge them with numbers instead of
impressions, where they fail, and how to set up a project so they work well in it.

| # | Topic | README |
|---|---|---|
| 1 | A map of AI tools: LLMs, code assistants, AI search, agents | [01-map-of-ai-tools](topics/01-map-of-ai-tools/README.md) |
| 2 | Chat models, IDE copilots and CLI agents compared | [02-chat-ide-and-cli](topics/02-chat-ide-and-cli/README.md) |
| 3 | Accuracy, context window, cost and latency | [03-accuracy-context-cost-latency](topics/03-accuracy-context-cost-latency/README.md) |
| 4 | When AI helps and where it fails | [04-when-ai-helps](topics/04-when-ai-helps/README.md) |
| 5 | An AI-ready development environment | [05-ai-dev-environment](topics/05-ai-dev-environment/README.md) |
| 6 | The same problem, two tools, one set of tests | [06-same-problem-many-tools](topics/06-same-problem-many-tools/README.md) |

## What works after this session

- [`CostCalculator`](src/main/java/jbdl/session25/cost/CostCalculator.java) turns token counts
  into dollars. Its test reproduces, to the last digit, the cost Claude Code reported for a real
  run ($0.2286246).
- [`CodebaseSize`](src/main/java/jbdl/session25/cost/CodebaseSize.java) answers "can I give the AI
  my whole project, and what would that cost?" (the Airbnb backend: about 38,000 tokens).
- [`AGENTS.md`](../AGENTS.md) and [`CLAUDE.md`](../CLAUDE.md) at the root of the repository tell
  AI tools the rules of this course. Both tools tried here repeated the rules back without being
  shown the file.
- [`PROBLEM.md`](topics/06-same-problem-many-tools/PROBLEM.md) was solved by Claude Code and by
  Codex, without help. Both solutions are in [`compare/`](src/main/java/jbdl/session25/compare) and
  pass the same 12 tests, which the tools never saw.
- 18 tests in the module (12 of them run twice, once per tool: 30 test runs).

## What you can do after this session

- Name the kinds of AI tools and pick the right one for a task.
- Compare models with numbers: tests passed, tokens, dollars, seconds.
- Estimate whether a codebase fits into a context window, and what reading it costs.
- Say where AI output must not be trusted without a check, and how to check it.
- Set up a repository with an instructions file, safe permissions and a fast test loop.
- Run a fair comparison of two tools: same input, fresh folder, hidden tests.

## Run it

```bash
./mvnw -pl session-25-ai-toolkit verify
java -cp session-25-ai-toolkit/target/classes jbdl.session25.cost.CodebaseSize airbnb-backend
```
