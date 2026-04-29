# Workshop: AI-gestütztes Code Refactoring

## Setup

**Voraussetzungen:** Java 21, Node.js 18+, Claude Code (`claude` im Terminal)

```bash
# Backend starten
cd backend
./gradlew bootRun

# Frontend starten (neues Terminal)
cd frontend
npm install
npm run dev
```

App öffnen: `http://localhost:5173`

---

## Modul 1 — App kennenlernen (15 min)

Klick durch die App und notiere was dir auffällt. Probiere folgende Szenarien:

1. Einen **Tag löschen**, der noch mit Todos verknüpft ist
2. Eine **Checkbox schnell zweimal klicken**
3. **Filter schnell wechseln** (All → Done → Open)

Was sind Bugs? Was sind Design-Probleme?

---

## Modul 2 — Backend Refactoring

Starte Claude Code und lass dir die Code-Qualitätsprobleme erklären:
```bash
claude
```

### Bereiche zum Untersuchen

**Dependency Injection**
- Wie werden Dependencies in `TodoController`, `TagController`, `TodoService` und `DataSeeder` injiziert?
- *Hint: Es gibt einen moderneren Weg der Felder `final` macht und Klassen testbarer.*

**Controller vs. Service**
- Wie viel Logik steckt in `TodoController.list()`?
- *Hint: Ein Controller sollte nur HTTP-Mapping machen — keine Business-Logik.*

**Fehlerbehandlung**
- Was passiert bei einem Fehler in einem Endpoint?
- *Hint: Spring hat einen zentralen Mechanismus für Exception-Handling.*

**Datenbankzugriffe**
- Aktiviere `spring.jpa.show-sql=true` in `application.properties`. Wie viele SQL-Queries erzeugt `GET /api/todos`?
- *Hint: Es sollte eine Query sein, nicht N+1.*

**API-Design**
- Was gibt der Controller direkt zurück? Welches Problem entsteht dadurch?
- *Hint: Stichwort DTO — Data Transfer Object.*

**Weitere Punkte zum Entdecken**
- Logging: `System.out.println` vs. richtiges Logging-Framework
- Validierung: Wie wird der Request-Body validiert?
- Secrets: Schau in `application.properties`

---

## Modul 3 — Frontend Refactoring

### Bereiche zum Untersuchen

**TypeScript**
- Schau in `App.tsx` — welchen Typ haben `todos` und die Handler-Parameter?
- Schau in `types.ts` — wird diese Datei irgendwo importiert?
- *Hint: `any` schaltet TypeScript effektiv aus.*

**API-Calls**
- Wo werden `fetch()`-Aufrufe gemacht? Was passiert wenn der Server 500 zurückgibt?
- *Hint: `fetch()` wirft keinen Fehler bei HTTP-Fehlercodes — `res.ok` muss manuell geprüft werden.*

**Komponenten-Struktur**
- Zähle die `useState`-Calls in `App.tsx`. Welche gehören zusammen?
- Welche Props werden durch mehrere Ebenen durchgereicht ohne genutzt zu werden?

**Listen-Rendering**
- Welchen `key`-Prop benutzt `TodoList`? Warum ist das problematisch?

**Weitere Punkte zum Entdecken**
- Loading- und Error-States: Was sieht der User beim Laden oder bei einem Fehler?
- Formular-Validierung: Was passiert beim Submit mit leerem Titel?
- Inline-Styles vs. CSS-Klassen

---

## Modul 4 — Bug Hunt

Nutze DevTools → Network → **Slow 3G** um die Bugs besser sichtbar zu machen.

| Bug | Wie reproduzieren |
|-----|-------------------|
| Toggle Race Condition | Checkbox schnell zweimal klicken |
| Filter Race Condition | Filter schnell wechseln |
| Datum Off-by-one | DevTools → Sensors → Timezone auf "America/Los_Angeles" |
| Tag-Delete Crash | Tag löschen der mit Todos verknüpft ist → Fehlermeldung im Backend-Log lesen |

*Hint für Race Conditions: Schau dir an wie React State-Updates und asynchrone Requests zusammenspielen.*

---

## Referenz

Vollständige Lösung: Branch `workshop-reference`
