# feature-tasks / Nachrichten

In diesem Package wird die Aufgaben-Seite für den ANDP-Portal realisiert. Die erstellten screens können im andp-mobile-app, aber auch im MicroApp-Aufgaben (andp-ma-tasks) verwendet werden.

## Projektstruktur

Dieses Projekt ist in mehrere Ordner unterteilt, um eine klare und organisierte Struktur zu gewährleisten. Jeder Ordner hat einen spezifischen Zweck, der die Entwicklung und Wartung erleichtert. Im Folgenden wird die Bedeutung jedes Ordners und dessen Inhalt beschrieben:

### `assets/`

Dieser Ordner enthält alle statischen Ressourcen der Anwendung, wie Bilder, Icons und Schriften.

- **Bilder**: PNGs, JPGs, etc.
- **Icons**: SVGs oder andere Icons, die für die App benötigt werden.
- **Schriften**: Custom Fonts oder andere Typografien.

### `components/`

Hier befinden sich alle wiederverwendbaren UI-Komponenten, die in verschiedenen Teilen der Anwendung eingesetzt werden. Die Komponenten können in einzelne Dateien oder in Unterordner für komplexe Komponenten organisiert werden.

- **Beispiel**: Buttons, Input-Felder, Modals, etc.

### `context/`

In diesem Ordner werden alle React-Contexts gespeichert, die der globalen Zustandsverwaltung der Anwendung dienen. Dies ist nützlich, um globale Daten wie Authentifizierungsstatus oder das aktuelle Theme zu teilen.

- **Beispiel**: AuthContext, ThemeContext.

### `hooks/`

Hier werden benutzerdefinierte React-Hooks abgelegt, die über verschiedene Komponenten hinweg verwendet werden können. Diese Hooks enthalten Logik, die in mehreren Komponenten benötigt wird.

- **Beispiel**: `useAuth` für Authentifizierung, `useFetch` für API-Aufrufe.

### `screens/`

Dieser Ordner enthält die Ansichten oder Hauptseiten der Anwendung. Jede Seite stellt eine Route der Anwendung dar und verwendet meist mehrere Komponenten.

- **Beispiel**: HomeScreen, ProfileScreen, SettingsScreen.

### `services/`

In diesem Ordner befinden sich alle servicebasierten Logiken, wie z.B. API-Aufrufe oder Interaktionen mit externen Diensten.

- **Beispiel**: API-Aufrufe, Authentifizierungsdienste.

### `store/`

Hier wird der globale Zustand der Anwendung gespeichert, insbesondere wenn eine State-Management-Library wie Redux verwendet wird. Enthält Actions, Reducers und Selectors.

- **Beispiel**: Auth-Reducer, User-Actions.

### `styles/`

Dieser Ordner enthält alle globalen Styles und Design-Token, die in der App verwendet werden. Dies kann auch Themen wie Farben, Schriftarten und Abstände umfassen.

- **Beispiel**: Farbschemata, Typografie, globale CSS/SCSS-Styles.

### `types/`

Hier werden Typdefinitionen für TypeScript gespeichert. Dies umfasst globale Interfaces, Typen und Enumerationen, die in der gesamten App verwendet werden.

- **Beispiel**: Globale Types und Interfaces.

### `utils/`

Dieser Ordner enthält Hilfsfunktionen und Utilities, die nicht spezifisch für eine bestimmte Komponente oder Funktionalität sind, aber an mehreren Stellen im Projekt verwendet werden.

- **Beispiel**: Datenformatierung, Validierungsfunktionen.
