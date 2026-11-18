# Topic 2: Anatomy of a developer prompt

A model only knows what is in the prompt (Session 25, topic 1). A good developer prompt answers the
questions a new colleague would ask before starting:

| Part | Question it answers | Example |
|---|---|---|
| **Role** | who should I be? | a senior Java developer on an Airbnb-like rental platform |
| **Context** | what do I need to know about the project? | hosts type any title, in any language; the slug goes into `/listings/42/<slug>` |
| **Task** | what exactly should I do? | write the class `ListingSlug` with `public static String of(String title)` |
| **Constraints** | what are the rules? | Java 17, only the JDK; only a-z, 0-9 and single dashes |
| **Examples** | what does right look like? (topic 3) | `Sunny Room & Garden` -> `sunny-room-and-garden` |
| **Answer format** | what should the answer look like? | only one Java code block, no explanation |

Only the task is required. The others turn a guess into an instruction.

## The code

[`DeveloperPrompt`](../../src/main/java/jbdl/session26/prompt/DeveloperPrompt.java) builds a prompt
from these parts and renders them in a fixed order; parts that were not set are left out:

```java
String prompt = DeveloperPrompt.task("Write the class ListingSlug with one method, ...")
        .role("a senior Java developer on an Airbnb-like rental platform")
        .context("Hosts type any title, in any language, with any punctuation. ...")
        .constraint("Java 17, only the JDK, package jbdl.session26.slug")
        .constraint("The slug contains only a-z, 0-9 and single dashes, no dash at the start or end")
        .constraint("Beginner-readable code with a class Javadoc")
        .answerFormat("Only one Java code block with the complete class, no explanation.")
        .render();
```

Each method returns the prompt itself (`return this;`), so the calls can be chained; this is the
builder style from Session 1's `Car`. Rendered, it is prompt B of topic 3,
[`prompts/prompt-B.txt`](../03-zero-shot-to-few-shot/prompts/prompt-B.txt):

```
You are a senior Java developer on an Airbnb-like rental platform.

Context:
Hosts type any title, in any language, with any punctuation. The slug goes into URLs like /listings/42/<slug>.

Task:
Write the class ListingSlug with one method, public static String of(String title), that turns a listing title into the slug used in the listing's URL.

Constraints:
- Java 17, only the JDK, package jbdl.session26.slug
- The slug contains only a-z, 0-9 and single dashes, no dash at the start or end
- Beginner-readable code with a class Javadoc

Answer format:
Only one Java code block with the complete class, no explanation.
```

[`DeveloperPromptTest`](../../src/test/java/jbdl/session26/prompt/DeveloperPromptTest.java) checks
the order, that empty parts are left out, and that every prompt file in this session is exactly what
the code renders.

## Why a structure helps, and what it does not do

Compare with prompt A, one sentence: "Write a Java method that turns a listing title into a URL
slug." The model answered A with a class it named itself (`Slugs.slugify`, `SlugUtils.toSlug`) in no
package; with B it wrote exactly `jbdl.session26.slug.ListingSlug.of`, in the right package, with a
Javadoc. **Structure controls the shape of the answer.**

But B did not pass more hidden tests than A (topic 3: 6, 6, 7 against 6, 6, 6). The rules that
mattered (`&` becomes `and`, at most 40 characters, `listing` when nothing is left) were in neither
prompt. **No structure replaces the facts the model needs.** Topics 3 and 5 add them.

## Tips

- Say **what**, not only **how**: "for URLs like `/listings/42/<slug>`" explains why dashes matter.
- Write constraints as checkable rules ("at most 40 characters"), not wishes ("short").
- Ask for a format you can use directly: one code block, or JSON, or `TOTAL: <number>` (topic 4).
- Keep the stable parts (role, context, constraints) in a file or a class like this one: the same
  context every time, and an `AGENTS.md` (Session 25) for agents.
