# Workshop: AI-gestütztes Code Refactoring

**Dauer:** ~3.5–4 Stunden  
**Zielgruppe:** Entwickler mit Java/Spring Boot Grundkenntnissen, React-Vorkenntnisse hilfreich aber nicht nötig  
**Ziel:** Zeigen wie Claude Code beim Aufdecken und Beheben von realistischen Code-Qualitätsproblemen hilft

---

## Vorbereitung (Trainer)

- Alle Teilnehmer auf Branch `workshop-start` auschecken lassen
- Java 21 und Node.js 18+ auf allen Rechnern sicherstellen
- Backend starten: `cd backend && ./gradlew bootRun`
- Frontend starten: `cd frontend && npm install && npm run dev`
- Claude Code installiert und eingeloggt: `claude` im Terminal

**Wichtig:** Die App läuft end-to-end und sieht von außen normal aus. Die Probleme sind absichtlich subtil und realistisch gehalten.

---

## Modul 1 — Aufwärmen: App kennenlernen (15 min)

**Ziel:** Teilnehmer sollen selbst Probleme entdecken, bevor Claude involviert wird.

### Aufgabe
Öffnet die App auf `http://localhost:5173` und klickt durch. Notiert was euch auffällt.

### Demo-Szenarien
1. **Tag löschen** der noch mit Todos verknüpft ist (z.B. "work") → HTTP 500
2. **Checkbox schnell doppelt klicken** → Todo bleibt im falschen State
3. **Filter schnell wechseln** (All → Done → Open) → falsche Liste erscheint kurz

### Diskussion
- Was habt ihr gefunden?
- Was ist ein Bug, was ist ein Design-Problem?
- Wie würdet ihr ohne KI-Tooling vorgehen?

---

## Modul 2 — Backend Refactoring (60–90 min)

**Empfehlung:** Teilnehmer arbeiten selbst mit Claude Code, Trainer moderiert und zeigt Alternativen.

### Einstieg mit Claude Code
```bash
claude
```
Dann z.B.: *"Schau dir den Backend-Code an und erkläre mir die größten Code-Qualitätsprobleme."*

### Anti-Patterns der Reihe nach (empfohlene Reihenfolge)

#### AP 1 — Constructor Injection (10 min)
**Wo:** `TodoController`, `TagController`, `TodoService`, `DataSeeder`  
**Problem:** `@Autowired` auf Feldern — Klassen nicht testbar ohne Reflection, Felder nicht `final`  
**Fix:** Konstruktor-Injection, `@Autowired` entfällt ab Spring 4.3

#### AP 2+3 — Fat Controller / Anämischer Service (20 min)
**Wo:** `TodoController.list()` — 70 Zeilen Filter/Sort/Pagination-Logik im Controller  
**Problem:** Controller hat Business-Logik, Service macht nur Pass-Through  
**Fix:** `findFiltered(done, priority, tagId, page, size)` in `TodoService`

#### AP 6+7 — @Transactional + @ControllerAdvice (15 min)
**Wo:** Alle Controller-Methoden — generisches `try/catch` → immer 500  
**Problem:** Inkonsistente DB-Operationen ohne Transaktion, keine sinnvollen HTTP Status Codes  
**Fix:** `@Transactional` auf Service-Methoden, `@RestControllerAdvice` mit spezifischen Exception-Handlern

#### AP 4+5 — DTOs + N+1 (15 min)
**Wo:** Alle Endpoints geben JPA-Entities direkt zurück, `getTags()` löst N+1 aus  
**Problem:** API-Schema = DB-Schema, pro Todo eine extra SQL-Query für Tags  
**Nachweis N+1:** `spring.jpa.show-sql=true` in `application.properties`, dann `GET /api/todos` beobachten  
**Fix:** `TodoResponse`/`TagResponse` Records, `@EntityGraph(attributePaths = "tags")` im Repository

#### AP 8+9+10+11 — Validation, Magic Strings, Logging, Secret (10 min)
Schnelle Wins die Claude gut selbstständig erledigen kann:
- Bean Validation mit `@Valid` + `@NotBlank`
- `Priority.valueOf()` statt `"HIGH".equals(prio)`
- SLF4J statt `System.out.println`
- `${API_EXTERNAL_KEY:}` statt Klartext-Secret

---

## Modul 3 — Frontend Refactoring (60–90 min)

### Anti-Patterns

#### AP 15+24 — any-Typen + doppelte Interfaces (10 min)
**Wo:** `App.tsx` hat lokales `Todo`-Interface, `types.ts` existiert aber wird ignoriert  
**Zeigen:** TypeScript ist effektiv ausgeschaltet — kein Fehler bei falschem Property-Zugriff  
**Fix:** `import { Todo, Tag } from './types'`, lokale Interfaces löschen

#### AP 16+17 — Inline fetch + kein Error-Handling (15 min)
**Wo:** `fetch()` direkt in `App.tsx` und `TagManager.tsx`, kein `res.ok`-Check  
**Problem:** Bei 500 vom Backend läuft `.then()` trotzdem durch, UI zeigt nichts  
**Fix:** `api.ts` mit zentralen Funktionen, jede prüft `res.ok` und wirft bei Fehler

#### AP 22 — Loading/Error States (10 min)
**Wo:** Liste ist beim Laden einfach leer, bei Fehler ebenfalls  
**Fix:** `loading` und `error` State, bedingte Anzeige im JSX

#### AP 14+18 — Mega App.tsx + useState-Wildwuchs (20 min)
**Zeigen:** `App.tsx` hat 18+ `useState`-Calls, Formular-State lebt im Parent  
**Fix:** `Sidebar`-Komponente extrahieren, `TodoForm` verwaltet eigenen State, Filter-State als Objekt gruppieren

#### AP 23 — Formular-Validierung (10 min)
**Problem:** Submit mit leerem Titel → Backend 400 → Frontend ignoriert das  
**Fix:** Client-side Validierung in `TodoForm`, Inline-Fehlermeldung

#### AP 25+21 — key={i} + ungenutztes Prop (5 min)
Schnelle Fixes die gut als Einstieg oder Pause-Füller taugen

---

## Modul 4 — Bug Hunt (45 min)

### Setup
DevTools öffnen → Network-Tab → Throttling auf **Slow 3G** stellen

### Bug 1 — Toggle Race Condition
**Reproduzieren:** Checkbox schnell zweimal klicken  
**Beobachten:** Zwei PUTs in Flight, zweiter überschreibt falschen State  
**Fix:** Optimistic Update — State lokal sofort ändern, Rollback bei API-Fehler

### Bug 2 — Filter-Pagination Race
**Reproduzieren:** Filter schnell zwischen "All" → "Done" → "Open" wechseln  
**Beobachten:** Ältere Response kommt später an, überschreibt neuere Liste  
**Fix:** `AbortController` im `useEffect` — laufenden Request beim nächsten Render abbrechen

### Bug 3 — dueDate Off-by-one
**Reproduzieren:** DevTools → Sensors → Timezone Override auf "America/Los_Angeles"  
**Beobachten:** Todos mit Due-Date zeigen einen Tag früher an  
**Erklärung:** `new Date("2026-05-15")` → UTC midnight → in UTC-7 ist das 14. Mai local  
**Fix:** String direkt splitten, kein `Date`-Objekt

### Bug 4 — Tag-Delete FK-Constraint
**Reproduzieren:** Tag "work" löschen (mit verknüpften Todos) → HTTP 500  
**Beobachten:** Backend-Log zeigt `JdbcSQLIntegrityConstraintViolationException`  
**Fix:** Vor `deleteById` alle Todos vom Tag trennen, alles in `@Transactional`

---

## Modul 5 — Abschlussdiskussion (15 min)

### Leitfragen
- Welches Anti-Pattern wäre euch ohne KI-Tooling am längsten verborgen geblieben?
- Wo hat Claude etwas falsch oder suboptimal gemacht?
- Wie ändert das euren Review-Prozess?
- Wo zieht ihr die Grenze — was delegiert ihr an Claude, was macht ihr selbst?

---

## Tipps für den Trainer

**Tempo:** Nicht alle Anti-Patterns müssen durchgearbeitet werden. Modul 2 + Bug 4 sind die wichtigsten.

**Wenn Claude einen Fehler macht:** Nicht sofort korrigieren — zeigen wie man den Fehler erkennt und Claude darauf hinweist. Das ist realistischer als perfekte Outputs.

**Referenz-Lösung:** Branch `feature/backend-refactoring` hat alle Fixes — hilfreich wenn Teilnehmer hängen.

**N+1 Demo:** Vorher `spring.jpa.show-sql=true` in `application.properties` einschalten, dann live im Terminal die SQL-Queries zählen — sehr eindrücklich.

**Reset:** Backend-Neustart setzt die H2-Datenbank zurück (DataSeeder läuft erneut).
