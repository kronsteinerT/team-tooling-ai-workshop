# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

- **Backend**: Spring Boot 3.3, Java 21, Gradle, Spring Data JPA, H2 (in-memory)
- **Frontend**: React 18, TypeScript, Vite

## Commands

### Backend
```bash
cd backend
./gradlew bootRun        # start on http://localhost:8080
./gradlew test           # run all tests
./gradlew test --tests "com.dynatrace.teamtooling.aiworkshop.TodoControllerTest" # single test class
```

### Frontend
```bash
cd frontend
npm install
npm run dev              # start on http://localhost:5173
npx tsc --noEmit         # type check (use ./node_modules/.bin/tsc if npx resolves wrong version)
```

## Architecture

The frontend proxies `/api/*` to the backend (configured in `vite.config.ts`). The backend uses an H2 in-memory database that is seeded with 5 tags and 15 todos on every startup via `DataSeeder.java`.

### Backend layers
- `controller/` — REST endpoints, request/response mapping via DTOs (`dto/`), no business logic
- `service/TodoService.java` — filtering, sorting, pagination logic; all methods `@Transactional`
- `repository/` — Spring Data JPA; `TodoRepository` has `@EntityGraph` methods to avoid N+1 on `tags`
- `model/` — JPA entities (`Todo`, `Tag`, `Priority` enum); `Todo` owns the `@ManyToMany` join table
- `controller/GlobalExceptionHandler.java` — `@RestControllerAdvice` handling validation errors (400), not-found (404), and generic errors (500)

DTOs (`TodoResponse`, `TagResponse`, `TodoCreateRequest`, `TodoUpdateRequest`) decouple the API schema from JPA entities. Controllers return DTOs, never entities directly.

### Frontend structure
- `api.ts` — all `fetch` calls in one place; every function checks `res.ok` and throws on error
- `types.ts` — single source of truth for `Todo` and `Tag` interfaces; import from here, never redefine locally
- `App.tsx` — top-level state: `todos`, `tags`, `filters` (grouped object), `loading`/`error`, `showForm`/`editingTodo`
- `components/Sidebar.tsx` — tag navigation, stats, tag manager toggle (owns `showTagManager` state)
- `components/TodoForm.tsx` — self-contained form with own state; receives `initialValues` for edit mode, calls `onSubmit(data)` on save

Filter state in `App.tsx` is a single object `{ done, priority, tagId, page }` — update via `setFilters(f => ({ ...f, key: value }))`. The `useEffect` watching `filters` uses an `AbortController` to cancel in-flight requests on rapid filter changes.

## H2 Console

Available at `http://localhost:8080/h2-console` — JDBC URL: `jdbc:h2:mem:tododb`, user: `sa`, no password.

## Logs

Application logs are written to `todo.log` in the project root (configured in `backend/src/main/resources/logback-spring.xml`).
