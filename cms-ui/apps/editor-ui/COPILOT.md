# Content Copilot — Aktionen erweitern

Diese Anleitung beschreibt, wie man eine **funktionsfähige Aktion** zum Content Copilot
hinzufügt — von der Backend-Seite bis zum Eintrag in der `copilot.json`, die der Editor-UI
liest.

> **Vorab:** das aktuelle UI-Scaffolding (`f-content-copilot-ui`-Branch) bringt den Toolbar-Button,
> die Sidebar und den Konfigurations-Loader mit. Action-Karten werden gerendert, wenn die JSON
> Aktionen enthält — sie sind aber **noch nicht klickbar mit Backend-Anbindung**. Schritt 3
> dieser Anleitung beschreibt diese einmalige Verdrahtung; Schritte 1, 2, 4 wiederholen sich pro
> Aktion.

---

## Überblick: Wer macht was

```
   ┌─────────────────────────────────────────────────────────────┐
   │ Browser (Editor-UI)                                         │
   │   • Klick auf Action-Karte                                  │
   │   • POST /rest/copilot/execute  (CMS-Session genügt)        │
   └─────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ CMS-Backend (Proxy)                                         │
   │   • prüft Session                                           │
   │   • hängt X-Copilot-Api-Key dran                            │
   │   • forwardet an Copilot-Backend                            │
   └─────────────────────────────┬───────────────────────────────┘
                                 │
                                 ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ Copilot-Backend (Spring Boot, eigener Service)              │
   │   POST /api/copilot/v1/execute                              │
   │   • validiert Aktion gegen Allowlist                        │
   │   • baut Prompt aus Draft-Kontext                           │
   │   • ruft LLM auf, validiert Ergebnis                        │
   │   • antwortet mit Suggestion(s)                             │
   └─────────────────────────────────────────────────────────────┘
```

| Schicht | Verantwortlich für | Ändert sich pro Aktion? |
|---|---|---|
| **Copilot-Backend** | Action-Logik, Prompt-Template, Allowlist | Ja, falls **neue** Aktion |
| **CMS-Backend (Proxy)** | Auth durchreichen, API-Key anhängen | Nein (einmalige Einrichtung) |
| **Editor-UI** | Klick-Handler, Result-Anzeige | Nein (einmalige Verdrahtung) |
| **`copilot.json`** | Welche Aktion mit welchem Label angezeigt wird | Ja |

Die wirklich wiederkehrende Arbeit pro Aktion ist also **JSON-Eintrag** (+ ggf. neuer Backend-Code).

---

## Schritt 1: Backend-Aktion verfügbar machen

### 1a) Bestehende Aktion nutzen

Im MVP des Copilot-Backends sind diese Aktionen schon eingebaut:

| API-Name | Was sie tut | Typischer Ziel-Feldname |
|---|---|---|
| `generate_teaser` | Kurzen Teaser aus Body/Title erzeugen | `teaser` |
| `generate_summary` | Redaktionelle Zusammenfassung | (frei wählbar) |
| `generate_seo_description` | SEO-Meta-Description | `seoDescription` |
| `find_similar_content` | Semantisch ähnliche Inhalte finden | (kein Schreibtarget — Liste) |
| `quality_check` | Editorial-QA mit Issue-Liste | (kein Schreibtarget — Issues) |

Für diese Aktionen reicht ein YAML-Eintrag (Schritt 4). Kein Backend-Code nötig.

### 1b) Neue Aktion hinzufügen

Wenn die fünf Standard-Aktionen nicht reichen, im Backend:

1. **Enum erweitern** — `gentics-cmp-content-copilot-backend/src/main/java/com/gentics/copilot/domain/action/CopilotAction.java`:

   ```java
   /** Schreibt den Body in einer angegebenen Tonalität um. */
   REWRITE_TONE("rewrite_tone"),
   ```

2. **Prompt-Templates** — `infrastructure/prompt/PromptTemplateService.java`:
   in `buildSystemPrompt(...)` und `buildUserMessage(...)` jeweils den neuen `case` ergänzen
   und eine kleine Helper-Methode mit System-Prompt + User-Message bauen. Bei den bestehenden
   Aktionen kannst du den Stil 1:1 abkupfern.

3. **Allowlist** — in `src/main/resources/application.yml` (oder via Env `copilot.allowed-actions`)
   den neuen API-Namen ergänzen:

   ```yaml
   copilot:
     allowed-actions:
       - generate_teaser
       - generate_summary
       - generate_seo_description
       - find_similar_content
       - quality_check
       - rewrite_tone        # ← neu
   ```

4. **Optional: Template-Profil** — wenn die Aktion auf einem bestimmten CMS-Template
   andere Felder als Source/Target nutzen soll, den Eintrag in
   `copilot-template-profiles.yml` anpassen (siehe `copilot-template-profiles.example.yml`
   im Backend-Repo).

5. **Build & Deploy** — Backend neu starten (`./mvnw spring-boot:run` lokal,
   oder Container-Image neu bauen/deployen).

> **Trockenlauf:** mit `GET /api/copilot/v1/capabilities` (mit `X-Copilot-Api-Key`-Header)
> kannst du jederzeit prüfen, ob deine neue Aktion in der Liste der unterstützten Aktionen
> erscheint.

---

## Schritt 2: CMS-Proxy-Endpunkt (einmalige Einrichtung)

**Warum überhaupt ein Proxy?** Der `X-Copilot-Api-Key` ist ein gemeinsames Geheimnis zwischen
CMS-Server und Copilot-Backend. **Er darf niemals im Browser landen** (sonst kann jeder, der den
Editor öffnet, ihn auslesen). Der CMS-Server hängt den Key serverseitig dran und reicht den
Body durch.

Empfohlen: ein neuer REST-Endpunkt im CMS-Server (analog zu bestehenden `/rest/...`-Endpoints):

```
POST /rest/copilot/execute
   ← Authentifizierung über die existierende CMS-Session
   ← Request-Body: { action, context, options }   (1:1 Copilot-Format)

   → forwarded an
       POST ${COPILOT_BACKEND_URL}/api/copilot/v1/execute
       Header X-Copilot-Api-Key: ${COPILOT_API_KEY}
   → Response 1:1 zurückgeben
```

Konfiguration auf dem CMS-Server (z. B. via Umgebungsvariablen oder `conf`-Datei):

| Variable | Beispiel |
|---|---|
| `COPILOT_BACKEND_URL` | `http://copilot-backend:8090` |
| `COPILOT_API_KEY` | (gemeinsames Geheimnis, in Vault o. Ä.) |

Sobald dieser Endpunkt einmal steht, ist Schritt 2 erledigt — nicht mehr pro Aktion.

> **Was passiert ohne Proxy?** Du könntest die UI temporär direkt gegen das Copilot-Backend
> sprechen lassen (z. B. lokal mit Vite-Proxy + Header-Injection). Für Produktion bitte nicht —
> der API-Key wäre in CORS-Headers, im Network-Tab und in der `copilot.yml` sichtbar, falls
> man ihn dort ablegt.

---

## Schritt 3: UI-Wiring (einmalige Verdrahtung)

Aktuell rendert die Sidebar die Action-Karten ohne Klick-Verhalten. Die produktive Verdrahtung
ist eine kleine Erweiterung in `cms-ui/apps/editor-ui/src/app/copilot/`:

**3a) Klick-Handler auf der Karte**

In `components/copilot-sidebar/copilot-sidebar.component.html` aus dem `<li>` einen
`<button>` machen oder ein `(click)`-Binding ergänzen:

```html
<li
    class="copilot-action-item"
    [attr.data-id]="action.id"
    (click)="runAction(action)"
>
   …
</li>
```

**3b) Service für den Backend-Call**

Neuer Service `providers/copilot-execution/copilot-execution.service.ts`, vereinfacht:

```ts
@Injectable({ providedIn: 'root' })
export class CopilotExecutionService {
    constructor(private http: HttpClient, private appState: ApplicationStateService) {}

    execute(action: CopilotAction): Observable<CopilotResponse> {
        const context = this.buildContext();          // aus appState (currentItem, fields, …)
        return this.http.post<CopilotResponse>(
            '/rest/copilot/execute',
            { action: action.id, context, options: { tone: 'editorial' } },
        );
    }

    private buildContext(): CopilotContext {
        const editor = this.appState.now.editor;
        const item   = this.appState.now.entities.page[editor.itemId];
        return {
            contentId:    String(item.id),
            project:      item.path?.split('/')[1] ?? '',
            templateId:   item.templateId,
            language:     item.language ?? 'de',
            contentType:  'page',
            fields:       extractFieldValues(item),    // siehe Hinweis unten
        };
    }
}
```

> **Felder extrahieren:** `extractFieldValues` muss die aktuell im Editor sichtbaren Tag-Werte
> liefern (z. B. `title`, `body`). Die einfachste Umsetzung: aus dem NGXS-State des aktuellen
> Items die bekannten Tag-Names lesen. Detaillierter: über die Aloha-Bridge (`AlohaIntegrationService`)
> live aus dem iFrame ziehen, dann hat man immer den ungespeicherten Stand.

**3c) Sidebar zeigt das Ergebnis**

Im `CopilotSidebarComponent`:

```ts
runAction(action: CopilotAction): void {
    this.executingAction = action;
    this.executor.execute(action).subscribe({
        next: (response) => this.showResult(response),
        error: (err)     => this.showError(err),
    });
}
```

Für die Anzeige reicht ein neuer Bereich `.copilot-result` in der Sidebar mit:

- Ladezustand (Spinner, "Generating …")
- Bei `status === "success"`: `result.suggestions[*].content` zeigen, plus „Übernehmen" /
  „Verwerfen"-Buttons
- Bei `status !== "success"`: `error.message` zeigen, „Erneut versuchen"

Apply-in-Field-Logik (für Generate-Aktionen): über `AlohaIntegrationService` den Inhalt
in das aktuell aktive Editable einfügen oder den entsprechenden Tag setzen.

> **Eine einmalige Aufgabe.** Sobald das Wiring steht, kommt jede zusätzliche Aktion
> ohne UI-Code-Änderung dazu — nur JSON-Eintrag (Schritt 4) und ggf. Backend-Code (Schritt 1b).

---

## Schritt 4: Aktion in `copilot.json` eintragen

Datei:  `<CMS-Installation>/ui-conf/copilot.json`
(im Container z. B. `/cms/ui-conf/copilot.json` parallel zur `ui-overrides.json`).

**Schema:**

```jsonc
{
    "enabled": true,                                  // Master-Schalter; ohne `true` ist der Button unsichtbar
    "actions": [
        {
            "id":              "<api-name-aus-backend>",                       // Pflicht
            "labelI18n":       { "de": "<Anzeigename>", "en": "<display>" },   // Pflicht — pro Sprache
            "icon":            "<material-symbol-name>",                       // optional
            "descriptionI18n": { "de": "<Untertitel>", "en": "<subtitle>" },   // optional — pro Sprache
            "prompt":          "<Freier Text>"                                 // optional, vom Backend ungelesen
        }
    ]
}
```

> Die `id` **muss exakt** dem `apiName` der Backend-Aktion entsprechen (z. B. `generate_teaser`,
> `rewrite_tone`). Das ist die einzige Stelle, an der UI-Karte und Backend-Action verknüpft sind.

> **i18n:** `labelI18n` und `descriptionI18n` sind Maps von Sprachcode (z. B. `de`, `en`) auf den
> Anzeigetext. Resolved via die `gtxI18nObject`-Pipe gegen die aktuelle UI-Sprache; Fallback auf
> `en`, dann auf den ersten verfügbaren Wert. Damit braucht es **keinen UI-Rebuild** mehr, um
> Aktionen in einer neuen Sprache zu zeigen — die Datei ist die Single Source of Truth.

**Vollständiges Beispiel:**

```yaml
enabled: true

```json
{
    "enabled": true,
    "actions": [
        {
            "id": "generate_teaser",
            "labelI18n":       { "de": "Teaser generieren", "en": "Generate teaser" },
            "icon": "short_text",
            "descriptionI18n": { "de": "Erstellt einen kurzen Teaser aus dem Body",
                                 "en": "Creates a short teaser from the body" }
        },
        {
            "id": "generate_seo_description",
            "labelI18n":       { "de": "SEO-Description vorschlagen", "en": "Suggest SEO description" },
            "icon": "search",
            "descriptionI18n": { "de": "Generiert eine 155-Zeichen Meta-Description",
                                 "en": "Generates a 155-char meta description" }
        },
        {
            "id": "quality_check",
            "labelI18n":       { "de": "Qualitätscheck", "en": "Quality check" },
            "icon": "spellcheck",
            "descriptionI18n": { "de": "Listet redaktionelle Probleme und Vorschläge",
                                 "en": "Lists editorial issues and suggestions" }
        },
        {
            "id": "find_similar_content",
            "labelI18n":       { "de": "Ähnliche Artikel finden", "en": "Find similar content" },
            "icon": "travel_explore",
            "descriptionI18n": { "de": "Sucht thematisch verwandte Inhalte",
                                 "en": "Finds thematically related content" }
        }
    ]
}
```

**Datei aktualisieren:**

```bash
# Beispiel: lokal in einem Docker-Setup mit dem Container `dockersetupgenticscms-cms-1`
docker exec -i dockersetupgenticscms-cms-1 \
    sh -c "cat > /cms/ui-conf/copilot.json" < copilot.json
```

Verify:

```bash
curl -s http://localhost:8080/ui-conf/copilot.json | python3 -m json.tool
```

Im Browser: Editor-UI-Tab **hart neu laden** (Cmd+Shift+R / Ctrl+Shift+R), damit der
App-Initializer die neue Konfig zieht. Die Karten erscheinen ohne UI-Rebuild.

**Icons:** Die Editor-UI bringt eine bestimmte Subset der Material-Symbols-Schrift mit. Nur
Symbole aus diesem Subset werden angezeigt — Glyphe wie `auto_awesome` sind z. B. **nicht**
enthalten. Verifizierte Icons aus bestehender Editor-UI-Verwendung: `lightbulb`, `edit`,
`edit_note`, `search`, `spellcheck`, `short_text`, `travel_explore`, `cloud_upload`, `history`,
`settings`. Wenn ein gewählter Name als leeres Quadrat erscheint, ist er nicht im Subset und
muss durch ein anderes ersetzt werden.

---

## Lokales Test-Setup

### Copilot-Backend lokal

```bash
cd gentics-cmp-content-copilot-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Standardmäßig läuft das Backend auf Port **8080** und nutzt einen **Mock-LLM-Provider**, der
keine echten LLM-Aufrufe macht. Kein OpenAI-Key nötig.

> **Port-Konflikt mit dem CMS:** das CMS läuft typischerweise ebenfalls auf 8080. Daher beim
> Backend einen anderen Port wählen, z. B.:
>
> ```bash
> ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
>   -Dspring-boot.run.jvmArguments="-Dserver.port=8090"
> ```

Default-API-Key im `dev`-Profil: `dev-secret-key`. Schnelltest:

```bash
curl -X POST http://localhost:8090/api/copilot/v1/execute \
  -H "Content-Type: application/json" \
  -H "X-Copilot-Api-Key: dev-secret-key" \
  -d '{
    "action": "generate_teaser",
    "context": {
      "language": "de",
      "contentType": "article",
      "targetField": "teaser",
      "fields": { "title": "Test", "body": "Hallo Welt" }
    }
  }'
```

Antwort enthält im Mock-Modus eine generierte Beispiel-Suggestion.

### Echtes LLM (OpenAI)

Im Backend-Profil `application.yml` oder per Env:

```yaml
copilot:
  llm:
    provider: openai
    base-url: https://api.openai.com/v1
    api-key: sk-...
    model:    gpt-4o
```

### CMS-Proxy-Konfiguration

Wenn der CMS-Server-seitige Proxy steht (Schritt 2), zwei Werte als Env oder
`conf/`-Property setzen:

```
COPILOT_BACKEND_URL=http://localhost:8090
COPILOT_API_KEY=dev-secret-key
```

CMS neu starten, Editor-UI hart neu laden — Klick auf eine Action-Karte landet beim Backend.

### Editor-UI im Dev-Modus

```bash
cd cms-ui
npx nx serve editor-ui
```

Der Dev-Server proxied `/rest/...` und `/ui-conf/...` an dein laufendes CMS — also reicht es,
die `copilot.json` einmal in den CMS-Container zu legen; der Dev-Server zieht sie automatisch.

---

## Troubleshooting

| Symptom | Wahrscheinliche Ursache | Lösung |
|---|---|---|
| Button taucht nicht auf | JSON fehlt, ist 404, oder `enabled: false` | `curl http://localhost:8080/ui-conf/copilot.json` testen, hart Reload |
| Button taucht nicht auf, JSON wird geladen | Du bist nicht im Edit-Mode einer Page (z. B. Vorschau, Properties, Folder) | Page über „Bearbeiten" öffnen |
| Console: `[Copilot] copilot.json is not valid JSON` | Syntax-Fehler (Komma, fehlende Klammer) | `curl ... \| python3 -m json.tool` zum Validieren, Beispiel oben kopieren |
| Console: `[Copilot] action is missing required …` | Pflichtfelder `id` oder `labelI18n` fehlen / leer | siehe Schema oben — `labelI18n` muss mind. einen Sprachcode-Eintrag haben |
| Aktions-Label leer / falsch | UI-Sprache nicht in `labelI18n` enthalten | Fallback ist `en`, dann erster Wert. Sprache in der Karte ergänzen |
| Karte ist da, Klick tut nichts | UI-Wiring (Schritt 3) fehlt | siehe Schritt 3 oder bestehende `runAction`-Implementierung prüfen |
| Backend antwortet `403 forbidden` | Aktion oder targetField nicht in Allowlist | `copilot.allowed-actions` und `copilot.allowed-target-fields` im Backend-`application.yml` prüfen |
| Backend antwortet `401 unauthorized` | API-Key fehlt im Proxy-Header | `COPILOT_API_KEY` auf CMS-Server-Seite gesetzt? Header heißt `X-Copilot-Api-Key` (case-sensitive) |
| Backend antwortet `502 timeout_error` | LLM-Provider erreicht nicht / zu langsam | Provider-URL, Netzwerk, `copilot.timeout.llm-ms` (Default 30s) |
| Karte ist da aber falsche Aktion läuft | JSON-`id` stimmt nicht mit Backend-`apiName` überein | exakte Schreibweise prüfen — Underscores zählen! (`generate_teaser`, nicht `generateTeaser`) |

---

## Sicherheitshinweise

- **API-Keys gehören nie in den Browser.** Weder in `copilot.json`, noch in URL-Parametern,
  noch in `localStorage`. Immer über den CMS-Proxy hinzufügen.
- **Allowlists eng halten.** `copilot.allowed-target-fields` legt fest, welche Felder die
  AI überhaupt überschreiben darf — das ist die letzte Verteidigungslinie gegen unbeabsichtigte
  Eingriffe in z. B. ID- oder Status-Felder.
- **Content-Logging.** `copilot.logging.redact-content: true` (Default) sollte in Produktion
  bleiben, damit Draft-Inhalte nicht in den Log-Dateien landen.
- **Audit-Trail.** Wenn nachvollziehbar sein muss, welche Suggestion akzeptiert wurde:
  `copilot.audit.enabled: true` setzen und auf der CMS-Seite mitloggen, ob die Suggestion
  übernommen wurde.

---

## Weiterführende Links

- Backend-README mit kompletten Configuration-Properties:
  `gentics-cmp-content-copilot-backend/README.md`
- Beispiel Template-Profile:
  `gentics-cmp-content-copilot-backend/src/main/resources/copilot-template-profiles.example.yml`
- Backend OpenAPI / Swagger UI (lokal): `http://localhost:8090/swagger-ui.html`
- UI-seitige Konfig-Loader-Quellen:
  `cms-ui/apps/editor-ui/src/app/copilot/providers/copilot-config/`
