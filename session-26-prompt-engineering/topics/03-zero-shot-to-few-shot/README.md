# Topic 3: From zero-shot to few-shot

- **Zero-shot:** the prompt describes the task, with no example of a right answer.
- **Few-shot:** the prompt also shows a few inputs with their right outputs. The model copies the
  pattern, including rules you never wrote down.

The strategy: **start zero-shot, and add examples when the answers miss.** Examples cost a few
lines; they are the fastest way to say "like this".

## The experiment

The task: a URL slug for an Airbnb listing title. The real rules of our platform, which a test
checks with 12 cases the model never saw:

- lowercase, only a-z, 0-9 and single dashes; accents removed (`Café` -> `cafe`)
- `&` becomes `and` (`Rock&Roll Loft` -> `rock-and-roll-loft`)
- at most 40 characters, never cut inside a word (unless one word alone is longer than 40)
- if nothing is left, the slug is `listing` (`!!!` -> `listing`)

Three prompts, each sent 3 times to Claude Haiku 5.5 (`claude -p --model haiku`, one turn, no tool
calls), all rendered by [`SlugPrompts`](../../src/main/java/jbdl/session26/prompt/SlugPrompts.java):

| Prompt | What it adds | File |
|---|---|---|
| A, zero-shot, vague | one sentence | [prompt-A.txt](prompts/prompt-A.txt) |
| B, zero-shot, structured | role, context, task, constraints, format (topic 2) | [prompt-B.txt](prompts/prompt-B.txt) |
| C, few-shot | B + 5 examples | [prompt-C.txt](prompts/prompt-C.txt) |

The examples of C (none of them is one of the 12 test cases):

```
Examples:
Input: Sunny Room & Garden
Output: sunny-room-and-garden
Input: Hôtel Été
Output: hotel-ete
Input:   Loft -- 2 Beds 
Output: loft-2-beds
Input: @@@
Output: listing
Input: Bright two bedroom apartment near the central railway station
Output: bright-two-bedroom-apartment-near-the
```

Each answer's code was compiled with `javac --release 17 -Xlint:all -Werror` (all 9 compiled) and
run against the 12 hidden cases.

## The results

| Run | Passed | Time | Output tokens | Cost |
|---|---|---|---|---|
| A1 | 6 / 12 | 6.1 s | 859 | $0.0031 |
| A2 | 6 / 12 | 9.1 s | 1,817 | $0.0036 |
| A3 | 6 / 12 | 9.5 s | 1,724 | $0.0036 |
| B1 | 6 / 12 | 7.6 s | 1,421 | $0.0035 |
| B2 | 6 / 12 | 7.1 s | 1,310 | $0.0034 |
| B3 | 7 / 12 | 13.6 s | 2,005 | $0.0038 |
| C1 | 9 / 12 | 13.0 s | 2,755 | $0.0042 |
| C2 | **12 / 12** | 15.9 s | 3,610 | $0.0041 |
| C3 | **12 / 12** | 16.9 s | 3,753 | $0.0042 |

What A and B got wrong, every time:

```
Bed & Breakfast -> bed-breakfast (expected bed-and-breakfast)
Rock&Roll Loft -> rock-roll-loft (expected rock-and-roll-loft)
Beautiful sunny apartment close to the beach and the old town
   -> beautiful-sunny-apartment-close-to-the-beach-and-the-old-town (expected beautiful-sunny-apartment-close-to-the)
!!! -> "" (expected listing)            (B3 got this one right)
...
```

Nothing wrong with the code: these were **our** rules, and A and B never stated them. The model wrote
a reasonable slug function, just not ours.

What C1 got wrong:

```
Beautiful sunny apartment close to the beach and the old town
   -> beautiful-sunny-apartment-close-to-th (expected beautiful-sunny-apartment-close-to-the)
```

C1 wrote `private static final int MAX_LENGTH = 37;`. The long example's output happens to be 37
characters long, and C1 took that as the limit, and also cut inside words. C2 and C3 read the same
example as "at most 40, cut at a word". **An example shows one answer; the rule behind it is a
guess.** Topic 5 fixes that by writing the rule down (prompt D: 12, 12, 12).

## The winner

C2's class, copied unchanged:
[`ListingSlug`](../../src/main/java/jbdl/session26/slug/ListingSlug.java), with
[`ListingSlugTest`](../../src/test/java/jbdl/session26/slug/ListingSlugTest.java) (the 12 hidden
cases and the 5 examples). It even explains its steps for beginners:

```java
// Step 3: write "&" as the word "and".
text = text.replace("&", " and ");

// Step 4: every run of anything that is not a-z or 0-9 becomes one dash.
String slug = NOT_ALLOWED.matcher(text).replaceAll("-");
```

## Lessons

- Zero-shot answers follow **common** conventions. If your rules differ, the model cannot know.
- A role and constraints change the form (names, package, Javadoc), not the hidden rules.
- Examples carry rules you forgot to write, and the edge cases you choose for them matter most.
- Examples are ambiguous. When a rule matters, write it **and** show it.
- Three runs of the same prompt gave 9, 12 and 12: always test more than one answer.
