# Todo App

Ein einfaches Todo-Tool mit Spring Boot Backend und React/TypeScript Frontend.

## Stack

- **Backend**: Spring Boot 3.3, Java 21, Gradle, Spring Data JPA, H2 (in-memory)
- **Frontend**: React 18, TypeScript, Vite

## Voraussetzungen

- Java 21
- Gradle 8.5+ (oder einmalig `gradle wrapper` ausführen, danach `./gradlew` benutzen)
- Node.js 18+
- npm

## Backend starten

```bash
cd backend
gradle bootRun
# oder mit Wrapper, falls einmalig erzeugt (gradle wrapper):
./gradlew bootRun
```

Das Backend läuft auf http://localhost:8080.

Beim Start werden 5 Tags und 15 Beispiel-Todos in die H2-In-Memory-DB geseedet.

H2-Konsole: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:tododb`, User `sa`, kein Passwort).

## Frontend starten

```bash
cd frontend
npm install
npm run dev
```

Das Frontend läuft auf http://localhost:5173 und proxied `/api/*` an das Backend.

## REST-Endpoints

### Todos
- `GET /api/todos?done=&priority=&tagId=&page=&size=` — Liste mit Filter und Pagination
- `GET /api/todos/{id}`
- `POST /api/todos` — Body: `{ title, description, priority, dueDate, tagIds }`
- `PUT /api/todos/{id}`
- `DELETE /api/todos/{id}`
- `POST /api/todos/{id}/tags/{tagId}` — Tag verknüpfen
- `DELETE /api/todos/{id}/tags/{tagId}` — Tag entfernen

### Tags
- `GET /api/tags`
- `GET /api/tags/{id}`
- `POST /api/tags` — Body: `{ name }`
- `PUT /api/tags/{id}`
- `DELETE /api/tags/{id}`

## Tests

```bash
cd backend
gradle test
```

## Projektstruktur

```
.
├── backend/                  # Spring Boot Anwendung
│   ├── build.gradle
│   ├── settings.gradle
│   └── src/
│       ├── main/java/com/example/todo/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── model/
│       │   └── config/
│       └── main/resources/application.properties
└── frontend/                 # React + Vite Anwendung
    ├── package.json
    └── src/
        ├── App.tsx
        ├── components/
        └── types.ts
```
