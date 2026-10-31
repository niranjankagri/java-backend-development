# Topic 4: Chain-of-thought prompting

**Chain of thought** means asking the model to write down its reasoning step by step before the
answer. For older models this was the classic trick for arithmetic and logic: a model that "thinks
out loud" makes fewer mistakes than one that must answer at once.

## The experiment

The price of a stay with the rules of Session 25 (weekend nights +20 %, 10 % off for 7+ nights, a
cleaning fee after the discount): 1234.50 per night, Thursday 24 Dec 2026 to Friday 1 Jan 2027.
Right answer: (6 x 1234.50 + 2 x 1481.40) x 0.9 + 500 = **9832.82**.

Two prompts from [`PricePrompts`](../../src/main/java/jbdl/session26/prompt/PricePrompts.java):

- [`direct.txt`](prompts/direct.txt): "Only the total as a number with 2 decimals, nothing else."
- [`steps.txt`](prompts/steps.txt): "Work it out step by step: list every night with its weekday
  and price, then the sum, the discount and the fee. End with one line: TOTAL: <number>"

Each was sent 5 times to Claude Haiku 5.5, at two effort levels (`--effort low` and `medium`, the
default for this model): 20 runs.

| Effort | Prompt | Right | Output tokens (average) | Time (average) |
|---|---|---|---|---|
| low | direct | 5 / 5 | 412 | 3.7 s |
| low | step by step | 5 / 5 | 985 | 6.1 s |
| medium | direct | 5 / 5 | 530 | 4.3 s |
| medium | step by step | 5 / 5 | 1,120 | 7.2 s |

All 20 right. Step by step cost about twice the tokens and time and gained nothing in accuracy.

## Why: the model already thinks

The "direct" answer was 7 characters (`9832.82`), but it used 365 output tokens. The usage the API
reported:

```
"output_tokens": 365, "output_tokens_details": {"thinking_tokens": 359}
```

359 of them were **thinking**: current models from all large providers can reason internally before
they answer, and many do it by default. The effort setting controls
how much. That is chain of thought built into the model, so the prompt no longer has to ask for it.

## So why still ask for steps?

Because of **you**, not the model. A step-by-step answer can be checked line by line:

```
| Night       | Weekday         | Price    |
|-------------|-----------------|----------|
| 24 Dec 2026 | Thursday        | 1,234.50 |
| 25 Dec 2026 | Friday (+20%)   | 1,481.40 |
| 26 Dec 2026 | Saturday (+20%) | 1,481.40 |
| 27 Dec 2026 | Sunday          | 1,234.50 |
...
Sum of nights: 6 x 1,234.50 + 2 x 1,481.40 = 7,407.00 + 2,962.80 = 10,369.80
Discount: ... 10% off 10,369.80 = -1,036.98, leaving 9,332.82
Cleaning fee: +500.00, added after the discount, gives 9,832.82
TOTAL: 9832.82
```

(run `low-steps-1`, shortened). If a weekday or a rule were wrong, you would see where.

## When to use it

| Situation | Ask for steps? |
|---|---|
| A model with built-in thinking, answer checked by a test or by code | no: pay for the answer only |
| You must review the reasoning (a calculation, a design decision, a bug analysis) | yes: steps make it checkable |
| A model without thinking (small, fast or older models) | yes: this is where the classic trick helps |
| You need a machine-readable result | yes, but fix the format of the last line (`TOTAL: <number>`) so code can read it |

For real calculations in an application, the answer is still: **let code calculate**
(`CodexPriceCalculator` in Session 25), and use the model to write and explain that code.
