# Topic 1: From prompts to structured specifications

A prompt is a request in a chat. A **specification** is a document the code must agree with. The
difference is not length, but that every rule in a specification can be checked.

## The request, as people write it

[`PROMPT.md`](PROMPT.md) gives the types and then says:

```
Guests get a full refund if they cancel a week or more before check-in, half if they cancel a few
days before, and nothing if they cancel at the last minute. If the host cancels, the guest gets
everything back. The cleaning fee should be refunded if the guest never arrives. Handle bad input
sensibly. Java 17, only the JDK.
```

Clear to a person in the same company. To a model, every phrase hides a question:

| Phrase | The question it leaves open | What the 5 runs decided |
|---|---|---|
| "a week or more" | 7 days counted how? | all 5: 7 or more days before check-in (as wanted) |
| "a few days" | from how many days? | 3 runs: 2 to 6 days; **2 runs: 3 to 6 days** |
| "the last minute" | 1 day? the same day? | follows from the "few days" choice |
| "never arrives" | is cancelling on the check-in day "never arriving"? | **1 run: yes, cleaning fee back on the check-in day**; 4 runs: no |
| "the host cancels" | also during the stay? | **all 5: yes, full refund at any time** (we wanted: not allowed) |
| (nothing said) | cancelling after check-out? | **all 5: allowed, refund 0** (we wanted: an error) |
| "handle bad input sensibly" | null? negative? which exception? | **4 of 5: a `NullPointerException` for null** (we wanted `IllegalArgumentException`) |

## The result

Each answer was run against 17 hidden test cases (the rules of the specification in topic 2):

| Run | Passed |
|---|---|
| P1 | 10 / 17 |
| P2 | 9 / 17 |
| P3 | 11 / 17 |
| P4 | 10 / 17 |
| P5 | 14 / 17 |

The code was fine Java in every run: it compiled with `-Xlint:all -Werror`, used `BigDecimal`, and
rounded correctly. What was wrong were the **decisions**, and the prompt had left them to the model.

## From prompt to specification

| Prompt | Specification |
|---|---|
| describes what you want | states what must be true |
| words like "a few", "sensibly", "quick" | numbers, boundaries, named exceptions |
| one paragraph | sections: goal, interface, definitions, rules, examples, not in scope |
| the edge cases are left to the model | the edge cases are listed, with the expected result |
| hard to test against | every rule becomes a test (topic 4) |

The same feature as a specification is topic 2. With it, the same model passed all 17 cases in 5 of
5 runs.
