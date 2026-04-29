# Workshop: AI-assisted Code Refactoring

## What is Claude Code?

Claude Code is an AI assistant that runs directly in your terminal. Unlike a chat interface, it has full access to your codebase — it can read files, make edits, run commands, and reason across multiple files at once.

Start it in any project directory:
```bash
claude
```

You interact with it in natural language. It will read the relevant code, explain problems, and make changes directly. You review and approve each step.

---

## Starting a Greenfield Project with Claude Code

When starting a new project from scratch, Claude Code works best when you give it enough context upfront rather than one small request at a time.

### 1. Describe the full picture first

Instead of asking for one file at a time, describe the whole project in one prompt:

```
I want to build a REST API in Spring Boot with the following requirements:
- Manage tasks with title, description, priority (LOW/MEDIUM/HIGH) and due date
- Filter and paginate tasks
- PostgreSQL as database, Flyway for migrations
- Use constructor injection, DTOs, @ControllerAdvice for error handling
- Tests with MockMvc

Generate the initial project structure.
```

The more context you give upfront, the less back-and-forth you need.

### 2. Let Claude scaffold, then review

Claude will generate multiple files at once. Before accepting everything:
- Read through the key files (entity, controller, service)
- Check that patterns match your team's standards
- Ask follow-up questions: *"Why did you choose X over Y?"*

### 3. Iterate incrementally

Once the scaffold is in place, add features one at a time:
```
Add tag support — a task can have multiple tags, many-to-many relationship.
Also add filtering by tag to the list endpoint.
```

---

## What is CLAUDE.md for?

`CLAUDE.md` is a file in the root of your repository that Claude Code reads **automatically at the start of every session**. It gives Claude persistent context about your project so you don't have to re-explain things every time.

### What to put in CLAUDE.md

**Commands** — how to build, test, run the project:
```markdown
## Commands
- Run backend: `cd backend && ./gradlew bootRun`
- Run tests: `./gradlew test`
- Single test: `./gradlew test --tests "ClassName"`
```

**Architecture** — things that aren't obvious from reading individual files:
```markdown
## Architecture
- Controllers return DTOs, never JPA entities directly
- All service methods are @Transactional
- Frontend proxies /api/* to localhost:8080 via Vite
```

**Conventions** — team decisions Claude should follow:
```markdown
## Conventions
- Use constructor injection, never @Autowired on fields
- New endpoints need an integration test in *ControllerTest.java
- Error responses always use the ErrorResponse record
```

### What NOT to put in CLAUDE.md

- Generic advice like "write clean code" or "add error handling" — too vague to be useful
- Things that are obvious from reading the code
- Temporary notes or in-progress work — use comments or a TODO file for that

### The payoff

Without `CLAUDE.md`, Claude has to re-read the whole codebase each session to understand your patterns. With a good `CLAUDE.md`, it immediately knows your conventions and produces consistent output from the first prompt.

---

## Setup

**Prerequisites:** Java 21, Node.js 18+, Claude Code (`claude` in terminal)

```bash
# Start backend
cd backend
./gradlew bootRun

# Start frontend (new terminal)
cd frontend
npm install
npm run dev
```

Open the app: `http://localhost:5173`

---

## Module 1 — Explore the App *(15 min)*

Click through the app and note anything that stands out. Try the following scenarios:

1. **Delete a tag** that is still linked to todos
2. **Double-click a checkbox** quickly
3. **Switch filters rapidly** (All → Done → Open)

What are bugs? What are design problems?

**Done when:** You have at least 3 observations to discuss.

---

## Module 2 — Backend Refactoring *(60–90 min)*

Start Claude Code and ask it to explain the code quality issues:
```bash
claude
```

### Useful prompts to get started

```
Look at the backend code and explain the biggest code quality problems you find.
```
```
What's wrong with how dependencies are injected in TodoController?
```
```
How much business logic does TodoController.list() contain, and where should it live?
```
```
What happens when an endpoint throws an exception? How should this be handled in Spring?
```
```
Enable show-sql and tell me how many SQL queries GET /api/todos produces and why.
```

### Areas to investigate

**Dependency Injection**
- How are dependencies injected in `TodoController`, `TagController`, `TodoService` and `DataSeeder`?
- *Hint: There is a more modern approach that makes fields `final` and classes easier to test.*

**Controller vs. Service**
- How much logic is inside `TodoController.list()`?
- *Hint: A controller should only handle HTTP mapping — no business logic.*

**Error Handling**
- What happens when an endpoint throws an exception?
- *Hint: Spring has a central mechanism for exception handling across all controllers.*

**Database Queries**
- Enable `spring.jpa.show-sql=true` in `application.properties`. How many SQL queries does `GET /api/todos` produce?
- *Hint: It should be one query, not N+1.*

**API Design**
- What does the controller return directly? What problem does this cause?
- *Hint: Look up DTO — Data Transfer Object.*

**More things to find**
- Logging: `System.out.println` vs. a proper logging framework
- Validation: How is the request body validated?
- Secrets: Check `application.properties`

### Done when:
- [ ] Constructor injection in all 4 classes
- [ ] Filter/sort/pagination logic moved to `TodoService`
- [ ] `@ControllerAdvice` handling exceptions centrally
- [ ] N+1 resolved — single query for todos with tags
- [ ] DTOs returned from all endpoints
- [ ] No `System.out.println` left in the codebase

---

## Module 3 — Frontend Refactoring *(60–90 min)*

### Useful prompts to get started

```
What are the biggest code quality problems in the frontend code?
```
```
What type does the todos state have in App.tsx, and why is that a problem?
```
```
What happens in the UI when an API call fails? How should it be handled?
```
```
How many useState calls are in App.tsx? Which ones could be grouped?
```
```
What key prop does TodoList use for rendering items, and why is that wrong?
```

### Areas to investigate

**TypeScript**
- Check `App.tsx` — what type do `todos` and the handler parameters have?
- Check `types.ts` — is this file imported anywhere?
- *Hint: `any` effectively disables TypeScript.*

**API Calls**
- Where are `fetch()` calls made? What happens when the server returns 500?
- *Hint: `fetch()` does not throw on HTTP error codes — `res.ok` must be checked manually.*

**Component Structure**
- Count the `useState` calls in `App.tsx`. Which ones belong together?
- Which props are passed through multiple layers without being used?

**List Rendering**
- What `key` prop does `TodoList` use? Why is this problematic?

**More things to find**
- Loading and error states: What does the user see while loading or on error?
- Form validation: What happens when submitting with an empty title?
- Inline styles vs. CSS classes

### Done when:
- [ ] No `any` types — `Todo` and `Tag` imported from `types.ts` everywhere
- [ ] All `fetch()` calls centralized in `api.ts` with `res.ok` checks
- [ ] Loading and error states visible in the UI
- [ ] `key={i}` replaced with `key={todo.id}` / `key={tag.id}`
- [ ] Unused `selectedFilter` prop removed

---

## Module 4 — Bug Hunt *(45 min)*

Use DevTools → Network → **Slow 3G** to make the bugs more visible.

| Bug | How to reproduce |
|-----|-----------------|
| Toggle Race Condition | Double-click a checkbox quickly |
| Filter Race Condition | Switch filters rapidly |
| Date Off-by-one | DevTools → Sensors → Timezone override to "America/Los_Angeles" |
| Tag-Delete Crash | Delete a tag linked to todos → read the error in the backend log |

### Useful prompts

```
Why does double-clicking a checkbox leave the todo in the wrong state?
```
```
What happens when filters are switched rapidly and multiple requests are in flight?
```
```
Why does new Date("2026-05-15").toLocaleDateString() sometimes return the wrong date?
```
```
Why does deleting a tag that is linked to todos crash with a 500 error?
```

*Hint for race conditions: Look at how React state updates and async requests interact.*

### Done when:
- [ ] Double-clicking a checkbox toggles correctly every time
- [ ] Rapid filter switching never shows stale results
- [ ] Due dates display correctly in all timezones
- [ ] Deleting a linked tag no longer crashes

---

## Module 5 — Prompt Patterns, Limits & Workflow *(20 min)*

### Good vs. bad prompts

The quality of Claude's output depends heavily on how you ask. Compare:

| Weak | Strong |
|------|--------|
| "Fix my code" | "The `findFiltered()` method returns all todos when `tagId` is null — it should return all todos regardless of tag. Here's the current code: ..." |
| "Add tests" | "Write integration tests for `TodoController` covering: 400 on blank title, 404 on unknown ID, and filtering by `done=true`" |
| "Refactor this" | "Extract the filter and sort logic from `TodoController.list()` into `TodoService`. Keep the existing method signatures." |

**The pattern:** Context + specific problem + expected outcome. The more Claude knows about what you want, the less you need to correct it.

### Use Claude to review your own code

Before opening a PR, ask Claude to review your changes:
```
Review the changes I just made to TodoController and TodoService.
Check for: correctness, edge cases, security issues, and anything that
doesn't match the patterns in the rest of the codebase.
```

Or target a specific concern:
```
Are there any scenarios where the new tag-delete logic could leave
the database in an inconsistent state?
```

### Use Claude for git workflow

Small but high-value use cases:
```
Write a commit message for my staged changes.
```
```
Write a PR description summarizing what changed and why.
```
```
What would be a good branch name for a feature that adds tag filtering?
```

### Generate tests with Claude

Give Claude an existing service and ask it to find edge cases:
```
Look at TodoService.findFiltered() and write tests that cover
all the edge cases you can think of — not just the happy path.
```
Claude will often find cases you'd have missed: null inputs, empty results, boundary conditions.

---

### Where Claude works well

- Spotting patterns and anti-patterns across many files
- Boilerplate-heavy tasks: DTOs, mappers, test scaffolding
- Explaining unfamiliar code or frameworks
- Generating first drafts that you refine
- Commit messages, PR descriptions, documentation

### Where Claude falls short

- **Domain knowledge** — Claude doesn't know your business rules. "A todo is overdue if..." needs to come from you.
- **Architecture decisions** — Claude will implement whatever you ask. It won't push back on a bad design choice unless you explicitly ask it to.
- **Large cross-cutting refactors** — changing a pattern across 50 files in one shot often produces inconsistent results. Break it into steps.
- **Security-critical code** — always review cryptography, auth, and data handling yourself. Claude can make subtle mistakes here.
- **Production database migrations** — never let Claude write and run migrations on prod data without human review.

### The right mental model

Claude is a **senior pair-programming partner who is very fast but doesn't know your domain**. It can implement anything you describe clearly. The better you can articulate what you want — and the more you push back when something looks wrong — the better the results.

---

## Reference

- Complete solution: branch `workshop-reference`
- Repository: https://github.com/kronsteinerT/team-tooling-ai-workshop
