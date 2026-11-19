# Topic 4: Reliable code aligned with the requirements

"Aligned with the requirements" has to be shown, not claimed. In spec-driven development the
specification produces three things, and they check each other:

```
                 SPEC.md (rules R1-R7, examples)
                 /                          \
     generate the code                 derive the tests
               |                              |
  StandardRefundPolicy  <--- run --->  StandardRefundPolicyTest
```

## Tests derived from the specification

[`StandardRefundPolicyTest`](../../src/test/java/jbdl/session27/refund/StandardRefundPolicyTest.java)
has one group per rule, named after it, plus the spec's own examples:

| Group | Checks | Tests |
|---|---|---|
| SPEC.md examples | the 5 examples with an amount, word for word | 5 |
| R1 | null arguments, a null field, a negative amount, check-out not after check-in | 4 |
| R2 | no cancellation on or after check-out | 1 |
| R3 and R4 | the host: full refund before the check-in day, an exception from then on | 2 |
| R5 and R6 | the guest at every boundary: 7, 6, 2, 1, 0 and -2 days; no cleaning fee | 7 |
| R7 | half of 1000.05 rounds to 500.03; the result has 2 decimals | 2 |

```java
@Nested
@DisplayName("R5 and R6: the guest cancels, the boundaries")
class GuestCancels {

    @ParameterizedTest(name = "{0} days before check-in -> {1}")
    @CsvSource({
            "7, 10500.00",   // 7 or more: all nights + cleaning fee
            "6, 5500.00",    // 2 to 6: half the nights + cleaning fee
            "2, 5500.00",
            "1, 500.00",     // 1: no nights, but the cleaning fee (still before the check-in day)
            "0, 0.00",       // the check-in day: nothing
            "-2, 0.00"       // during the stay: nothing
    })
```

A failing test now says which rule broke: `R5 and R6: the guest cancels, the boundaries > 2 days
before check-in -> 5500.00`. That is **traceability**: from a rule to its test, and from a red test
back to the rule.

The tests were written from SPEC.md, not from the generated code. Tests written by reading the code
would check what the code does, not what it should do.

## Can failing tests replace the specification?

A tempting shortcut: skip the spec, send the model its failing tests and let it fix the code. Tried
with the weakest answer of topic 1 (run P2, 9 of 17), 3 runs. The prompt was
[`FEEDBACK-PROMPT.md`](FEEDBACK-PROMPT.md): the original request, P2's code and the failures:

```
PASSED 9/17
  FAIL guest 2 days before: 500.00 (expected 5500.00)
  FAIL guest on check-out day: 0.00 (expected IllegalArgumentException)
  FAIL host during the stay: 10500.00 (expected IllegalArgumentException)
  FAIL null cancelledBy: NullPointerException (expected IllegalArgumentException)
  ...
```

| Run | Passed | What went wrong now |
|---|---|---|
| F1 | 15 / 17 | now throws an exception when the **guest** cancels on the check-in day or during the stay (were right before) |
| F2 | 16 / 17 | now refunds half the nights 1 day before check-in (was right before) |
| F3 | 16 / 17 | now throws an exception when the guest cancels during the stay (was right before) |

Every run fixed the cases that were named, and every run broke a case that had been right. "Host
during the stay: expected IllegalArgumentException" does not say **why**; F1 guessed "no one may
cancel once the stay has started", and was wrong for the guest. Failing tests are examples, and
examples are ambiguous (Session 26, topic 3). The rules are what generalises.

## The reliable loop

1. Write the specification (topic 2) and get it reviewed: this is where the decisions are made.
2. Derive the acceptance tests from it, before or while the code is generated.
3. Generate the code from the specification (and your patterns, topic 3).
4. Run the tests. When one fails, decide: is the code wrong, or the specification? Fix **that**.
5. Keep the spec next to the code: it is the documentation of why the code does what it does.
