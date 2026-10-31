# Topic 2: Writing a clear specification before generating code

[`SPEC.md`](SPEC.md) is the refund feature written as a specification. It is one page, and every
section has a job:

| Section | Its job | In SPEC.md |
|---|---|---|
| **Goal** | why the feature exists; helps with cases the rules forget | the amount is shown to the guest and refunded exactly |
| **Interface** | the exact names and types the code must use | `StandardRefundPolicy implements RefundPolicy`, the existing types |
| **Definitions** | words that need one meaning | "days before check-in" = `ChronoUnit.DAYS.between(cancelledOn, checkIn)`: 1 the day before, 0 on the day, negative during the stay |
| **Rules** | numbered, each one checkable | R1 to R7 |
| **Examples** | inputs with the expected result, each linked to its rules | 6 rows, including one exception |
| **Not in scope** | what not to build | early departure, other currencies, the payment |
| **Answer format** | what to hand back | one Java code block, Java 17, only the JDK |

## How the rules are written

```
| R5 | Guest cancels: the nights part of the refund depends on the days before check-in: 7 or more: 100 %
       of nightsTotal; 2 to 6: 50 %; 1 or fewer (including during the stay): 0 %. |
| R6 | Guest cancels before the check-in day (days before check-in >= 1): the whole cleaning fee is
       refunded too. On or after the check-in day: nothing of it. |
```

- **Boundaries as numbers, both ends:** "2 to 6", "7 or more", "1 or fewer". No "a few".
- **One defined word for one idea:** "days before check-in" is defined once and then used everywhere.
- **Errors are part of the behaviour:** R1, R2 and R4 name the exception, not "handle sensibly".
- **The order of operations when it matters:** R7, "add the parts first, then round once".

## The result

| Run | Passed | Lines | Time |
|---|---|---|---|
| S1 | 17 / 17 | 83 | 12.9 s |
| S2 | 17 / 17 | 65 | 9.4 s |
| S3 | 17 / 17 | 68 | 11.6 s |
| S4 | 17 / 17 | 67 | 10.7 s |
| S5 | 17 / 17 | 62 | 11.9 s |

Every run correct, and faster than the prompt runs (15 to 18 s): fewer things to decide.
16 of the 17 test cases were not among the spec's 6 examples; the rules covered them.

## Writing it costs time; is it worth it?

The specification took longer to write than the prompt. But it is written once and then used for
the code, the tests (topic 4), the review and the documentation. And the questions in topic 1
("a few days?", "may the host cancel during the stay?") have to be answered by someone anyway. The
only choice is whether a person answers them, or the model guesses.

## A checklist for your own specs

- [ ] Can each rule be checked by a test? If not, rewrite it.
- [ ] Are there words like "few", "soon", "sensible", "fast", "nice"? Replace them with numbers.
- [ ] Is every boundary stated on both sides (`>= 7`, `2 to 6`, `<= 1`)?
- [ ] Does each error case name what happens (which exception, which HTTP status)?
- [ ] Is there an example for every rule, and one for each boundary that is easy to get wrong?
- [ ] Is it clear what is **not** part of the feature?
