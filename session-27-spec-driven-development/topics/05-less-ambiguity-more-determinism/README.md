# Topic 5: Less ambiguity, more determinism

A model gives a different answer each time you ask (Session 26). You cannot switch that off for
current models, and you do not need to: what must be the same is the **behaviour**, not the text of
the code.

## Measuring it

All generated classes were run on the same 34 inputs: one booking (1000.05 for the nights, 250.00
cleaning fee, 20 to 23 Dec 2026), cancelled on every day from 8 to 24 Dec, by the guest and by the
host. For each input: do **all** runs of a variant give the same result (amount or exception)?

| Variant | Runs | All runs agree on | Runs that match the spec on all 34 |
|---|---|---|---|
| P: chat-style request | 5 | 32 of 34 | 0 of 5 |
| S: specification | 5 | **34 of 34** | 5 of 5 |
| C: specification + patterns | 5 | **34 of 34** | 5 of 5 |
| F: P2 + failing tests as feedback | 3 | 29 of 34 | 0 of 3 |

The S runs differ in their code (62 to 83 lines, different names and structure) and still behave
identically on every input. That is the determinism that matters.

## Consistent is not correct

The P runs agreed on 32 of 34 inputs, almost as well as S. But on 7 inputs all 5 agreed on the
**same wrong answer**:

```
input            P1       P2       P3       P4       P5      the spec
12-20 HOST       1250.05  1250.05  1250.05  1250.05  1250.05 IllegalArgumentException
12-23 GUEST      0.00     0.00     0.00     0.00     0.00    IllegalArgumentException
...
```

The model's default reading of "if the host cancels, the guest gets everything back" is "at any
time". Ask five times and you get the same default five times. Agreement between runs only shows
that the model is sure, not that it is right. Only a check against the requirement shows that.

Where the P runs did **not** agree is exactly where the words were vague:

```
input            P1       P2       P3       P4       P5
12-18 GUEST      750.03   250.00   750.03   250.00   750.03    "a few days": from 2 or from 3?
12-20 GUEST      250.00   0.00     0.00     0.00     0.00      "never arrives": on the check-in day?
```

Disagreement between runs is a cheap **ambiguity detector**: ask for the same thing several times,
compare the behaviour, and every difference points at a sentence in your request that needs a rule.

## How to reduce ambiguity

| Instead of | Write |
|---|---|
| "a few days", "soon", "large" | numbers with both boundaries: "2 to 6 days" |
| "handle bad input sensibly" | each bad input and its result: "null -> `IllegalArgumentException`" |
| a word used in two meanings | one definition, then only that word ("days before check-in") |
| leaving out a case | saying what happens, even "not allowed" (R2, R4) |
| "like the other services" | the actual example file (topic 3) |
| "make it correct" | examples with the expected result, at the boundaries |

And the habits that make generated code dependable:

- Ask more than once for important code, and compare the behaviour (not the text).
- Keep the spec and the tests in the repository; regenerate or review against them.
- Let code compute what must be exact (money, dates), and let the AI write that code.
