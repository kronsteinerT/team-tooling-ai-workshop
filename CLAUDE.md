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
./node_modules/.bin/tsc --noEmit  # type check
```

## Architecture

The frontend proxies `/api/*` to the backend (configured in `vite.config.ts`). The backend uses an H2 in-memory database seeded with 5 tags and 15 todos on every startup via `DataSeeder.java`.

### Backend layers
- `controller/` — REST endpoints; all business logic, filtering, sorting and pagination lives here (known issue)
- `service/TodoService.java` — currently a pass-through to the repository with no logic
- `repository/` — Spring Data JPA, no custom queries
- `model/` — JPA entities (`Todo`, `Tag`, `Priority` enum); `Todo` owns the `@ManyToMany` join table

Dependencies are injected via `@Autowired` field injection throughout. Controllers return JPA entities directly — no DTOs. Error handling is done with generic `try/catch` in every method.

### Frontend structure
- `App.tsx` — all state, all fetch calls, all event handlers, sidebar JSX (~260 lines)
- `components/TodoList.tsx`, `TodoItem.tsx`, `TodoForm.tsx`, `TagManager.tsx`, `FilterBar.tsx`
- `types.ts` — `Todo` and `Tag` interfaces exist but are not imported anywhere; `any` is used throughout
- `fetch()` calls are made directly inside components with no error handling and no `res.ok` check

## H2 Console

Available at `http://localhost:8080/h2-console` — JDBC URL: `jdbc:h2:mem:tododb`, user: `sa`, no password.
