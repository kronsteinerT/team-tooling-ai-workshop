# Workshop Notes (Trainer-Only)

> This file is NOT for workshop participants. It lists all intentionally built-in anti-patterns and bugs, with hints for detection and fixes.

## Overview

The app is a todo management tool (15 seeded todos, 5 tags). Backend in Spring Boot, frontend in React/TypeScript. It works end-to-end but has intentional legacy problems — all realistic, nothing caricatured.

Includes:
- 13 backend anti-patterns
- 12 frontend anti-patterns
- 4 subtle bugs

---

## Backend Anti-Patterns

### 1. Field Injection instead of Constructor Injection
**Where:** `TodoController` (fields `todoService`, `todoRepository`, `tagRepository`), `TagController` (field `tagRepository`), `TodoService` (field `todoRepository`), `DataSeeder`
**How to spot:** `@Autowired` directly on fields, no constructors
**Fix:** Constructor injection with `final` fields, `@Autowired` is implicit since Spring 4.3. Makes classes testable without reflection and immutable.

### 2. Fat Controller (200+ lines)
**Where:** `TodoController` — filter logic, sort logic, pagination, validation, mapping, tag association all in the controller
**How to spot:** Methods `list()`, `create()`, `update()` contain business logic; `priorityRank()` as private helper in the controller
**Fix:** Move logic into `TodoService`, filter via Spring Data JPA Specifications or native query, sort via `Sort` parameter, pagination via `Pageable`/`Page<>`

### 3. Anemic Service (Pass-Through)
**Where:** `TodoService`
**How to spot:** Service only calls `repository.findAll()`, `save()`, `deleteById()`. No business logic.
**Fix:** Move filter/sort/validation logic from controller into service — then the service has a reason to exist.

### 4. JPA Entity directly as REST Response
**Where:** All endpoints in `TodoController` and `TagController` return `Todo`/`Tag` directly
**How to spot:** Response contains JPA-specific fields, prone to lazy-loading issues, couples API schema to DB schema
**Fix:** Introduce DTOs (`TodoResponse`, `TodoCreateRequest` etc.), mapping via MapStruct or manually

### 5. N+1 when loading Todos with Tags
**Where:** `TodoController.list()` — `todoService.findAll()` loads todos, serializing to JSON evaluates `getTags()` lazily — one SQL query per todo
**How to spot:** `spring.jpa.show-sql=true` in `application.properties` → console shows 1× SELECT FROM todo followed by N× SELECT FROM tag JOIN todo_tags for `GET /api/todos`
**Fix:** `@EntityGraph(attributePaths = "tags")` on custom repository method, or `@Query("SELECT t FROM Todo t LEFT JOIN FETCH t.tags")`

### 6. Missing `@Transactional`
**Where:** `TodoController.attachTag()`, `detachTag()`, and `update()` (multiple DB steps without a transaction). `TodoService` also has no `@Transactional`.
**How to spot:** Multiple DB operations in one controller method without `@Transactional`. DB is inconsistent if an error occurs mid-way.
**Fix:** `@Transactional` on service methods, service takes complete operations.

### 7. Generic Exception → 500, no @ControllerAdvice
**Where:** Every method in `TodoController`, `TagController` has `try { ... } catch (Exception e) { return ResponseEntity.status(500)... }`
**How to spot:** Generic 500s are always returned instead of meaningful status codes
**Fix:** `@ControllerAdvice` with `@ExceptionHandler` for `EntityNotFoundException`, `MethodArgumentNotValidException`, etc. Custom exceptions instead of generic catch-all.

### 8. Manual Validation instead of Bean Validation
**Where:** `TodoController.create()` and `update()`: `if (body.get("title") == null || body.get("title").toString().isBlank())`. `TagController.create()`: similar.
**How to spot:** `if`-validation in controller, no `@Valid`, no `jakarta.validation` import
**Fix:** `spring-boot-starter-validation` as dependency, DTOs with `@NotBlank`, `@Size`, `@Valid` on controller parameter, exception handler for `MethodArgumentNotValidException`

### 9. Magic Strings instead of Enum comparison
**Where:** `TodoController.list()` — `if ("HIGH".equals(priority))` instead of `Priority.HIGH`
**How to spot:** String literals in code instead of enum comparison. Typos cause no compile-time error.
**Fix:** `Priority.valueOf(priority)` with try/catch or as `@RequestParam Priority priority` (Spring converts automatically).

### 10. System.out.println instead of Logger
**Where:**
- `TodoController.list()` → "DEBUG: GET /api/todos ..."
- `TodoController.create()` → "DEBUG: created todo ..."
- `TagController.create()` → "DEBUG: created tag ..."
- `DataSeeder.run()` → "DEBUG: seeding database"
**How to spot:** No log levels, no MDC, no structured logging
**Fix:** SLF4J `Logger log = LoggerFactory.getLogger(...)` or `@Slf4j` (Lombok), use log levels

### 11. Plaintext Secret in Properties
**Where:** `backend/src/main/resources/application.properties` — `api.external.key=sk-dummy-1234567890abcdef`
**How to spot:** API key directly in source
**Fix:** External config via environment variable (`${API_EXTERNAL_KEY}`), Spring Cloud Config, Vault, or at minimum `.env` with `.gitignore`

### 12. Shallow Test
**Where:** `TodoControllerTest.java`
**How to spot:** Test only checks status code 200, no body assertions, no edge cases
**Fix:** Tests for filtering, validation (400 on blank title), 404 on non-existent ID, tag association, etc.

### 13. Flaky Test (time-dependent)
**Where:** `TodoFlakyTest.java`
**How to spot:** Test uses `LocalDateTime.now()` and `Thread.sleep(50)`. `before` and `saved.getCreatedAt()` can be equal with fine clock resolution or under load → `isAfter` fails.
**Fix:** Inject `Clock` abstraction, mock with `Clock.fixed(...)` in tests. Or add tolerance (`isBefore(after.plusSeconds(1))`).

---

## Frontend Anti-Patterns

### 14. Mega App.tsx (≈260 lines)
**Where:** `frontend/src/App.tsx` — state, all fetches, form handling, sidebar, stats loading all inline
**How to spot:** One component, many responsibilities. Hard to test, hard to reuse.
**Fix:** Split into `<Sidebar />`, `<Stats />`, `<TodoFormContainer />`. State in custom hooks (`useTodos`, `useFilters`) or context/store.

### 15. `any` Types
**Where:**
- `App.tsx`: `useState<any[]>([])` for `todos`, `handleEdit(todo: any)`, `handleToggleDone(todo: any)`
- `TodoList.tsx`: `todos: any[]`
- `TodoItem.tsx`: all props typed as `any`
**How to spot:** `: any` and `<any>` in many places — TypeScript is effectively disabled
**Fix:** Clean `Todo` type in `types.ts` (already exists but nobody imports it), reference everywhere

### 16. Direct fetch Calls in Components
**Where:** `App.tsx` (multiple `fetch('/api/...')`), `TagManager.tsx` (`fetch('/api/tags')`)
**How to spot:** URL, headers, JSON parsing all inline in JSX components
**Fix:** `api.ts` module with `getTodos()`, `createTodo()`, etc. Ideally custom hooks (`useTodos`) or React Query.

### 17. No Error Handling on API Calls
**Where:** All fetch calls in `App.tsx`, `TagManager.tsx` have no `.catch()`, no `response.ok` check
**How to spot:** When backend returns 500 (e.g. tag-delete bug), frontend swallows it and calls `.then()` with the error body
**Fix:** try/catch with async/await, `if (!response.ok) throw...`, UI state for errors with toasts/banners

### 18. useState Sprawl
**Where:** `App.tsx` has 18 separate `useState` calls
**How to spot:** Many related states kept separately (form state, filter state)
**Fix:** `useReducer` for complex state, or grouped objects (`useState<FormState>(...)`). Or a state library (Zustand, Jotai).

### 19. useEffect with Missing Dependencies
**Where:** `App.tsx` filter refetch effect — `// eslint-disable-next-line react-hooks/exhaustive-deps`. `pageSize` is used in the body but not in the deps list.
**How to spot:** `eslint-disable` comment — never accept without justification
**Fix:** Include all used values in deps or stabilize with `useCallback`/`useMemo`. For intent-based deps, prefer a custom hook.

### 20. Mixed Inline Styles and CSS
**Where:**
- `App.tsx`: `style={{ display: 'flex', minHeight: '100vh' }}`, inline styles for search input and stats
- `TodoItem.tsx`: `style={{ opacity: ... }}`, `style={{ textDecoration: ... }}`, `style={{ color: priorityColor }}`
- `TodoForm.tsx`: `style={{ marginRight: 8, ... }}`
**How to spot:** Mix of CSS classes and inline styles — style logic split across two places
**Fix:** Everything in CSS modules or styled-components, dynamic values via CSS vars or data-attributes

### 21. Prop Drilling
**Where:** `App.tsx` → `<TodoList selectedFilter={doneFilter} />` → `TodoList.tsx` → `<TodoItem selectedFilter={selectedFilter} />` → `TodoItem.tsx` doesn't use it (!)
**How to spot:** `selectedFilter` is passed through and never used at the end — classic "someone needed it once and never cleaned up"
**Fix:** Remove the prop (not needed at all). For real global state use Context API or a state library.

### 22. No Loading/Error States
**Where:** `App.tsx`, all components — no `isLoading`, `error` states
**How to spot:** User sees empty list while loading, nothing or broken UI on error
**Fix:** Loading skeletons, error banner. Custom hook (`useFetch`) returning `{ data, loading, error }`.

### 23. Form without Validation
**Where:** `TodoForm.tsx` and `App.tsx::handleSubmit` — no check for empty title before submit
**How to spot:** Submit with empty title sends a POST. Backend returns 400, frontend ignores it (see AP 17), modal stays open with no feedback.
**Fix:** Client-side validation with a form library (react-hook-form, formik) or manually. Disabled submit + inline errors.

### 24. Duplicate Type Definitions with Drift
**Where:**
- `frontend/src/types.ts` — canonical, but **imported by nobody**: `Todo { dueDate: string; description: string; tags: Tag[] }`
- `App.tsx` — local `interface Todo { description?: string; dueDate?: string; tags?: any[] }` (drifted)
- `TodoForm.tsx` — local `interface FormTodo { dueDate: Date | string; ... }` (different drift; unused!)
**How to spot:** 3 places with "Todo" type, slight differences, types.ts is dead code
**Fix:** types.ts as single source, import everywhere, delete local interfaces

### 25. Index as Key in Lists
**Where:**
- `TodoList.tsx`: `todos.map((todo, i) => <TodoItem key={i} ... />)`
- `TagManager.tsx`: `tags.map((tag, i) => <li key={i}>...)`
**How to spot:** `key={i}` instead of `key={item.id}`. On reorder/delete React may update the wrong component.
**Fix:** `key={todo.id}` and `key={tag.id}`

---

## Bugs

### Bug 1: Race Condition on Toggle Done
**Where:** `App.tsx::handleToggleDone` and `TodoController::update` (backend simulates 200ms delay)
**How to reproduce:** Click a todo's checkbox twice quickly (backend has `Thread.sleep(200)` in PUT for demo purposes).
**What happens:** First click reads `todo.done = false`, sends PUT with `done: true`. While in flight, user clicks again. Second click still reads `todo.done = false` (state not yet updated), sends another PUT with `done: true`. Expected after 2 clicks: `done = false`. Actual: stays `done = true`. If timing reverses (responses arrive out of order), state oscillates.
**Fix:**
1. Optimistic update: `setTodos(state with toggled)` first, then fetch — handler always reads current state
2. Use AbortController to cancel in-flight requests, or
3. Serialize mutations (e.g. via React Query mutation queue)

### Bug 2: Filter-Pagination Race
**Where:** `App.tsx::useEffect` with deps `[doneFilter, priorityFilter, selectedTagId, page]`. Backend has `Thread.sleep(150)` in GET to amplify it.
**How to reproduce:** Switch filter rapidly between "All" → "Done" → "Open" (or switch tags quickly in sidebar).
**What happens:** useEffect fires two requests in flight. If the older response arrives later, it overwrites the newer one. UI shows the wrong list that doesn't match the current filter.
**Fix:**
1. AbortController:
```ts
useEffect(() => {
  const ac = new AbortController();
  fetch(url, { signal: ac.signal }).then(...);
  return () => ac.abort();
}, [...]);
```
2. Or request ID/stale check: tag each request, verify on receive if still current.
3. Use React Query — handles this automatically.

### Bug 3: dueDate Off-by-one
**Where:**
- Backend: `Todo.dueDate` as `LocalDate`, serialized as ISO date string "2026-05-15"
- Frontend: `TodoItem.tsx` does `new Date(todo.dueDate).toLocaleDateString()`
**How to reproduce:**
1. Open DevTools → "Sensors" → Timezone override to "America/Los_Angeles"
2. Reload page
3. Todos with due dates now show one day earlier
**What happens:** `new Date("2026-05-15")` parses as UTC midnight → `2026-05-15T00:00:00Z`. In UTC-7 (Los Angeles) that is `2026-05-14T17:00 local` → `toLocaleDateString()` returns `5/14/2026`. Not visible in CET.
**Fix:**
1. Don't pass date strings through `Date`. Use `dueDate.split('-').reverse().join('.')` directly, or a library like `date-fns`/`dayjs`.
2. Or have the backend return the date as `{ year, month, day }` object → no timezone trap in frontend.

### Bug 4: Tag Orphan on Delete (visible crash)
**Where:** `TagController.delete()` — `tagRepository.deleteById(id)`. `Tag` has `@ManyToMany(mappedBy = "tags")` (non-owning), `Todo` is owning side with join table `todo_tags`. No cascade, no `@PreRemove` logic.
**How to reproduce:**
1. Open UI → "Manage tags"
2. Delete a tag still linked to todos (e.g. "work")
3. Frontend shows nothing, but DevTools Network → 500 with stack trace in backend log: `JdbcSQLIntegrityConstraintViolationException: Referential integrity constraint violation`
**What happens:** Hibernate sees no cascade config on the inverse side, deletes only the tag row, ignores the join table entries. H2 has FK constraint active → throws.
**Fix options:**
1. Service method `deleteTag(id)` with `@Transactional` that first removes the tag from all todos (or SQL `DELETE FROM todo_tags WHERE tag_id=?`), then deletes the tag.
2. Proper config: reconsider the ManyToMany — for a classic tagging setup an explicit join entity (`TodoTag`) with its own repository is cleaner.
3. Quick fix for demo purposes: in `TagController.delete()` first iterate all todos and remove the tag, then delete — works but is N+1 and not atomic.

---

## Session Flow Suggestions

### Warm-up (15 min)
- Have participants start the app and click through
- Question: "What stands out?" (note UX bugs — missing loading states, tag-delete crash, etc.)

### Backend Refactor (60–90 min)
- Introduce constructor injection (1)
- Build service layer (2 + 3)
- Introduce DTOs (4)
- Fix N+1 (5)
- @Transactional + @ControllerAdvice (6, 7)
- Bean Validation (8)

### Frontend Refactor (60–90 min)
- Extract API client (16)
- Loading/Error States (22, 17)
- Consolidate types (15, 24)
- Split components (14)

### Bug Hunt (45 min)
- Bugs 1, 2 with DevTools Network
- Bug 3 with timezone override
- Bug 4 — read stack trace, identify root cause

### Discussion (15 min)
- Which anti-pattern is most dangerous?
- Where would AI tooling help most (type inference, test generation, refactor suggestions)?

---

## Performance Demo Tips

- N+1 (#5): watch the console during `GET /api/todos`, count before/after
- Race conditions (Bug 1, 2): Browser DevTools Network → "Slow 3G" emulation amplifies them
- Bug 3: Timezone override in DevTools → Sensors

## Reset Notes

- DB is H2 in-memory — restarting the backend resets data via DataSeeder
- If frontend hangs: restart `npm run dev`

## Intentional Omissions

- Authentication
- Persistence across restarts
- Rate limiting
- Meaningful UI pagination (page size is fixed at 20)
- Realistic error boundaries

These gaps can provide material for a follow-up session.
