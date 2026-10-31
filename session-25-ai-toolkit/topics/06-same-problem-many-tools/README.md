# Topic 6: The same problem, two tools, one set of tests

The only fair way to compare AI tools: give them **the same problem, the same starting files, and no
help**, then judge the results with checks they never saw.

## The setup

- **The problem:** [`PROBLEM.md`](PROBLEM.md). Compute the price of a stay for the Airbnb project:
  weekend nights cost 20 % more, 7+ nights get 10 % off, a cleaning fee is added after the discount,
  and only the final total is rounded. Plus input checks.
- **The starting files:** for each tool, a new empty folder with just `PROBLEM.md` and the
  interface [`PriceCalculator`](../../src/main/java/jbdl/session25/compare/PriceCalculator.java).
  No repository, no `AGENTS.md`, no tests.
- **The task**, the same for both (only the tool name differs):

  ```
  Solve the task in PROBLEM.md. The interface is in src/jbdl/session25/compare/PriceCalculator.java.
  <tool> is codex, so write src/jbdl/session25/compare/codex/CodexPriceCalculator.java.
  You may compile with javac to check your work.
  ```

- **The judge:** [`PriceCalculatorContract`](../../src/test/java/jbdl/session25/compare/PriceCalculatorContract.java),
  12 tests written before the tools ran, including the traps in the rules: a stay over New Year,
  30 nights allowed but 31 not, rounding only at the end (rounding each night gives 566.66 instead
  of 566.67), the result always with 2 decimals, `IllegalArgumentException` for `null`.

```java
public abstract class PriceCalculatorContract {

    /** The solution under test; each subclass returns its own. */
    protected abstract PriceCalculator createCalculator();

    @Test
    void sevenNightsGetTenPercentOffTheNightsButNotOffTheCleaningFee() {
        // (5 x 1000 + 2 x 1200) x 0.9 + 500 = 6660 + 500
        assertThat(calculator.total(PRICE, MONDAY, MONDAY.plusDays(7))).isEqualTo(new BigDecimal("7160.00"));
    }
    ...
}

class CodexPriceCalculatorTest extends PriceCalculatorContract {
    @Override
    protected PriceCalculator createCalculator() {
        return new CodexPriceCalculator();
    }
}
```

The tests live in an abstract class; each tool gets a three-line subclass, and JUnit runs all 12
tests once per subclass.

## The results

Both solutions were copied into this module **unchanged**:
[`ClaudePriceCalculator`](../../src/main/java/jbdl/session25/compare/claude/ClaudePriceCalculator.java)
and [`CodexPriceCalculator`](../../src/main/java/jbdl/session25/compare/codex/CodexPriceCalculator.java).

| | Claude Code | Codex |
|---|---|---|
| Version | 2.1.316 | codex-cli 0.163.0 |
| Model | claude-opus-5-5 | gpt-6-luna, reasoning effort xhigh (this PC's setting) |
| Time | 36 s (44 s with start-up) | 111 s |
| Steps | 8 turns | 11 shell commands |
| Tokens | 176,263 in (176,251 of them written to or read from the cache) + 3,236 out | 35,511 (as reported; another tokenizer, not comparable) |
| Cost | $0.2286 at API prices (here paid by a subscription) | not shown in dollars (ChatGPT plan) |
| Checked its own work | yes: `javac --release 17 -Xlint:all -Werror` passed | no: the sandbox blocked `javac` from writing class files; it **said so** |
| Hidden tests | **12 of 12** | **12 of 12** |
| Course build (`-Xlint:all -Werror`, Java 17 and 25) | passes | passes |
| Size | 81 lines | 52 lines |

What Claude Code said when it finished (shortened):

```
I wrote src/jbdl/session25/compare/claude/ClaudePriceCalculator.java, and it compiles cleanly with
javac --release 17 -Xlint:all -Werror. I didn't run it on any sample stay: the quick check I tried
needed approval, so I skipped it.
...
Worked example: 7 nights from Mon 2 Nov 2026 to Mon 9 Nov at 1000 per night should give
(5 x 1000 + 2 x 1200) x 0.9 + 500 = 7160.00.
```

And Codex:

```
Implemented the stay pricing rules in CodexPriceCalculator.java.

`javac -Xlint:all -Werror` reported an error while writing the output class file, without reporting
a source error, so compilation could not be confirmed.
```

## Same result, different code

Both are correct, but they read differently:

| | Claude Code | Codex |
|---|---|---|
| Class Javadoc | lists all five rules | one line |
| The numbers 30 and 7 | named constants `MAX_NIGHTS`, `DISCOUNT_MIN_NIGHTS` | written inline |
| Price of one night | own method `priceOfNight` | inside the loop |
| Loop over the nights | `for (LocalDate night = checkIn; night.isBefore(checkOut); ...)` | `for (long offset = 0; offset < nights; offset++)` |
| Extras | an empty public constructor with Javadoc (not needed) | none |

Which one is better is a matter of taste and of your team's rules, which is exactly what an
`AGENTS.md` (topic 5) and a specification (Session 27) pin down.

## What this comparison does and does not show

- **One run is a sample of one.** Run the same prompt again and you get different code. This
  happened here: the first Codex run waited for keyboard input (topic 2). Stopping the waiting command
  closed that input, so the run went on working at the same time as a new run, and the two runs wrote different code into the
  same folder. That attempt was thrown away and
  Codex ran again in a fresh folder; the table shows the clean run.
- **The problem was small and clearly specified.** Both tools got it right because the rules left
  little to guess. Vaguer tasks spread the results much further apart (Session 26).
- **Time and tokens depend on settings** (reasoning effort, model, the machine), not only on the tool.
- **Tests decide, not looks.** Both solutions look right; only the 12 tests show that they are.

## Try a third tool

1. Give a chat model or your IDE assistant the text of [`PROBLEM.md`](PROBLEM.md) with
   `<tool>` set to its name (e.g. `copilot`).
2. Save its class as `src/main/java/jbdl/session25/compare/copilot/CopilotPriceCalculator.java`.
3. Add `CopilotPriceCalculatorTest extends PriceCalculatorContract` (copy one of the two).
4. `./mvnw -pl session-25-ai-toolkit verify` and compare.
