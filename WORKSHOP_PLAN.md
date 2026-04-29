# Workshop: AI-assisted Code Refactoring

## What is Claude Code?

Claude Code is an AI assistant that runs directly in your terminal. Unlike a chat interface, it has full access to your codebase — it can read files, make edits, run commands, and reason across multiple files at once.

Start it in any project directory:
```bash
claude
```

You interact with it in natural language. It will read the relevant code, explain problems, and make changes directly. You review and approve each step.

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

## Reference

- Complete solution: branch `workshop-reference`
- Repository: https://github.com/kronsteinerT/team-tooling-ai-workshop
