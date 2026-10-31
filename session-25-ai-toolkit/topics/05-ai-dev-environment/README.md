# Topic 5: An AI-ready development environment

An AI tool is only as good as what it knows about your project and what it may do in it. Setting
it up well takes four steps.

## 1. Install the tools

| Tool | Install | Sign in |
|---|---|---|
| GitHub Copilot in IntelliJ IDEA | Settings > Plugins > "GitHub Copilot" | GitHub account with Copilot |
| JetBrains AI Assistant | Settings > Plugins > "JetBrains AI Assistant" | JetBrains account |
| Claude Code (CLI agent) | see the official docs (native installer or npm) | Claude subscription or API key |
| Codex CLI (CLI agent) | `npm install -g @openai/codex` | ChatGPT plan or API key |

Check the versions you got, as in topic 2 (`claude --version`, `codex --version`). Agents need
the same tools you need: Java 17+, Maven (here the wrapper `./mvnw`), git, and Docker for the
Testcontainers tests.

## 2. Tell the tools how the project works: `AGENTS.md`

Every agent starts each session knowing nothing about your project. An **instructions file** in
the repository is read automatically at the start of every session:

- [`AGENTS.md`](../../../AGENTS.md): read by Codex and several other tools (an open convention).
- [`CLAUDE.md`](../../../CLAUDE.md): read by Claude Code. Ours has one line, `@AGENTS.md`, which
  imports the other file, so the rules are written only once.

What belongs in it is what a new colleague needs on day one, and nothing else:

```markdown
## Build and test
- `./mvnw verify` builds everything and runs all tests. One module: `./mvnw -pl <module> verify`.
- The compiler runs with `-Xlint:all -Werror`: every warning fails the build.

## Rules for code
- **Keep it simple for a beginner.** Prefer a plain loop and if/else. If a more advanced construct
  is used ..., add a comment that explains it and shows the simpler version.
```

Short and specific beats long and general: "run `./mvnw verify` before saying a change is done" is
an instruction; "write good code" is not.

Did it work? Both tools were asked the same question in the repository, with Claude Code not
allowed to open any file:

```
$ claude -p "Without opening any files, answer from the instructions you were given for this
  repository: what must code in this repository look like? Answer in at most 4 short bullets."
  --model haiku --allowedTools ""
- Simple for a beginner: use plain loops and if/else. If you use a more advanced construct
  (streams, switch expressions, Optional chains, generics tricks), add a comment explaining it and
  show the simpler version.
- Naming and docs: packages are jbdl.sessionNN.<topic>. Each test class has a class Javadoc saying
  which topic it belongs to, and each test step has a short comment.
- Build: it must compile on Java 17 (CI also runs on 25) with -Xlint:all -Werror, so no warnings.
  Docker-dependent tests use @Testcontainers(disabledWithoutDocker = true).
- Dependencies and secrets: add no new library without a reason written in the pom, and keep
  versions in the root pom.xml. Never commit secrets; passwords come from environment variables,
  and examples use obvious demo values.

$ codex exec -s read-only -C . "<the same question>"
- Keep code simple and beginner-friendly; explain advanced constructs with a simpler alternative.
- Use `jbdl.sessionNN.<topic>` packages; give each test class a topic Javadoc and brief comments
  for test steps.
- Avoid new libraries unless the reason is recorded in the POM; keep versions in the root POM.
- Never commit secrets; read passwords from environment variables and use obvious demo values in
  examples.
```

Both answers come from `AGENTS.md` alone. Without it, the tools would have had to guess or search.

## 3. Decide what the agent may do

Agents ask before risky actions, and you can widen or narrow that:

| Setting | Effect |
|---|---|
| Codex `-s read-only` | may read, may not change files or run commands that write |
| Codex `-s workspace-write` | may change files inside the project folder only |
| Claude Code `--allowedTools "Read,Edit,Bash(./mvnw:*)"` | only these tools run without asking |
| Claude Code `--permission-mode acceptEdits` | file edits without asking, commands still ask |

Start narrow. Never give an agent production credentials, and keep secrets out of the folder it
works in (environment variables, not files in the repository; `.gitignore` for local files).

The sandbox has a price: in topic 6, Codex's Windows sandbox did not let `javac` write class
files, so it could not check its own code. Know what your sandbox blocks.

## 4. Give it a fast way to check its work

Agents are much better when they can **run something** that says right or wrong: a compiler, a
test suite, a linter. In this repository that is `./mvnw -pl <module> verify` (seconds for one
module) and CI on every pull request. A project without tests gets confident, untested changes.

## Checklist

- [ ] Tools installed and signed in; versions noted
- [ ] `AGENTS.md` (and `CLAUDE.md` importing it) with build, test and code rules
- [ ] Permissions start narrow; no secrets in the working folder
- [ ] One command that builds and tests, and CI that runs it on every change
- [ ] You review every change before it is merged
