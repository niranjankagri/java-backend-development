# Topic 2: Chat models, IDE copilots and CLI agents compared

The three ways developers use AI every day differ in one thing above all: **who moves the
information** between the AI and your project.

| | Chat model (browser or app) | IDE copilot | CLI agent |
|---|---|---|---|
| Examples | ChatGPT, Claude, Gemini | GitHub Copilot, JetBrains AI Assistant, Cursor | Claude Code, Codex CLI, Gemini CLI |
| What it sees | only what you paste | the open file, often parts of the project | the whole folder it runs in (it reads files itself) |
| What it can do | write text | suggest and insert code | read, edit, run commands, run tests, use git |
| Who copies code around | you | the IDE | the agent |
| Who checks the result | you | you, while typing | the agent runs tests; you review the change |
| Typical task size | a question, a snippet | a line to a method | a feature or a bug across several files |
| Main risk | out-of-date or invented answers | accepting a suggestion without reading it | a large change nobody read; commands with side effects |

## Chat models

You paste code and a question, you get an answer, you copy it back. Simple and safe (it cannot
change anything), but you are the courier: what you do not paste, it does not know. Good for
learning ("explain `@Transactional` propagation"), design discussions and reviews of one class.

## IDE copilots

Suggestions appear as you type (press Tab to accept), and a chat panel knows about the open files.
Fast for the small steps of writing code. Read every suggestion: it looks plausible by design.

## CLI agents

You give a task in the terminal; the agent works in your project folder. The two used in this
session, as installed on the PC this course was written on:

```
$ claude --version
2.1.316 (Claude Code)
$ codex --version
codex-cli 0.163.0
```

Both also run without a human in the loop ("headless"), which topic 6 used for a fair comparison:

```bash
claude -p "<task>" --permission-mode acceptEdits --allowedTools "Read,Write,Edit,Glob,Bash(javac:*)" --output-format json
codex exec -s workspace-write -C <folder> "<task>" < /dev/null
```

(Without `< /dev/null`, `codex exec` waited for more input from the keyboard and never started. A
real first-try problem from this session.)

## They work together

Most developers use more than one: chat to think, the copilot to type, an agent for the bigger
changes. The skills from Sessions 26 and 27 (clear prompts, written specifications) make all three
better.
