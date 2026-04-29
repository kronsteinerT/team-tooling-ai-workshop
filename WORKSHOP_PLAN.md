# Workshop: AI-assisted Code Refactoring

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

## Module 1 — Explore the App (15 min)

Click through the app and note anything that stands out. Try the following scenarios:

1. **Delete a tag** that is still linked to todos
2. **Double-click a checkbox** quickly
3. **Switch filters rapidly** (All → Done → Open)

What are bugs? What are design problems?

---

## Module 2 — Backend Refactoring

Start Claude Code and ask it to explain the code quality issues:
```bash
claude
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

---

## Module 3 — Frontend Refactoring

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

---

## Module 4 — Bug Hunt

Use DevTools → Network → **Slow 3G** to make the bugs more visible.

| Bug | How to reproduce |
|-----|-----------------|
| Toggle Race Condition | Double-click a checkbox quickly |
| Filter Race Condition | Switch filters rapidly |
| Date Off-by-one | DevTools → Sensors → Timezone override to "America/Los_Angeles" |
| Tag-Delete Crash | Delete a tag linked to todos → read the error in the backend log |

*Hint for race conditions: Look at how React state updates and async requests interact.*

---

## Reference

Complete solution: branch `workshop-reference`
