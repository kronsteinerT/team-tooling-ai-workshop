# Workshop Notes (Trainer-Only)

> Diese Datei ist NICHT für Workshop-Teilnehmer. Sie listet alle bewusst eingebauten Anti-Patterns und Bugs, mit Hinweisen zu Aufdeckung und Fix.

## Übersicht

Die App ist eine Todo-Verwaltung (15 seeded Todos, 5 Tags). Backend in Spring Boot, Frontend in React/TypeScript. Sie funktioniert end-to-end, hat aber bewusst Legacy-Probleme — alles realistisch, nichts karikaturhaft.

Inkludiert:
- 13 Backend-Anti-Patterns
- 12 Frontend-Anti-Patterns
- 4 subtile Bugs

---

## Backend Anti-Patterns

### 1. Field Injection statt Constructor Injection
**Wo**: `backend/src/main/java/com/example/todo/controller/TodoController.java` (Felder `todoService`, `todoRepository`, `tagRepository`), `controller/TagController.java` (Feld `tagRepository`), `service/TodoService.java` (Feld `todoRepository`), `config/DataSeeder.java`
**Erkennen**: `@Autowired` direkt auf Feldern, keine Konstruktoren
**Fix**: Konstruktor-Injection mit `final`-Feldern, `@Autowired` ist seit Spring 4.3 implizit. Macht Klassen testbar ohne Reflection und unveränderlich.

### 2. Fat Controller (200+ Zeilen)
**Wo**: `backend/src/main/java/com/example/todo/controller/TodoController.java` — Filter-Logik, Sort-Logic, Pagination, Validierung, Mapping, Tag-Verknüpfung alles im Controller
**Erkennen**: Methoden `list()`, `create()`, `update()` enthalten Business-Logik; `priorityRank()` als private Helper im Controller
**Fix**: Logik in `TodoService` ziehen, Filter via Spring Data JPA Specifications oder native Query, Sort via `Sort` Parameter, Pagination via `Pageable`/`Page<>`

### 3. Anämischer Service (Pass-Through)
**Wo**: `backend/src/main/java/com/example/todo/service/TodoService.java`
**Erkennen**: Service ruft nur `repository.findAll()`, `save()`, `deleteById()` durch. Keine Business-Logik.
**Fix**: Filter-/Sort-/Validation-Logik aus dem Controller in den Service ziehen, dann hat der Service Daseinsberechtigung.

### 4. JPA-Entity direkt als REST-Response
**Wo**: alle Endpoints in `TodoController` und `TagController`, gibt `Todo`/`Tag` direkt zurück
**Erkennen**: Response enthält JPA-spezifische Felder, ist anfällig für Lazy-Loading-Probleme, koppelt API-Schema an DB-Schema
**Fix**: DTOs einführen (`TodoResponse`, `TodoCreateRequest` etc.), Mapping via MapStruct oder manuell

### 5. N+1 beim Laden von Todos mit Tags
**Wo**: `TodoController.list()` — `todoService.findAll()` lädt Todos, beim JSON-Serialisieren wird `getTags()` lazy ausgewertet — pro Todo 1 SQL-Query
**Erkennen**: `application.properties` hat `spring.jpa.show-sql=true` → in der Konsole sieht man bei `GET /api/todos` 1× SELECT FROM todo gefolgt von N× SELECT FROM tag JOIN todo_tags ...
**Fix**: `@EntityGraph(attributePaths = "tags")` auf custom Repository-Methode, oder `@Query("SELECT t FROM Todo t LEFT JOIN FETCH t.tags")`

### 6. Fehlende `@Transactional`
**Wo**: `TodoController.attachTag()`, `detachTag()`, sowie `update()` (mehrere DB-Schritte ohne Transaktion). Auch `TodoService` hat keine `@Transactional`.
**Erkennen**: Mehrere DB-Operationen in einer Controller-Methode ohne `@Transactional`. Bei Fehler zwischen den Schritten ist DB inkonsistent.
**Fix**: `@Transactional` auf Service-Methoden, Service nimmt komplette Operationen entgegen.

### 7. Generic Exception → 500, kein @ControllerAdvice
**Wo**: jede Methode in `TodoController`, `TagController` hat `try { ... } catch (Exception e) { return ResponseEntity.status(500)... }`
**Erkennen**: Generische 500er werden immer zurückgegeben, statt sinnvoller Status Codes
**Fix**: `@ControllerAdvice` mit `@ExceptionHandler` für `EntityNotFoundException`, `MethodArgumentNotValidException`, etc. Custom Exceptions statt generic catch-all.

### 8. Manuelle Validierung statt Bean Validation
**Wo**: `TodoController.create()` und `update()`: `if (body.get("title") == null || body.get("title").toString().isBlank())`. `TagController.create()`: ähnlich.
**Erkennen**: `if`-Validierung im Controller, kein `@Valid`, kein `jakarta.validation` Import
**Fix**: `spring-boot-starter-validation` als Dependency, DTOs mit `@NotBlank`, `@Size`, `@Valid` am Controller-Parameter, Exception-Handler für `MethodArgumentNotValidException`

### 9. Magic Strings statt Enum-Vergleich
**Wo**: `TodoController.list()` — `if ("HIGH".equals(priority))` statt `Priority.HIGH`
**Erkennen**: String-Literals im Code statt Enum-Vergleich. Bei Tippfehler keine Compile-Zeit-Hilfe.
**Fix**: `Priority.valueOf(priority)` mit try/catch oder als `@RequestParam Priority priority` (Spring konvertiert automatisch).

### 10. System.out.println statt Logger
**Wo**: 
- `TodoController.list()` → "DEBUG: GET /api/todos ..."
- `TodoController.create()` → "DEBUG: created todo ..."
- `TagController.create()` → "DEBUG: created tag ..."
- `DataSeeder.run()` → "DEBUG: seeding database"
**Erkennen**: keine Log-Levels, keine MDC, kein strukturiertes Logging
**Fix**: SLF4J `Logger log = LoggerFactory.getLogger(...)` oder `@Slf4j` (Lombok), Levels nutzen

### 11. Klartext-Secret in Properties
**Wo**: `backend/src/main/resources/application.properties` — `api.external.key=sk-dummy-1234567890abcdef`
**Erkennen**: API-Key direkt im Source
**Fix**: Externe Konfiguration via Environment-Variable (`${API_EXTERNAL_KEY}`), Spring Cloud Config, Vault, oder zumindest .env mit .gitignore

### 12. Oberflächlicher Test
**Wo**: `backend/src/test/java/com/example/todo/TodoControllerTest.java`
**Erkennen**: Test prüft nur Status-Code 200, keine Body-Assertions, keine Edge Cases
**Fix**: Tests für Filter, Validierung (400 bei leerem title), 404 bei nicht-existenter ID, Tag-Verknüpfung, etc.

### 13. Flaky Test (zeitabhängig)
**Wo**: `backend/src/test/java/com/example/todo/TodoFlakyTest.java`
**Erkennen**: Test nutzt `LocalDateTime.now()` und `Thread.sleep(50)`. `before` und `saved.getCreatedAt()` können bei feiner Clock-Resolution oder unter Last gleich sein → `isAfter` schlägt fehl.
**Fix**: `Clock`-Abstraktion injizieren, in Tests `Clock.fixed(...)` mocken. Oder Toleranzen einbauen (`isBefore(after.plusSeconds(1))`).

---

## Frontend Anti-Patterns

### 14. Mega App.tsx (≈260 Zeilen)
**Wo**: `frontend/src/App.tsx` — State, alle Fetches, Form-Handling, Sidebar, Stats-Loading inline
**Erkennen**: Eine Komponente, viele Verantwortlichkeiten. Schwer testbar, schwer wiederverwendbar.
**Fix**: Auflösen in `<Sidebar />`, `<Stats />`, `<TodoFormContainer />`. State in Custom Hooks (`useTodos`, `useFilters`) oder Context/Store.

### 15. `any`-Typen
**Wo**: 
- `App.tsx`: `useState<any[]>([])` für `todos`, `editingTodo: Todo | null` aber `handleEdit(todo: any)`, `handleToggleDone(todo: any)`, `(t: any) => t.id`
- `TodoList.tsx`: `todos: any[]`
- `TodoItem.tsx`: alle Props mit `any`
**Erkennen**: `: any` und `<any>` an vielen Stellen — TypeScript ist effektiv ausgeschaltet
**Fix**: Sauberer `Todo`-Typ in `types.ts` (existiert schon, wird aber von niemandem importiert), überall referenzieren

### 16. Direkte fetch-Calls in Komponenten
**Wo**: `App.tsx` (mehrere `fetch('/api/...')`), `TagManager.tsx` (`fetch('/api/tags')`)
**Erkennen**: URL, Headers, JSON-Parsing alles inline in JSX-Komponenten
**Fix**: `api.ts`-Modul mit `getTodos()`, `createTodo()`, etc. Idealerweise Custom Hooks (`useTodos`) oder React Query.

### 17. Kein Error-Handling bei API-Calls
**Wo**: alle fetch-Aufrufe in `App.tsx`, `TagManager.tsx` haben kein `.catch()`, kein Check auf `response.ok`
**Erkennen**: Wenn Backend 500 zurückgibt (z.B. bei Tag-Delete-Bug), schluckt der Frontend-Code das und ruft `.then()` mit dem Error-Body weiter
**Fix**: try/catch mit async/await, `if (!response.ok) throw...`, UI-State für Errors mit Toasts/Banners

### 18. useState-Wildwuchs
**Wo**: `App.tsx` hat 18 separate `useState`-Calls (todos, tags, selectedTagId, doneFilter, priorityFilter, page, pageSize, showForm, editingTodo, title, description, priority, dueDate, selectedFormTagIds, showTagManager, searchTerm, totalCount, doneCount)
**Erkennen**: viele zusammenhängende States werden separat gehalten (Form-State, Filter-State)
**Fix**: `useReducer` für komplexen State, oder zusammengefasste Objekte (`useState<FormState>(...)`). Evtl. State-Library (Zustand, Jotai).

### 19. useEffect mit fehlenden Dependencies
**Wo**: `App.tsx` Filter-Refetch-Effect — `// eslint-disable-next-line react-hooks/exhaustive-deps`. `pageSize` ist im Body benutzt aber nicht in der Deps-Liste.
**Erkennen**: `eslint-disable` Kommentar — niemals akzeptieren ohne Begründung
**Fix**: Alle benutzten Werte in Deps oder per `useCallback`/`useMemo` stabilisieren. Bei intent-statt-deps lieber Custom Hook.

### 20. Inline-Styles + CSS gemischt
**Wo**: 
- `App.tsx`: `style={{ display: 'flex', minHeight: '100vh' }}`, mehrfach inline für Search-Input, Stats
- `TodoItem.tsx`: `style={{ opacity: ... }}`, `style={{ textDecoration: ... }}`, `style={{ color: priorityColor }}`
- `TodoForm.tsx`: `style={{ marginRight: 8, ... }}`
**Erkennen**: Mix aus CSS-Klassen und inline-Styles — Stil-Logik verteilt auf 2 Orte
**Fix**: Alles in CSS Modules oder Styled-Components, dynamische Werte via CSS-Vars oder data-attributes

### 21. Prop-Drilling
**Wo**: `App.tsx` → `<TodoList selectedFilter={doneFilter} />` → `TodoList.tsx` → `<TodoItem selectedFilter={selectedFilter} />` → `TodoItem.tsx` benutzt es nicht (!)
**Erkennen**: `selectedFilter` wird durchgereicht und am Ende nicht verwendet — typisches "irgendjemand hat's mal gebraucht und nicht aufgeräumt"
**Fix**: Prop entfernen (gar nicht benötigt). Bei echtem globalen State Context API oder State-Library nutzen.

### 22. Keine Loading-/Error-States
**Wo**: `App.tsx`, alle Components — keine `isLoading`, `error` States
**Erkennen**: User sieht beim Laden leere Liste, beim Fehler nichts oder kaputte UI
**Fix**: Loading-Skeletons, Error-Banner. Custom Hook (`useFetch`) der `{ data, loading, error }` zurückgibt.

### 23. Formular ohne Validierung
**Wo**: `TodoForm.tsx` und `App.tsx::handleSubmit` — kein Check auf leeren Title vor Submit
**Erkennen**: Submit mit leerem Title schickt POST. Backend antwortet 400, Frontend ignoriert das (siehe Anti-Pattern 17), Modal bleibt offen ohne Feedback.
**Fix**: Client-side Validierung mit Form-Library (react-hook-form, formik) oder manuell. Disabled-Submit + Inline-Errors.

### 24. Doppelte Type-Definitionen mit Drift
**Wo**: 
- `frontend/src/types.ts` — kanonisch, aber von **niemandem importiert**: `Todo { dueDate: string; description: string; tags: Tag[] }`
- `App.tsx` — lokales `interface Todo { description?: string; dueDate?: string; tags?: any[] }` (drift)
- `TodoForm.tsx` — lokales `interface FormTodo { dueDate: Date | string; ... }` (anderer drift; ungenutzt!)
**Erkennen**: 3 Stellen mit "Todo"-Typ, leichte Unterschiede, types.ts ist toter Code
**Fix**: types.ts als Single Source, überall importieren, lokale Interfaces löschen

### 25. Index als Key in Listen
**Wo**: 
- `TodoList.tsx`: `todos.map((todo, i) => <TodoItem key={i} ... />)`
- `TagManager.tsx`: `tags.map((tag, i) => <li key={i}>...)`
**Erkennen**: `key={i}` statt `key={item.id}`. Bei Reorder/Delete kann React Komponenten falsch updaten.
**Fix**: `key={todo.id}` und `key={tag.id}`

---

## Bugs

### Bug 1: Race Condition beim "Toggle Done"
**Wo**: `App.tsx::handleToggleDone` und `TodoController::update` (Backend simuliert 200ms Delay)
**Wie reproduzieren**: Schnell zweimal hintereinander auf die Checkbox eines Todos klicken (Backend hat `Thread.sleep(200)` im PUT für Demo-Zweck).
**Was passiert**: Beim ersten Klick liest Handler `todo.done = false`, schickt PUT mit `done: true`. Während das in Flight ist, klickt User zweimal. Der zweite Klick liest immer noch `todo.done = false` (State noch nicht aktualisiert), schickt nochmal PUT mit `done: true`. Erwartet wäre nach 2 Klicks wieder `done = false`. Stattdessen: bleibt `done = true`. Wenn das Timing andersrum ist (Antworten kommen umgedreht an), wird der State zwischen true und false oszillieren.
**Fix**: 
1. Optimistic Update: erst `setTodos(state mit toggled)`, dann fetch — handler liest immer den aktuellen State
2. AbortController nutzen um in-flight Requests zu canceln, oder
3. Mutationen serialisieren (z.B. via React Query mit Mutations-Queue)

### Bug 2: Filter-Pagination Race
**Wo**: `App.tsx::useEffect` mit deps `[doneFilter, priorityFilter, selectedTagId, page]`. Backend hat `Thread.sleep(150)` im GET zur Verstärkung.
**Wie reproduzieren**: Filter schnell zwischen "All" → "Done" → "Open" wechseln (oder Tag in Sidebar schnell wechseln).
**Was passiert**: useEffect feuert zwei Requests in Flight. Wenn die ältere Response später ankommt, überschreibt sie die neuere. UI zeigt falsche Liste, die nicht zum aktuellen Filter passt.
**Fix**: 
1. AbortController:
```ts
useEffect(() => {
  const ac = new AbortController();
  fetch(url, { signal: ac.signal }).then(...);
  return () => ac.abort();
}, [...]);
```
2. Oder Request-ID/Stale-Check: jede Anfrage taggen, beim Receive prüfen ob noch aktuell.
3. React Query nutzen — managed das automatisch.

### Bug 3: dueDate Off-by-one
**Wo**: 
- Backend: `Todo.dueDate` als `LocalDate`, serialisiert als ISO-Date-String "2026-05-15"
- Frontend: `TodoItem.tsx` macht `new Date(todo.dueDate).toLocaleDateString()`
**Wie reproduzieren**: 
1. DevTools öffnen → "Sensors" → Timezone Override auf "America/Los_Angeles"
2. Page reload
3. Todos mit due-Date werden jetzt einen Tag früher angezeigt
**Was passiert**: `new Date("2026-05-15")` parsed als UTC midnight → `2026-05-15T00:00:00Z`. In TZ UTC-7 (Los Angeles) ist das `2026-05-14T17:00 local` → `toLocaleDateString()` gibt `5/14/2026`. In CET sieht man's nicht.
**Fix**: 
1. Tag-Strings nicht durch `Date` jagen. Direkt `dueDate.split('-').reverse().join('.')` oder mit Library wie `date-fns`/`dayjs`.
2. Oder Backend gibt Datum als `{ year, month, day }` Objekt → Frontend hat keine TZ-Falle.

### Bug 4: Tag-Orphan beim Delete (Variante 1: sichtbarer Crash)
**Wo**: `TagController.delete()` — `tagRepository.deleteById(id)`. `Tag` hat `@ManyToMany(mappedBy = "tags")` (non-owning), `Todo` ist owning side mit Join-Tabelle `todo_tags`. Kein cascade, keine `@PreRemove` Logik.
**Wie reproduzieren**: 
1. UI öffnen → "Manage tags"
2. Tag löschen, der noch mit Todos verknüpft ist (z.B. "work")
3. Frontend zeigt nichts, aber DevTools-Network → 500 mit Stacktrace im Backend-Log: `org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException: Referential integrity constraint violation`
**Was passiert**: Hibernate sieht keine Cascade-Konfiguration auf der inversen Seite, löscht nur die Tag-Row, ignoriert die Join-Table-Einträge. H2 hat FK-Constraint aktiv → wirft.
**Fix-Optionen**: 
1. Service-Methode `deleteTag(id)` mit `@Transactional`, die zuerst alle `Todo`s lädt, die diesen Tag haben (bzw. SQL `DELETE FROM todo_tags WHERE tag_id=?`), dann Tag löscht.
2. Saubere Konfiguration: ManyToMany sauber überdenken — bei klassischem Tagging-Setup besser eine explizite Join-Entity (`TodoTag`) mit eigener Repository.
3. Schneller Fix für Demo-Zwecke: in `TagController.delete()` zuerst `for (Todo t : todoRepo.findAll()) { t.getTags().removeIf(tag -> tag.getId().equals(id)); todoRepo.save(t); }` — funktioniert, aber ist N+1 und nicht atomic. Besserer Fix: Custom Query `@Modifying @Query("DELETE FROM Todo t SET ... ")` o.ä.

---

## Workshop-Ablauf-Vorschläge

### Aufwärm-Runde (15 min)
- Teilnehmer App starten lassen, durchklicken
- Frage: "Was fällt euch auf?" (User-Experience-Bugs notieren — fehlende Loading-States, Crash bei Tag-Delete, etc.)

### Backend-Refactor (60-90 min)
- Constructor Injection einführen (1)
- Service-Layer aufbauen (2 + 3)
- DTOs einführen (4)
- N+1 fixen (5)
- @Transactional + @ControllerAdvice (6, 7)
- Bean Validation (8)

### Frontend-Refactor (60-90 min)
- API-Client extrahieren (16)
- Loading/Error States (22, 17)
- Types konsolidieren (15, 24)
- Komponenten aufteilen (14)

### Bug-Hunt (45 min)
- Bug 1, 2 mit DevTools Network reproduzieren
- Bug 3 mit TZ-Override demonstrieren
- Bug 4 — Stack Trace lesen, Root Cause identifizieren

### Diskussion (15 min)
- Welches Anti-Pattern ist am gefährlichsten?
- Wo würde KI-Tooling besonders helfen (Type-Inferenz, Test-Generation, Refactor-Vorschläge)?

---

## Performance-Demo-Tipps

- N+1 (#5): bei `GET /api/todos` in der Console mitlesen, vorher/nachher zählen
- Race Conditions (Bug 1, 2): Browser DevTools Network → "Slow 3G" emulation verstärkt es
- Bug 3: TZ-Override in DevTools → Sensors

## Hinweise zum Reset

- DB ist H2 in-memory — Backend-Restart resettet Daten via DataSeeder
- Falls Frontend mal hängt: `npm run dev` neustarten

## Was die App noch nicht hat (bewusst weggelassen)

- Authentifizierung
- Persistenz über Restart hinaus
- Rate-Limiting
- Pagination im UI sinnvoll (Page-Größe ist auf 20 fix)
- Realistische Error-Boundaries

Diese Lücken können in einer Folgesession Material liefern.
