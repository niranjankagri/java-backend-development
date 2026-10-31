# Topic 3: Few-shot with real codebase patterns

The specification decides **what** the code does. It does not decide **how it looks**: names,
comments, structure, error messages. In a real project that matters: generated code that looks
different from the rest is harder to review and to maintain. The fix is few-shot again (Session 26),
but with your own code as the example.

## What the model got

[`PATTERNS.md`](PATTERNS.md), added after SPEC.md, contains:

1. the "Rules for code" section of this repository's [`AGENTS.md`](../../../AGENTS.md) (Session 25);
2. one existing class of the codebase, Session 25's
   [`ClaudePriceCalculator`](../../../session-25-ai-toolkit/src/main/java/jbdl/session25/compare/claude/ClaudePriceCalculator.java),
   with one line saying what to copy from it: "the class Javadoc that lists the rules, named
   constants for every number, the input checks first, a small private method for each step,
   comments for beginners, BigDecimal for money".

Pick an example that is **typical and good**: the model copies everything, also the bad habits.

## The style checks

Six simple checks on each generated class (a script with regular expressions, not a real linter):

| Check | Spec only (S, 5 runs) | Spec + patterns (C, 5 runs) |
|---|---|---|
| package `jbdl.session27.refund` | 5 / 5 | 5 / 5 |
| a class Javadoc | **0 / 5** | 5 / 5 |
| no `double` / `float` for money | 5 / 5 | 5 / 5 (C3 was flagged, but the only match was its comment "never with `double`") |
| at least 3 named constants | **0 / 5** | 5 / 5 |
| no streams | 5 / 5 | 5 / 5 |
| exceptions with a message | 5 / 5 | 5 / 5 |

Both were correct (17 of 17 hidden tests, every run). Only C looks like the rest of the course.

## What it looks like

The spec-only answers put the numbers inline, without a Javadoc on the class. The pattern answers
copied the shape of `ClaudePriceCalculator` (run C1, now
[`StandardRefundPolicy`](../../src/main/java/jbdl/session27/refund/StandardRefundPolicy.java)):

```java
/**
 * Calculates how much money a guest gets back when a booking is cancelled.
 *
 * <p>The rules:
 * <ol>
 *   <li>Input check: no argument and no field of the booking may be null. ...
 *   ...
 */
public class StandardRefundPolicy implements RefundPolicy {

    private static final int MIN_DAYS_FULL_NIGHTS_REFUND = 7;
    private static final int MIN_DAYS_HALF_NIGHTS_REFUND = 2;
    ...

    @Override
    public BigDecimal refund(Booking booking, LocalDate cancelledOn, CancelledBy cancelledBy) {
        // Step 1: check the input first. Stop early if something is wrong.
        checkInput(booking, cancelledOn, cancelledBy);
        ...
    }
```

Even the empty constructor came from the example, with almost the same Javadoc: "Creates a price
calculator. It has no state, so one instance can be reused." became "Creates a refund policy. It has
no state, so one instance can be reused."

## The price

| | Spec only | Spec + patterns |
|---|---|---|
| Lines | 62 to 83 | 132 to 141 |
| Output tokens | 1,822 to 3,279 | 4,937 to 6,593 |
| Time | 9 to 13 s | 19 to 24 s |

About twice the output, because the code explains itself. For a learning project that is the point;
for a team, decide which patterns are worth it and put **those** into the example.

## Tips

- Give the model one or two real files that show your conventions, not a description of them.
- Put the stable rules into the instruction file (`AGENTS.md`), so agents get them every time.
- Say in one line what to copy from the example, or it may copy the wrong things (its domain logic).
- Check the result with your real tools: the compiler with `-Werror`, a linter, the review.
