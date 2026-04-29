# Todo App

A simple todo management app with a Spring Boot backend and React/TypeScript frontend.

## Stack

- **Backend**: Spring Boot 3.3, Java 21, Gradle, Spring Data JPA, H2 (in-memory)
- **Frontend**: React 18, TypeScript, Vite

## Prerequisites

- Java 21
- Node.js 18+
- npm

## Start Backend

```bash
cd backend
./gradlew bootRun
```

Backend runs on http://localhost:8080.

On startup, 5 tags and 15 sample todos are seeded into the H2 in-memory database.

H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:tododb`, user `sa`, no password).

## Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:5173 and proxies `/api/*` to the backend.

## REST Endpoints

### Todos
- `GET /api/todos?done=&priority=&tagId=&page=&size=` — list with filter and pagination
- `GET /api/todos/{id}`
- `POST /api/todos` — body: `{ title, description, priority, dueDate, tagIds }`
- `PUT /api/todos/{id}`
- `DELETE /api/todos/{id}`
- `POST /api/todos/{id}/tags/{tagId}` — attach tag
- `DELETE /api/todos/{id}/tags/{tagId}` — detach tag

### Tags
- `GET /api/tags`
- `GET /api/tags/{id}`
- `POST /api/tags` — body: `{ name }`
- `PUT /api/tags/{id}`
- `DELETE /api/tags/{id}`

## Tests

```bash
cd backend
./gradlew test
```
