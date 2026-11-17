# Topic 1: A map of AI tools

"AI" in a developer's day means four different things. They are built on the same core (a large
language model) but solve different problems.

```
                 +---------------------------------------------+
                 |  LLM: a model that continues text            |
                 |  (Claude, GPT, Gemini, Llama, Mistral, ...)  |
                 +---------------------------------------------+
                    |              |               |             |
             chat window     inside the IDE    + web search   + tools + a loop
                    |              |               |             |
              Chat models    Code assistants    AI search      Agents
```

## 1. The LLM (large language model)

A program trained on a huge amount of text and code. It gets text in (the **prompt**) and writes
the most likely continuation, one **token** (a piece of a word) at a time.

- It has no memory between requests: everything it should know must be in the prompt. The
  maximum the prompt plus the answer can hold is the **context window** (topic 3).
- It knows what was in its training data, up to a date (the **knowledge cutoff**). It does not
  know your code, your company, or a library version released after that date, unless you give it.
- It is not a database: it can write a confident, wrong answer (a **hallucination**, topic 4).

You reach a model through an **API** (an HTTP endpoint, paid per token) or through a product built
on it. Everything below is such a product.

## 2. Code assistants

An LLM inside the editor. It sees the file you are in (and often more of the project) and suggests
code: completing the line you type, writing a method from a comment, explaining a selection, or
answering questions about the project in a side panel. Examples: GitHub Copilot, the JetBrains AI
Assistant in IntelliJ IDEA, Cursor.

Best for: small steps while you type, where you read every suggestion immediately.

## 3. AI search

An LLM that searches the web first and answers from the pages it found, with links to them.
Examples: Perplexity, the search modes of ChatGPT, Claude and Gemini, Google's AI answers.

Best for: questions about current things the model cannot know ("what is the newest Spring Boot
version?", an error message from a library released last month). Always open the linked source:
the summary can still be wrong.

## 4. Agents

An LLM that can **act**: it gets tools (read a file, run a command, search, edit), decides which one
to use, looks at the result, and repeats until the task is done. This loop is the difference:

```
task -> model thinks -> calls a tool (e.g. "javac ...") -> reads the result -> thinks again -> ... -> done
```

Examples for developers: Claude Code, OpenAI Codex, Gemini CLI, and the "agent modes" of Copilot
and Cursor. In topic 6, both agents read the problem, wrote a class, and tried to compile it, all
without help.

Best for: tasks with several steps across files, where the result can be checked by running
something (tests, the compiler). The risk grows with what the agent may do (topic 5).

## How to choose

| You want to ... | Use |
|---|---|
| understand a concept, discuss a design | a chat model |
| write the next lines faster | a code assistant in the IDE |
| know something current, with sources | AI search |
| get a multi-step change done and tested | an agent |
| build AI into your own application | the model's API (Session 28 builds on this) |
