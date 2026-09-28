---
paths:
  - "src/**/*.{tsx,css}"
---

# Gentics Workspace: Design Guidelines

Dieses Dokument beschreibt das Aussehen der Workspace-UI: Farben, Schrift, Abstände, Formen, Schatten, Icons, Bewegung, Zustände und Bausteine.

---

## 1. Grundregeln

1. **Nur Tokens.** Jede Farbe, jeder Radius, Schatten, Abstand und jede Schriftgröße kommt aus diesem Dokument. Keine weiteren Hex- oder Alpha-Werte.
2. **Fehlt ein Wert,** gilt der nächstliegende Token.
3. **Beide Themes.** Jeder Baustein funktioniert in Hell und Dunkel. Farben laufen immer über Tokens; beim Theme-Wechsel ändert sich nur der Token-Block.
4. **Barrierefreiheit geht vor Branding** (WCAG 2.2 AA), mit zwei festgelegten Ausnahmen (§4.1, §4.2).

---

## 2. Look & Feel

- **Ruhiges Werkzeug, keine Marketing-Seite.** Das Interface wirkt wie eine IDE: feste Spalten, klare 1-px-Linien, dichte, aber luftige Listen. Die bunte Fläche ist die **Vorschau der Website**, nicht die Arbeitsoberfläche drumherum.
- **Weiß trägt.** Arbeitsflächen, Chat, Panels, Karten und Formulare sind weiß (`--surface`). Getönte Flächen (`--tint`) gibt es nur klein: Hover, Chips, Kopfzeilen von Chat-Karten, Fußleisten.
- **Blau heißt: interaktiv.** Azure/Akzent markieren nur Aktion, Auswahl, Fokus und Verweise. Blau ist nie Dekoration und nie Fließtext.
- **Hierarchie über Größe und Farbe, nicht über Gewicht.** Es gibt nur Regular 400 und Medium 500.
- **Eins nach dem anderen.** Ein Schritt ist sichtbar, der Rest wartet. Keine Erklär- oder Hinweiszeilen unter Bausteinen: Labels und Zustände sprechen für sich.
- **Bewegung ist kurz und hat eine Richtung.** Dinge steigen leicht auf oder erscheinen an Ort und Stelle. Nichts springt oder federt (Ausnahme: das Erfolgs-Häkchen).

---

## 3. Farben

### 3.1 Tokens

Jede Farbe, die mit Transparenz gebraucht wird, hat eine `-rgb`-Variante (`rgba(var(--az-rgb), .10)`). Nur dieser Block wechselt zwischen den Themes.

| Token | Hell | Dunkel | Rolle |
|---|---|---|---|
| `--ink` | `#004C77` | `#ECEDEE` | Primärer Text, Überschriften, Werte, starke Labels |
| `--ink-rgb` | `0,76,119` | `152,157,162` | Basis für Linien, Scrollbar, Skeletons |
| `--slate` | `#3E4850` | `#C7CACD` | Sekundärer Text, Beschreibungen, Tabellenköpfe |
| `--slate-rgb` | `62,72,80` | `172,177,182` | Basis für Feldränder, Chevrons u. Ä. |
| `--muted` | `#697278` | `#979A9C` | Meta-Text, Zähler, Overlines, Platzhalter |
| `--azure` | `#029DDD` | `#45B4E8` | Aktiver Zustand, Fokus, Auswahlrahmen, Icon-Akzent, Links |
| `--akzent` | `#1AA4DE` | `#45B4E8` | Füllung primärer Buttons, Senden, Häkchen-Boxen |
| `--az-rgb` | `2,157,221` | `69,180,232` | Basis aller blauen Hover-/Auswahl-Flächen |
| `--surface` | `#FFFFFF` | `#1B1D1F` | Panels, Karten, Chat-Karten, Popover, Inputs |
| `--canvas` | `#FFFFFF` | `#0F1112` | Fläche hinter Chat und Spalten |
| `--tint` | `#F3F6F8` | `#0F1112` | Kleine Füllungen: Zeilen-Hover, Chips, Kopf-/Fußleisten |
| `--stage` | `#EAF4FC` | `#0A0C0D` | Nur die Bühne hinter der Seiten-Vorschau |
| `--solid` | `#004C77` | `#2B2E31` | Eigene Chat-Nachricht, Toast, Auswahl-Leiste, Primär-Button auf dunklen Flächen |
| `--aibg` | `#F2F8FD` | `= --surface` | Hintergrund der Agent-Nachricht |
| `--ailine` | `rgba(az, .18)` | `= --line` | Rand der Agent-Nachricht |
| `--line` | `rgba(ink, .12)` | `rgba(ink, .12)` | Standard-Trennlinie, Kartenrand |
| `--line2` | `rgba(ink, .20)` | `rgba(ink, .20)` | Stärkerer Rand: Buttons, Popover, Composer |
| `--line-field` | `rgba(slate, .40)` = `#B2B6B9` | `rgba(slate, .34)` = `#4C4F52` | Rand von Eingabefeldern |
| `--sh` | `0,76,119` | `0,0,0` | Farbe aller Schatten und Backdrops |
| `--err` / `--errbg` | `#B3261E` / `#FDECEA` | `#F2B5B1` / `#331A18` | Fehler, blockierende Prüfung, Löschen |
| `--warn` / `--warnbg` | `#8A5A00` / `#FFF6E5` | `#EFC470` / `#2E2610` | Warnung, gesperrtes Element, neuer Tag-Typ |
| `--ok` / `--okbg` | `#1B6B4A` / `#EAF6F0` | `#8AD6B2` / `#15291F` | Erfolg, bestandene Prüfung, geänderte Stelle |

`color-scheme` wird pro Theme gesetzt (`light` / `dark`), damit native Formularfelder dem Theme folgen.

### 3.2 Blaue Zustandsflächen (Alpha von `--az-rgb` auf `--surface`)

| Zweck | Wert | Beispiele |
|---|---|---|
| Hover leicht | `.05`–`.07` | Menüeinträge, To-do-Zeilen, sekundärer Button (`.06`) |
| Hover Icon/Ghost | `.10` | Icon-Buttons, Ghost-Buttons, Marke in der Topbar |
| Ausgewählt | Fläche `.09` + Rand `.26` | Aktive Session, aktuelle Seite, gewählte Tabellenzeile |
| Gedrückt / an | `.14`–`.16` | Toggle-Icon-Button, aktiver Viewport, aktiver Tab (`.12` + Rand `.28`) |
| Verweis / Chip | Fläche `.13` + Rand `.30` | `@`-Token, Referenz-Chip |
| Fokus-Halo (Felder) | `0 0 0 3px rgba(az, .16)` | Inputs, Suchfeld, Textarea |
| Treffer-Markierung | `.26`–`.30` | `<mark>` in Suchergebnissen |

Zeilen-Hover in Listen (Sessions, Tabellen, Auswahllisten) nutzt `--tint` statt Blau.

### 3.3 Status-Farben

- Status nie nur über Farbe: immer **Icon + Text** (z. B. `circle-alert` + „2 fehlen“).
- Status-Chip: Text `--err|--warn|--ok` auf `--errbg|--warnbg|--okbg`.
- Prüfzeile mit Fehler: Rand `rgba(err, .30)` plus **3 px** linke Kante in `--err`; Warnung: 3 px linke Kante in `--warn`.
- Ungültiges Feld: Rand `rgba(err, .55)`, Fläche `--errbg`, Pflicht-Stern in `--err`.
- „Neuer Tag-Typ“ / gesperrt durch Template: Warn-Familie (`--warnbg`, Rand `rgba(warn, .45)`).

---

## 4. Kontrast

Berechnet nach der WCAG-2.x-Formel (relative Luminanz); transparente Farben auf ihren Hintergrund gemischt. Schwellen nach WCAG 2.2 AA: **1.4.3** Text ≥ 4.5:1 (große Schrift ≥ 3:1; groß heißt ≥ 24 px regulär oder ≥ 18,66 px fett, Medium 500 zählt nicht als fett); **1.4.11** UI-Grenzen und Fokus ≥ 3:1.

| Paar | Hell | Dunkel | AA erfüllt |
|---|---|---|---|
| `--ink` auf `--surface` | 9.13 | 14.42 | ja |
| `--ink` auf `--tint` / `--aibg` | 8.41 / 8.53 | — | ja |
| `--slate` auf `--surface` | 9.34 | 10.27 | ja |
| `--muted` auf `--surface` | 4.91 | 5.97 | ja |
| `--muted` auf `--tint` / `--aibg` | 4.52 / 4.59 | 6.69 (`--canvas`) | ja |
| Weiß auf `--solid` | 9.13 | 13.66 | ja |
| `--err` auf `--errbg` | 5.72 | 9.23 | ja |
| `--warn` auf `--warnbg` | 5.52 | 9.14 | ja |
| `--ok` auf `--okbg` | 5.83 | 9.02 | ja |
| `--azure` (Fokusring) auf `--surface` | 3.06 | 7.20 | ja (UI ≥ 3) |
| Weiß auf `--akzent` (primärer Button) | 2.83 | 2.35 | nein, Ausnahme §4.1 |
| Weiß auf `--azure` (Button-Hover) | 3.06 | 2.35 | nein, Ausnahme §4.1 |
| `--azure` als Textfarbe (Links) | 3.06 | 7.20 | hell nein, Ausnahme §4.1 |
| `--line-field` auf `--surface` | 2.04 | 2.05 | nein, Ausnahme §4.2 |

**Meta-Text:** Informationstragender Text ist nie heller als `--muted`. Ausgenommen sind deaktivierte Elemente und reine Deko-Icons.

### 4.1 Ausnahme: Markenblau als Fläche und Linkfarbe

Primäre Buttons, Senden, aktive Checkboxen und Plan-Schritte nutzen `--akzent` mit weißem Inhalt, Hover `--azure`. Textlinks und Text-Buttons (z. B. „Show all“, „Add step“) nutzen `--azure`. Diese Paare liegen bei 2.35–3.06:1 und damit unter WCAG 1.4.3. Das ist festgelegt.

- Gilt **nur** für die genannten Stellen.
- Kein Fließtext, keine Meta-Texte und keine Status-Texte in `--azure` oder `--akzent`.
- Weißer Text auf `--akzent`/`--azure` immer in Gewicht 500, mindestens 12 px, kurz (1–3 Wörter), möglichst mit Icon.
- Links in `--azure` werden bei Hover und Fokus unterstrichen (`text-underline-offset: 2px`).

### 4.2 Ausnahme: Rand von Eingabefeldern

Der Ruhe-Rand von Inputs, Selects und Textareas (`--line-field`) liegt bei ≈ 2:1 und damit unter WCAG 1.4.11. Das ist festgelegt.

- Gilt nur für den Ruhe-Rand. Fokus (`--azure` + Halo) und Ungültig (§3.3) bleiben unverändert.
- Jedes Feld hat ein sichtbares Label darüber.

---

## 5. Typografie

- **Schrift:** `'TWK Everett', system-ui, -apple-system, sans-serif`.
- **Mono:** `ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace`. Nur für Code, IDs, Dateinamen.
- **Nur zwei Gewichte:** 400 für Text, Daten und Meta; 500 für Überschriften, Buttons, Labels, Namen in Listen, aktive Tabs. **Nie 600/700** in der Workspace-UI.
- **Grundwert:** 13 px, Zeilenhöhe 1.45, `-webkit-font-smoothing: antialiased`.
- **Zahlen** in Tabellen, Zählern, KPIs und Zeitangaben: `font-variant-numeric: tabular-nums`.

### 5.1 Größen

| Token | Größe | Gewicht | Farbe | Einsatz |
|---|---|---|---|---|
| `--fs-overline` | 10 px, `uppercase`, Laufweite `.07em`–`.08em` | 500 | `--muted` | Abschnitts-Köpfe, Kopf von Chat-Karten (dort `--ink`), Tabellenköpfe, Feld-Keys |
| `--fs-xs` | 11 px | 400 | `--muted` | Meta, Zeitangaben, Zähler, Status in der Kopfzeile |
| `--fs-sm` | 12 px | 400/500 | `--slate` | Sekundärtext, kleine Buttons, Tabellen, Chips |
| `--fs-base` | 13 px | 400/500 | `--ink` | Fließtext, Buttons, Listennamen, Inputs, Panel-Titel |
| `--fs-md` | 14 px | 500 | `--ink` | Titel von Dialog und Drawer, Hervorhebung auf der Review-Seite |
| `--fs-lg` | 17 px | 400 | `--ink` | Eingabe auf dem Dashboard |
| `--fs-xl` | 23 px, Laufweite `-.01em` | 500 | `--muted` | Begrüßung auf dem Dashboard; KPI-Wert in `--ink` |
| `--fs-2xl` | 30 px, Laufweite `-.015em` | 500 | `--ink` | Erfolgs-Überschrift nach dem Veröffentlichen |

Badges: 10 px, 500, `uppercase`, Laufweite `.06em`. Beschriftungen von Formularfeldern: 12 px, 500, `--ink`, über dem Feld.

---

## 6. Form: Radius, Linien, Schatten

### 6.1 Radius

| Token | Wert | Einsatz |
|---|---|---|
| `--r-xs` | 4 px | Checkbox, `kbd`, Mini-Schließen-Buttons, Textmarkierungen, Element-Labels |
| `--r-sm` | 6 px | Badges, `@`-Tokens, Status-Chips |
| `--r-md` | 8 px | Kleine Buttons, Icon-Buttons, Inputs, Selects, Menü- und Popover-Einträge, Tabs |
| `--r-lg` | 10 px | Buttons, Listenzeilen, Prüfzeilen, Anhänge, KPI-Kacheln, Toast |
| `--r-xl` | 12 px | Chat-Karten (Parts), Popover, Menüs, schwebende Leisten, Vorschau-Seite |
| `--r-2xl` | 14 px | Karten, Chat-Blasen, Composer |
| `--r-3xl` | 16 px | Modale Dialoge |
| `--r-full` | 999 px | Avatare, Pills, Icon-Chips, Schritt-Nummern |

Chat-Blasen haben eine „Ecke“ zum Absender: Agent unten links 5 px, eigene Nachricht unten rechts 5 px. Mobile-Rahmen der Vorschau: 22 px.

### 6.2 Linien

- Zonen werden durch **1-px-Linien** in `--line` getrennt, nicht durch Schatten oder Farbflächen.
- `--line2` für Ränder, die ein Bedienelement umreißen (Buttons, Composer, Popover, Dialog).
- Splitter: Trefferfläche 9 px, sichtbar 1 px `--line`; bei Hover/Ziehen 2 px `--azure` plus Griff (4 × 30 px → 46 px, Farbe von `rgba(ink,.22)` zu `--azure`).

### 6.3 Schatten (Farbe immer `rgba(var(--sh), a)`)

Standard ist **flach**. Schatten bekommt nur, was über dem Inhalt schwebt.

| Token | Wert | Einsatz |
|---|---|---|
| `--sh-1` | `0 1px 2px .06` | Composer in Ruhe, Vorschau-Seite (`0 1px 3px .07`), aktiver Segment-Knopf (`.10`) |
| `--sh-float` | `0 6px 22px .13, 0 1px 2px .08` | Schwebender Composer; Release-Leiste `0 4px 16px .10` |
| `--sh-pop` | `0 12px 34px .18` | Popover, Menüs, Hover-Karte (`.20`), Element-Menü (`0 14px 38px .22`) |
| `--sh-sheet` | `0 18px 40px .18` | Seiten-Einstellungen, die über die Vorschau fallen |
| `--sh-modal` | `0 18px 60px .28` | Modale Dialoge; Drawer `0 0 40px .22` |
| `--sh-toast` | `0 12px 32px .32` | Toast, Auswahl-Leiste (`0 8px 22px .30`) |

Hintergrund hinter Drawer/Dialog: `rgba(var(--sh), .28)`.

---

## 7. Abstände & Dichte

Die Oberfläche ist **kompakt**. Skala in px: `2 · 4 · 6 · 8 · 10 · 12 · 14 · 16 · 20 · 24 · 28`.

| Element | Innenabstand / Maß |
|---|---|
| Button | 8 × 12 px; klein 4 × 10 px; Abstand Icon–Text 6 px |
| Icon-Button | 28 × 28 px, Icon 16–20 px |
| Input / Suchfeld | Höhe 32 px, 6 × 8 px |
| Listenzeile | 8 × 8 px, Abstand zwischen Zeilen 2 px |
| Chat-Karte (Part) | Kopf 8 × 10 px, Körper 10 × 10 px, Fuß 8 × 10 px |
| Chat-Blase | 10 × 12 px, Abstand zwischen Nachrichten 12 px |
| Karte / Panel-Inhalt | 12–16 px |
| Topbar | Höhe 48 px, seitlich 12 px |
| Panel-Kopf | Höhe mind. 44 px |
| Tab-Leiste (Vorschau) | Höhe 40 px |

---

## 8. Zustände & Fokus

| Zustand | Aussehen |
|---|---|
| **Fokus (Tastatur)** | Jedes interaktive Element: `box-shadow: 0 0 0 2px var(--surface), 0 0 0 4px var(--azure)` bei `:focus-visible`. |
| **Fokus (Textfeld)** | Rand `--azure` + Halo `0 0 0 3px rgba(az,.16)` statt Ring. |
| **Hover** | Siehe §3.2. Übergang 120–140 ms auf `background`, `color`, `border-color`, `opacity`. |
| **Ausgewählt** | `rgba(az,.09)` + Rand `rgba(az,.26)`; bei Kacheln 2 px `--azure` + Häkchen-Badge. |
| **Deaktiviert** | `opacity: .42`, `cursor: not-allowed`, kein Hover. |
| **Nur lesen** | Fläche `--tint`, Text `--slate`, Rand `--line` (bei Eigenschafts-Feldern gestrichelt). |
| **Ungültig** | Siehe §3.3. |
| **Lädt** | Skeleton: Verlauf `rgba(ink,.05 → .11 → .05)`, 200 % breit, 1.1 s linear endlos. „Denkt“: drei 5-px-Punkte in `--akzent`, 1 s, versetzt um 0.14 s, 3 px Hub. Streaming: 6 × 13 px Cursor in `--azure`, blinkt im 1-s-Takt. |
| **Leer** | Zentrierter Text 12–13 px in `--muted`, 26 px oben/unten. Keine Illustration. |
| **Aufmerksamkeit** | Puls-Ring `0 → 12px`, `rgba(az,.45 → 0)`, 1 s, höchstens 2-mal. |

---

## 9. Icons

- **Bibliothek:** Lucide. Kein Icon-Font.
- **Strichstärke:** 1.7; `stroke-linecap` und `stroke-linejoin`: `round`.
- **Größen:** 14 · 16 · 18 (Standard) · 20 · 22 px. Inline in Chips 11–13 px.
- **Farbe:** `currentColor`. Dezente Icons in `--muted`/`--slate`, Icons mit Aussage (Karten-Kopf, Verweis, aktive Aktion) in `--azure`, Status-Icons in ihrer Statusfarbe.
- **Icon-Chip** (runder Hintergrund `rgba(az,.11)`, 28 px): nur für Absender-Avatar, herausgehobene Hauptaktionen und Eintragsarten im `@`-Menü.
- **Logo:** Gentics-Logomark als Maske in `currentColor` = `--azure`, Seitenverhältnis 218:256, in der Topbar 20 × 24 px.

**Icon-Zuordnung**

| Bedeutung | Icon | Bedeutung | Icon |
|---|---|---|---|
| Bestätigen | `check` | Schließen | `x` |
| Aufklappen | `chevron-down` | Weiter | `chevron-right` |
| Senden | `arrow-up` | Zurück | `arrow-left` |
| Hinzufügen | `plus` | Suchen | `search` |
| Sessions / Chat | `messages-square` | `@`-Verweis | `at-sign` |
| Datei anhängen | `paperclip` | Bild hinzufügen | `image-plus` |
| Spracheingabe | `mic` | Stoppen | `square` |
| Verbatim | `quote` | Release | `rocket` |
| Erfolg | `circle-check` | Fehler | `circle-alert` |
| Abgebrochen | `circle-x` | Info | `info` |
| Hilfe | `circle-question-mark` | Geprüft | `badge-check` |
| Gesperrt / entsperrt | `lock` / `lock-open` | Ansehen | `eye` |
| Bearbeiten | `pencil` | Notiz bearbeiten | `notebook-pen` |
| Löschen | `trash` | Rückgängig | `undo-2` |
| Aktualisieren | `refresh-cw` | Verlauf | `rotate-ccw-clock` |
| Speichern | `save` | Einstellungen | `sliders-horizontal` |
| Seite / Dokument | `file-text` | Ordner | `folder` |
| Bild | `image` | Bildauswahl | `images` |
| Hochladen | `upload` | Link | `link` |
| Extern öffnen | `external-link` | Vergrößern / verkleinern | `maximize-2` / `minimize-2` |
| Desktop | `monitor` | Mobil | `smartphone` |
| Seitenleiste öffnen / schließen | `panel-left-open` / `panel-left-close` | Dunkel / Hell | `moon` / `sun` |
| Baumansicht | `list-tree` | Tabelle | `rows-3` |
| Checkliste | `list-checks` | Alle erledigt | `check-check` |
| Ebenen | `layers` | KI / automatisch verbessern | `sparkles` / `wand-sparkles` |
| Fett / Kursiv / Liste | `bold` / `italic` / `list` | Text | `type` |
| Tauschen | `arrow-left-right` | Abspielen | `play` |
| Blockiert | `ban` | Schnellaktion | `zap` |
| Barrierefreiheit | `accessibility` | Ablauf | `route` |
| Person | `user` | Datenquelle | `database` |

Für Bedeutungen, die hier fehlen, gilt das nächstliegende Lucide-Icon.

---

## 10. Bewegung

- **Kurve:** `cubic-bezier(.2, .8, .2, 1)` für alles, was erscheint oder sich bewegt.
- **Dauer:** Zustandswechsel 120–140 ms · Popover/Menü 160 ms · Nachricht/Karte 240–260 ms · Dashboard-Einstieg 340 ms, gestaffelt um je 30–40 ms · Vorschau-Spalte ein-/ausfahren 420 ms.
- **Muster:** `rise` (7 px nach oben + einblenden) für Nachrichten und Listen, `pop` (5 px + Skalierung .985) für Popover und Karten, `drop` (−8 px) für Sheets, die von oben fallen. Drawer: 24 px von links.
- **Erfolg:** Häkchen-Marke skaliert von .6 auf 1 (`cubic-bezier(.2,1.4,.4,1)`, 500 ms). Die einzige federnde Bewegung.
- **`prefers-reduced-motion: reduce`:** alle Animationen und Übergänge auf praktisch 0 ms.

---

## 11. Layout-Muster

- **Shell:** Topbar (48 px, `--surface`, Linie unten) → Arbeitsfläche mit festen Höhen. Nur ausgewiesene Zonen scrollen, nie die ganze Seite.
- **Drei Spalten mit Splittern:** Sessions (292 px) · Chat (mind. 320 px, flexibel) · Vorschau (46 %). Unter 1180 px: 228 px / 280 px / 42 %. Unter 900 px: 200 px / 240 px / 40 %. Spalten werden schmaler, aber nie zum Overlay.
- **Vorschau erst bei Bedarf:** Bis die Seite angefasst wird, bleibt die Vorschau-Spalte weg und der Chat hat die volle Breite (Inhalt max. 760 px, zentriert).
- **Chat:** Nachrichten max. 740 px breit. Agent links, eigene Nachricht rechts. Der Composer schwebt unten über dem Verlauf, mit Verlauf zu `--canvas` dahinter; die Release-Leiste sitzt direkt darüber.
- **Dashboard:** eine Eingabe, sonst nichts. Oben verankert (Abstand oben `min(28vh, 280px)`), max. 660 px breit. Die Eingabe hat **keinen Kasten**: Text auf der Fläche, beim Fokus eine weiche Fläche `rgba(az,.07)`. Offene To-dos ab 1360 px als Karte oben rechts (330 px), darunter in der Spalte.
- **Review:** links die Vorschau (flexibel, auf `--stage`), rechts ein Panel mit 400 px (340 px unter 1180 px) mit Checkliste und Veröffentlichen-Aktionen unten.
- **Vorschau-Bühne:** Die Seite liegt als weißes Blatt (`--surface`, Rand `rgba(ink,.15)`, `--r-xl`, `--sh-1`) auf `--stage`, 12 px Rand. Mobil: 390 px breit, 6-px-Rahmen `rgba(ink,.34)`, Radius 22 px.
- **Overlays:** Drawer links 380 px · Dialog max. 720 × 600 px · Popover 350–410 px · Toast unten mittig, max. 520 px.
- **Kundenseite in der Vorschau:** hat eigene Stile (u. a. Gewicht 600, Verläufe). Diese gelten **nicht** für die Workspace-UI.

---

## 12. Bausteine

**Buttons**
- *Primär:* Fläche `--akzent`, Text weiß, Rand gleich Fläche; Hover `--azure` (§4.1).
- *Sekundär:* `--surface`, Rand `--line2`; Hover Rand `--azure` + Fläche `rgba(az,.06)`.
- *Ghost:* transparent; Hover `rgba(az,.10)`.
- *Gefahr:* Text `--err`, Rand `rgba(err,.30)`; Hover `--errbg` + Rand `--err`.
- *Auf dunklen Flächen* (Hover-Karte, Toast): Primär = `--solid` mit weißem Text; Toast-Aktion `rgba(255,255,255,.14)`, Hover `.26`.
- Alle: Gewicht 500, `--r-lg` (klein `--r-md`), Icon links.

**Senden / Mikrofon:** 30 px mit `--r-md`, auf dem Dashboard 34 px mit `--r-lg`. Senden: `--akzent`, weißes Pfeil-Icon. Mikrofon: Rand `--line2`; beim Zuhören Fläche `--azure` + Puls, daneben Wellen-Balken (3 px, `--azure`) und Zeit in `tabular-nums`. Countdown als Ring um Senden in `--azure`.

**Eingabefelder:** Fläche `--surface`, Rand `--line-field`, `--r-md`, 32 px hoch, Label darüber. Select mit eigenem Chevron (zwei 5-px-Dreiecke), kein natives Aussehen. Textarea vertikal skalierbar.

**Checkbox / Radio:** 16–18 px, Rand 1.5 px `--line2` (bzw. `rgba(slate,.45)`), `--r-xs`. An: Fläche `--akzent`/`--azure`, weißes Häkchen. Radio an: 4-px-Ring `--azure`.

**Badge:** 10 px, `uppercase`, `--r-sm`, 2 × 8 px. Standard `rgba(az,.12)` + `--ink`; Status-Varianten siehe §3.3; „flat“ ohne Fläche in `--muted`.

**`@`-Token / Referenz:** `rgba(az,.13)`, Rand `rgba(az,.30)`, `--r-sm`, 12 px, 500, Icon 13 px `--azure`, max. 270 px mit Auslassungspunkten. **Verbatim** = gleicher Token mit **gestricheltem** Rand; Verbatim-Textstelle: `rgba(az,.14)` + 1-px-Unterstrich `--azure`. Verbatim-Block in Nachrichten: `rgba(az,.11)`, 2 px linke Kante `--azure`, rechts `--r-md`.

**Chat-Blasen:** Agent: `--aibg`, Rand `--ailine`, `--r-2xl`, Ecke unten links 5 px. Eigene: `--solid`, weißer Text, Ecke unten rechts 5 px; Tokens darin `rgba(255,255,255,.20)`. Absender-Zeile: Overline in `--muted`. Avatar 26 px rund.

**Chat-Karte (Part):** `--surface`, Rand `--line`, `--r-xl`. Kopf: `--tint`, Overline in `--ink`, Icon `--azure`, Linie darunter. Fuß: Linie oben, Beschriftung links in `--muted`, Aktionen rechts. Gilt für Baumansicht, Auswahlliste, Eigenschaften, Bildraster, Tabelle, Plan, Checkliste.

**Listenzeile:** `--r-lg`, Name 13 px/500 mit Auslassungspunkten, Meta 11 px `--muted`. Hover `--tint`; aktuell = Ausgewählt-Stil (§8). Löschen-Knopf erscheint bei Hover/Fokus, Hover `--errbg`/`--err`.

**Tabelle:** 12 px; Kopf als Overline mit Linie `--line2`; Zeilen 6 × 8 px mit Linie `--line`; Zahlen `tabular-nums` in `--slate`. Status-Punkt 7 px (ok / warn / offline = `rgba(slate,.40)`).

**Bildraster:** 3 Spalten, 8 px Abstand, Kacheln 16:10, `--r-lg`. Ausgewählt: 2 px `--azure` + runder Häkchen-Badge oben rechts. Gesperrt: entsättigt + Schloss-Badge. Upload-Kachel: gestrichelt `--line2` auf `--tint`.

**Plan-Schritte:** Nummer 18 px rund in `rgba(az,.13)`; erledigt `--okbg`/`--ok`; läuft `--akzent` + Puls. Bearbeiten direkt in der Zeile (Rand `--line2`, Fokus wie Textfeld).

**Checkliste / Guidelines:** Pro Guideline eine Gruppe (`--line`, `--r-xl`) mit Kopf (Name 500, Status rechts in Statusfarbe). Punkte mit Status-Icon; nicht erfüllte Punkte in `--ink`/500 mit Lösungs-Button rechts; erfüllte Punkte zusammenklappbar.

**KPI-Kachel:** Rand `--line`, `--r-lg`, Wert 23 px/500 `tabular-nums`, Label 11 px `--muted`.

**Tabs / Segmente:** Tab: 12 px, `--r-md`, aktiv `rgba(az,.12)` + Rand `.28` + 500. Segment-Umschalter: Spur `--tint`, aktives Segment `--surface` + `--sh-1`.

**Popover / Menü:** `--surface`, Rand `--line2`, `--r-xl`, `--sh-pop`. Gruppen-Köpfe als Overline. Eintrag: `--r-md`, Hover `rgba(az,.07)`, Tastatur-Auswahl `rgba(az,.15)` + innerer Ring `rgba(az,.55)`. Kopf- und Fußzeilen `--tint`.

**Dialog / Drawer:** `--surface`, Rand `--line2`; Dialog `--r-3xl` + `--sh-modal`; Drawer ohne Radius, `--sh-modal`-Variante. Kopf mit Titel 14 px/500, Fußzeile `--tint` mit Linie oben.

**Toast:** `--solid`, weißer Text, `--r-lg`, `--sh-toast`, Icon `#7FC7EA` (4.90:1 auf `--solid` hell), optional eine Aktion (z. B. „Undo“).

**Elemente in der Vorschau:** Hover 1.5 px gestrichelt `rgba(az,.55)`, Abstand 2 px (nur das innerste Element). Ausgewählt 2 px `--azure`. Ziel des Chats 2 px `--azure` + Halo `6px rgba(az,.12)`. Geändert: Fläche `rgba(ok,.07)` (Buttons: Ring `3px rgba(ok,.28)`). Fehler 2 px `--err`. Wird bearbeitet 2 px `--akzent`. Gesperrt: Hover-Rahmen in `rgba(warn,.55)`. Läuft: Skeleton über dem Element. Element-Label: 10 px, 500, Fläche `--ink`, Text `--surface`, `--r-xs`, oben links. Nicht betroffene Teile im Fokus-Modus: Deckkraft .24, Sättigung .3.

**Erfolg nach dem Veröffentlichen:** Marke 72 px, Radius 24 px, `--okbg`/`--ok`, Ring `8px rgba(ok,.06)`; Überschrift `--fs-2xl`; darunter die Eingabe für eine neue Session.

---

## 13. Texte (Ton)

- Kurz, aktiv, Satzanfang groß, Rest klein („Review & release“, „Find a session“, „What should happen?“).
- Keine Erklärzeilen unter Feldern, Karten oder Buttons. Hilfetext nur dort, wo er Pflicht ist (Seiten-Einstellungen).
- Zwei Sprachen (EN/DE).
- Status sagt, was ist („3 published, 1 sent to review“), nicht, was das System tut.

---

## 14. Niemals

- **Nie** Fließtext oder große Textblöcke in Azure oder Akzent.
- **Nie** Blau auf Blau mit wenig Kontrast (z. B. `--azure` auf `--stage`).
- **Nie** Gewicht 600 oder 700 in der Workspace-UI.
- **Nie** Fokus ausblenden, ohne ihn zu ersetzen.
- **Nie** informationstragenden Text heller als `--muted`.
- **Nie** Status nur über Farbe.
- **Nie** große getönte Flächen als Hintergrund der Arbeitsbereiche; `--tint` nur klein, `--stage` nur hinter der Vorschau.
- **Nie** Schatten zur Trennung von Zonen; Schatten nur für schwebende Ebenen.
- **Nie** Farben, Radien oder Schatten außerhalb dieses Dokuments.
