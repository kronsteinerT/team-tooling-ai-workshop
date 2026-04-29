# Workshop: AI-assisted Code Refactoring

**Duration:** ~3.5–4 hours
**Target audience:** Developers with Java/Spring Boot basics; React experience helpful but not required
**Goal:** Show how Claude Code helps identify and fix realistic code quality problems

---

## Preparation (Trainer)

- Have all participants check out branch `workshop-start`
- Ensure Java 21 and Node.js 18+ are available on all machines
- Start backend: `cd backend && ./gradlew bootRun`
- Start frontend: `cd frontend && npm install && npm run dev`
- Claude Code installed and logged in: `claude` in terminal

**Note:** The app runs end-to-end and looks normal from the outside. The problems are intentionally subtle and realistic.

---

## Module 1 — Explore the App (15 min)

**Goal:** Participants discover problems themselves before involving Claude.

### Task
Open the app on `http://localhost:5173` and click through it. Note what stands out.

### Demo scenarios
1. **Delete a tag** still linked to todos (e.g. "work") → HTTP 500
2. **Double-click a checkbox** quickly → todo stays in wrong state
3. **Switch filters rapidly** (All → Done → Open) → wrong list briefly appears

### Discussion
- What did you find?
- What is a bug, what is a design problem?
- How would you approach this without AI tooling?

---

## Module 2 — Backend Refactoring (60–90 min)

**Recommendation:** Participants work with Claude Code themselves, trainer moderates and shows alternatives.

### Getting started with Claude Code
```bash
claude
```
Try: *"Look at the backend code and explain the biggest code quality issues."*

### Anti-patterns in recommended order

#### AP 1 — Constructor Injection (10 min)
**Where:** `TodoController`, `TagController`, `TodoService`, `DataSeeder`
**Problem:** `@Autowired` on fields — classes not testable without reflection, fields not `final`
**Fix:** Constructor injection, `@Autowired` is implicit since Spring 4.3

#### AP 2+3 — Fat Controller / Anemic Service (20 min)
**Where:** `TodoController.list()` — 70 lines of filter/sort/pagination logic in the controller
**Problem:** Controller contains business logic, service only passes through
**Fix:** `findFiltered(done, priority, tagId, page, size)` in `TodoService`

#### AP 6+7 — @Transactional + @ControllerAdvice (15 min)
**Where:** All controller methods — generic `try/catch` → always 500
**Problem:** Inconsistent DB operations without transactions, no meaningful HTTP status codes
**Fix:** `@Transactional` on service methods, `@RestControllerAdvice` with specific exception handlers

#### AP 4+5 — DTOs + N+1 (15 min)
**Where:** All endpoints return JPA entities directly, `getTags()` triggers N+1
**Problem:** API schema = DB schema, one extra SQL query per todo for tags
**Prove N+1:** Set `spring.jpa.show-sql=true` in `application.properties`, then observe `GET /api/todos`
**Fix:** `TodoResponse`/`TagResponse` records, `@EntityGraph(attributePaths = "tags")` in repository

#### AP 8+9+10+11 — Validation, Magic Strings, Logging, Secret (10 min)
Quick wins Claude can handle well on its own:
- Bean Validation with `@Valid` + `@NotBlank`
- `Priority.valueOf()` instead of `"HIGH".equals(prio)`
- SLF4J instead of `System.out.println`
- `${API_EXTERNAL_KEY:}` instead of plaintext secret

---

## Module 3 — Frontend Refactoring (60–90 min)

### Anti-patterns

#### AP 15+24 — any types + duplicate interfaces (10 min)
**Where:** `App.tsx` has a local `Todo` interface, `types.ts` exists but is ignored
**Show:** TypeScript is effectively disabled — no error on wrong property access
**Fix:** `import { Todo, Tag } from './types'`, delete local interfaces

#### AP 16+17 — Inline fetch + no error handling (15 min)
**Where:** `fetch()` directly in `App.tsx` and `TagManager.tsx`, no `res.ok` check
**Problem:** On backend 500, `.then()` still runs, UI shows nothing
**Fix:** `api.ts` with central functions, each checks `res.ok` and throws on error

#### AP 22 — Loading/Error States (10 min)
**Where:** List is simply empty while loading or on error
**Fix:** `loading` and `error` state, conditional rendering in JSX

#### AP 14+18 — Mega App.tsx + useState sprawl (20 min)
**Show:** `App.tsx` has 18+ `useState` calls, form state lives in the parent
**Fix:** Extract `Sidebar` component, `TodoForm` manages own state, group filter state into one object

#### AP 23 — Form Validation (10 min)
**Problem:** Submit with empty title → backend 400 → frontend ignores it
**Fix:** Client-side validation in `TodoForm`, inline error message

#### AP 25+21 — key={i} + unused prop (5 min)
Quick fixes that work well as a warm-up or filler

---

## Module 4 — Bug Hunt (45 min)

### Setup
Open DevTools → Network tab → throttle to **Slow 3G**

### Bug 1 — Toggle Race Condition
**Reproduce:** Double-click a checkbox quickly
**Observe:** Two PUTs in flight, second overwrites wrong state
**Fix:** Optimistic update — update state locally immediately, rollback on API error

### Bug 2 — Filter-Pagination Race
**Reproduce:** Switch filter rapidly between "All" → "Done" → "Open"
**Observe:** Older response arrives later and overwrites the newer list
**Fix:** `AbortController` in `useEffect` — cancel in-flight request on next render

### Bug 3 — dueDate Off-by-one
**Reproduce:** DevTools → Sensors → Timezone override to "America/Los_Angeles"
**Observe:** Todos with due dates show one day earlier
**Explanation:** `new Date("2026-05-15")` parses as UTC midnight → in UTC-7 that is May 14 local
**Fix:** Split the string directly, no `Date` object

### Bug 4 — Tag-Delete FK Constraint
**Reproduce:** Delete tag "work" (linked to todos) → HTTP 500
**Observe:** Backend log shows `JdbcSQLIntegrityConstraintViolationException`
**Fix:** Remove the tag from all todos before `deleteById`, all inside `@Transactional`

---

## Module 5 — Closing Discussion (15 min)

### Discussion questions
- Which anti-pattern would have stayed hidden longest without AI tooling?
- Where did Claude make a mistake or produce a suboptimal result?
- How does this change your review process?
- Where do you draw the line — what do you delegate to Claude, what do you keep yourself?

---

## Trainer Tips

**Pace:** Not all anti-patterns need to be covered. Module 2 + Bug 4 are the most important.

**When Claude makes a mistake:** Don't correct it immediately — show how to spot the error and guide Claude toward the fix. More realistic than perfect output.

**Reference solution:** Branch `workshop-reference` has all fixes — useful when participants get stuck.

**N+1 demo:** Enable `spring.jpa.show-sql=true` first, then count SQL queries live in the terminal — very impactful.

**Reset:** Restarting the backend resets the H2 database (DataSeeder runs again).
