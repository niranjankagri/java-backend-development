# Topic 3: Accuracy, context window, cost and latency

"This model feels smarter" is not a measurement. Four numbers describe how well a model or tool
fits a task.

| Measure | Question | How to measure it |
|---|---|---|
| Accuracy | is the answer right? | tests the AI did not see (topic 6) |
| Context window | does everything it needs fit into one request? | count tokens |
| Cost | what does one task cost? | tokens x price |
| Latency | how long do I wait? | a stopwatch; time to first token and total time |

They pull against each other: a bigger model is usually more accurate, slower and more expensive.
The right choice is the cheapest, fastest one that is still accurate enough **for this task**.

## Tokens

Models read and write **tokens**, pieces of words. A rough rule for English text and code: about
4 characters per token. [`TokenEstimator`](../../src/main/java/jbdl/session25/cost/TokenEstimator.java)
uses it:

```java
public static long estimate(long characters) {
    // adding (4 - 1) makes the division round up: 1 to 4 characters are 1 token, 5 are 2
    return (characters + CHARACTERS_PER_TOKEN - 1) / CHARACTERS_PER_TOKEN;
}
```

It is an estimate. Every model family has its own tokenizer, and the same text gives different
counts on different models. That is also why token counts from two vendors (topic 6) cannot be
compared directly. For an exact number, ask the provider's API (Claude: `count_tokens`).

## The context window

The most tokens one request can hold: your prompt, the files, the conversation so far, **and** the
answer. [`CostCalculator.fits`](../../src/main/java/jbdl/session25/cost/CostCalculator.java) checks
it and keeps room for the answer:

```java
public static boolean fits(ModelPrice model, long inputTokens, long answerTokens) {
    return inputTokens + answerTokens <= model.contextWindow();
}
```

Does our Airbnb project fit? [`CodebaseSize`](../../src/main/java/jbdl/session25/cost/CodebaseSize.java)
counts all source files (code, SQL, configuration, scripts, docs; not `target/`):

```
$ ./mvnw -q -pl session-25-ai-toolkit package
$ java -cp session-25-ai-toolkit/target/classes jbdl.session25.cost.CodebaseSize airbnb-backend
airbnb-backend: 55 source files, 152,781 characters, about 38,196 tokens
claude-opus-5-5    fits: yes  read once: $0.1528  read again from the cache: $0.0076
claude-sonnet-5-5  fits: yes  read once: $0.0764  read again from the cache: $0.0076
claude-haiku-5-5   fits: yes  read once: $0.0038  read again from the cache: $0.0004

$ java -cp session-25-ai-toolkit/target/classes jbdl.session25.cost.CodebaseSize .
.: 554 source files, 1,358,460 characters, about 339,615 tokens
claude-opus-5-5    fits: yes  read once: $1.3585  read again from the cache: $0.0679
claude-sonnet-5-5  fits: yes  read once: $0.6792  read again from the cache: $0.0679
claude-haiku-5-5   fits: yes  read once: $0.0340  read again from the cache: $0.0034
```

Three lessons from these numbers:

- **The whole course fits** into a 1 million token window. Fitting is not the same as being used
  well: models find details less reliably in a very long prompt, and every request pays for all of
  it again. Agents therefore **search** (list files, grep, open a few) instead of reading everything.
- **The tool is simplified on purpose.** Haiku 5.5's price is for prompts up to 100K tokens; above
  that it costs $0.50 per million, so the whole course would cost about $0.17 to read, not $0.034.
  A calculator is only as good as the price rules you put into it.
- **The prompt cache** changes everything: reading the same 38,000 tokens again costs 5 % (Opus) of
  the first time. Tools keep the start of the conversation identical so that it can be cached.

## Cost

Providers charge per million tokens, separately for each kind
([`ModelPrice`](../../src/main/java/jbdl/session25/cost/ModelPrice.java), prices on 31 Oct 2026):

| Model | Input | Output | Cache write (1 h) | Cache read | Context window |
|---|---|---|---|---|---|
| claude-opus-5-5 | $4.00 | $20.00 | $8.00 | $0.20 | 1,000,000 |
| claude-sonnet-5-5 | $2.00 | $10.00 | $4.00 | $0.20 | 1,000,000 |
| claude-haiku-5-5 | $0.10 | $0.50 | $0.20 (assumed) | $0.01 (assumed) | 1,000,000 |

The calculation is the same for every kind of token:

```java
private static BigDecimal part(long tokens, BigDecimal pricePerMillion) {
    // tokens x price / 1,000,000; dividing by a million is always exact, so no rounding here
    return BigDecimal.valueOf(tokens).multiply(pricePerMillion).divide(MILLION);
}
```

[`CostCalculatorTest`](../../src/test/java/jbdl/session25/cost/CostCalculatorTest.java) checks it
against reality. Claude Code reported this usage and cost for its run in topic 6:

```
"usage": {"input_tokens": 12, "cache_creation_input_tokens": 16488,
          "cache_read_input_tokens": 159763, "output_tokens": 3236, ...}
"total_cost_usd": 0.2286246
```

```
input        12 x $4  / million = $0.000048
cache write  16,488 x $8  / million = $0.131904
cache read   159,763 x $0.20 / million = $0.0319526
output       3,236 x $20 / million = $0.06472
total                                $0.2286246   (exactly what Claude Code reported)
```

Two things are worth seeing here:

- The agent read **176,263 input tokens** for an 81-line class. An agent sends the whole
  conversation again on every step (8 steps here), so input grows with every tool call.
- Without the cache the same run would cost **$0.7698** (second test): caching saved 70 %.

Output tokens cost 5 times as much as input. Long answers, long explanations and "thinking" are
the expensive part; a short, precise task is cheaper.

With a subscription (Claude Pro/Max, ChatGPT Plus/Pro) you do not pay per token, but the same
tokens count against your usage limits.

## Latency

Measured in topic 6 for the same task:

| | Time to first token | Total |
|---|---|---|
| Claude Code (claude-opus-5-5) | 2.6 s | 36 s (44 s including start-up) |
| Codex (gpt-6-luna, reasoning effort xhigh) | not reported | 111 s |

An agent's total time is mostly its steps (each one a model call plus a tool run), not the speed of
writing. For typing help in the IDE, the time to the first token matters most; for an agent task,
the total time and whether you must wait at all (run it in the background).

## Accuracy

The only honest measure is a check the AI did not write and did not see: tests, a compiler, a real
run. Topic 6 does exactly that. Benchmarks published by vendors tell you how a model does on
**their** tasks, not on yours.
