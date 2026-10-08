# Project Instructions

This repository is a React + TypeScript application using Vite, plain CSS, TanStack Query, TanStack Router, Web Streams, Vitest, Testing Library, Playwright, ESLint with ESLint Stylistic, npm, GitHub Actions, and Renovate. It is intentionally developed with AI assistance. Follow these instructions for every task.

- Use npm as the package manager. Do not change it to pnpm, Yarn, Bun, or another tool unless explicitly requested.
- Requires Node.js `>=26.4.0` and npm `>=11` (`engines` in `package.json`; `.nvmrc` pins `v26.4.0`). If the active Node version is lower, say so before reporting validation results.

## Project Layout and Commands

This app lives in `apps/workspace-ui` of the `cmp` repository. Work only inside this folder unless the task requires otherwise.

Changelog Folder: changelog

- `src/services/<name>Service/` one folder per service, test next to it: `apiService/apiService.ts` GenAIx API client (`genaix/`: GenAIx types, `schema.d.ts` generated, aliases in `types.ts`) · `cmsApiService/cmsApiService.ts` CMS REST client · `httpService/httpService.ts` minimal JSON request helper (`httpRequest`, `HttpError`) · `src/helper/<name>/` app-wide helpers, test next to them: `queryClient/queryClient.ts` the app's TanStack Query client (`createQueryClient`, error notifications after the last retry) · `errorMapper/errorMapper.ts` error to i18n key (`errorMessageKey`) · `composerParts/composerParts.ts` a composer's field to message parts, verbatim passages · `src/pages/<Name>/` one folder per page shown by a route (e.g. `DashboardPage`, `SessionPage`); the routes are declared in `src/router.tsx` · `src/hooks/` React hooks (e.g. `useComposer`) · `src/store/` Zustand stores · `src/i18n/` i18next setup and `locales/{en,de}/common.json` · `src/components/ui/` the component layer (shadcn/ui on Base UI + Tailwind; wrap the app in `UiProvider`) · `src/catalogue/` component catalogue (`index.html`, served by `npm run catalogue`) · `src/test/setup.ts` Vitest setup.
- Unit/component tests sit next to the code as `src/**/*.test.{ts,tsx}`. E2E tests live in `e2e/` (for example `e2e/App.spec.ts`).
- Import from `src/` with the `@/` alias (for example `@/services/apiService/apiService`).

| Command | Purpose |
| --- | --- |
| `npm run dev` | Vite dev server; proxies `/genaix/api/v1` and adds the GenAIx headers (README, "Local GenAIx API"), and `/rest` to `CMS_PROXY_TARGET` when set (`.env.example`) |
| `npm run catalogue` | Component catalogue on its own Vite dev server, `:5174` (`vite.catalogue.config.ts`); not part of `npm run dev` or `npm run build` |
| `npm run mock:genaix` | GenAIx mock on `:8123` from `../../../api-contract` (`GENAIX_MOCK_PORT=8123`, the port of its docker compose stack) |
| `npm run typecheck` | `tsc -b` |
| `npm run generate:api` | Regenerate `src/services/apiService/genaix/schema.d.ts` from `.claude/contracts/openapi.yaml` |
| `npm run lint` | ESLint; warnings fail |
| `npm run format` / `npm run lint:fix` | ESLint autofix |
| `npm test` / `npm run test:run` | Vitest in watch mode / once |
| `npm run test:coverage` | Vitest with coverage |
| `npm run test:e2e` / `npm run test:e2e:ui` | Playwright (starts Vite itself). Browsers must be installed first: `npx playwright install` |
| `npm run check` | typecheck, lint, `test:run`, build |

## Core Principles

### Inspect before modifying

1. Inspect the relevant files and understand the existing architecture and conventions.
2. Search for existing implementations that solve a similar problem, and reuse existing patterns where appropriate.
3. Only then make changes. Do not modify files based on assumptions about how the project works.

### Preserve the existing architecture

- Do not restructure the project unless the task requires it. Do not introduce new architectural patterns merely because they are personally preferred.
- When extending functionality, follow the existing structure and conventions.
- If an architectural change appears necessary, explain why before making a broad change. Do not silently make major architectural decisions.

### Prefer the smallest change

- Make the smallest change that correctly solves the requested problem. A task should not become an excuse to "clean up" unrelated code.
- Avoid unrelated refactoring, unnecessary renaming, speculative abstractions, large-scale rewrites, formatting unrelated files, and introducing dependencies for trivial functionality.

### Ambiguous requirements

- Use existing project conventions where possible, and do not invent behavior.
- Ask for clarification when the ambiguity materially affects the implementation.

## Do Not Invent Things

- Never invent API endpoints, request fields, response fields, authentication mechanisms, environment variables, database schemas, library APIs, configuration options, or undocumented behavior. If an API or behavior is unclear, inspect the repository and available documentation first. If it still cannot be determined, state the uncertainty instead of guessing.
- IMPORTANT: Never claim that tests passed, lint passed, type checking passed, a build succeeded, an API works, a browser test passed, or something was deployed unless the relevant command was actually executed and succeeded. If a command could not be run, explicitly say so.

## Technology Guidelines

### React

- Prefer functional components, hooks, composition, small focused components, and semantic HTML. Avoid unnecessary component abstractions.
- Do not put business logic directly into large UI components when it can reasonably live in a feature or domain module.

### Text and i18n

- UI text goes through react-i18next (`useTranslation()`, `t('key')`) instead of being hard-coded. The app is in English and German.
- Add every new key to both `src/i18n/locales/en/common.json` and `src/i18n/locales/de/common.json` (default namespace `common`, fallback `en`).

### Styling

- Use plain CSS. Do not add a CSS preprocessor (Sass/SCSS, Less) or CSS-in-JS.
- **Exception: the component layer `src/components/ui/`** is shadcn/ui on Base UI (`@base-ui/react`, not Radix) styled with Tailwind. Tailwind, `cn()` (`ui/utils.ts`), `cva`, `clsx` and `tailwind-merge` are allowed only there. `ui/ui.css` clears Tailwind's theme and maps only the `design.md` tokens (1 spacing unit = 1 px, alpha via `/`, e.g. `bg-azure/10`), and scans only that folder, so Tailwind classes elsewhere produce no CSS; ESLint (`no-restricted-syntax`, `no-restricted-imports`) reports them. Everything outside the layer uses the components and CSS Modules. Components added with `npx shadcn add` (`components.json`) must be restyled with these tokens before use.
- Component styles use CSS Modules: `Component.module.css` next to the component, imported as `import styles from './Component.module.css';`.
- Global styles (design tokens, base styles) live only in `src/index.css`. Do not add global class names for components.
- Design tokens are CSS custom properties. Visual values (colors, radii, shadows, spacing, font sizes) come from `.claude/rules/design.md`.
- Native CSS nesting, cascade layers, and container queries are allowed. `var()` does not work in `@media` conditions; write breakpoints as literal values.

### Icons

- Lucide via `lucide-react` (`design.md` §9). `UiProvider` sets the defaults (stroke 1.7, 18 px); pass `size` only for other sizes.
- Never draw icons as inline `<svg>` paths, not even copied from a design draft. Use the Lucide icon named in `design.md` §9, or the nearest one.

### UI Components

- Check `src/components/ui/` and the catalogue (`npm run catalogue`, `src/catalogue/Catalogue.tsx`) before building UI; the catalogue shows every component in every variant and state. The layer has Button (variants `primary`, `secondary`, `ghost`, `danger`, `solid`; sizes `default`, `sm`, `icon`, `icon-lg`), Checkbox, Label, Select, Tabs, Tooltip, DropdownMenu (with checkbox and radio items), Dialog, Drawer, and toasts (`useToast()` from `ui/use-toast`).
- Do not rebuild what the layer has. Outside it, native `<button>`, `<select>`, `<dialog>` and inline `<svg>` are ESLint errors (`no-restricted-syntax`). If a variant or size is missing, add it to the component in the layer and to the catalogue. Do not restyle a layer component from outside: a CSS Module class on it competes with its Tailwind utilities.
- Base UI, not Radix: compose with the `render` prop (`<DropdownMenuTrigger render={<Button variant="ghost" />}>`), not `asChild`. Checkbox and radio items keep a menu open unless they get `closeOnClick`; `finalFocus` on `DropdownMenuContent` sets where focus goes when the menu closes.
- A new or changed layer component or variant goes into the catalogue (texts under `catalogue.*` in both locales) and gets a test next to it.
- Tests that render layer components wrap them like the tests in `src/components/ui/`: `render(<X />, { wrapper: UiProvider })` and `import '@/i18n'`. `useToast()` throws without `UiProvider`.
- App components: one folder per component, `src/components/<Name>/` with `<Name>.tsx`, `<Name>.module.css` and `<Name>.test.tsx`. Pages: `src/pages/<Name>/`.
- CSS Modules take every visual value from the tokens in `src/index.css`: no hex or literal `rgb()`/`hsl()` colours (alpha via `rgba(var(--interactive-rgb), .1)`), no durations or `cubic-bezier()` (use `--duration-state`, `--duration-popover`, `--duration-card`, `--easing`), no font weight above 500. `src/test/designTokens.test.ts` checks every `*.module.css`.
- Theme: `index.html` pins `data-theme="light"`; `useThemeStore` switches it. Every component must work in both themes (`design.md` §1).

### State Management

Choose the simplest state-management mechanism appropriate for the type of state. Do not move state into a more global system unless the scope of the state requires it.

| State | Preferred tool |
| --- | --- |
| Local to a component or small component subtree | React `useState` / `useReducer` |
| Shared client/UI state accessed by multiple components or features | Zustand |
| Server/API state | TanStack Query |
| State that should be represented in the URL | TanStack Router search/route params |
| Form state | Local React state |

### Zustand

- Stores belong in `src/store/`, organized by feature or domain rather than a single global store. Use descriptive filenames such as `useAuthStore.ts`, `useUIStore.ts`, `useCartStore.ts`.
- Appropriate for UI state shared across unrelated components, application-wide preferences, client-side workflows, ephemeral client-side state shared across features, and other client-side state that does not belong in React local state, TanStack Query, or URL state.
- Do not use Zustand for server state. Keep business logic close to the state it manages, but do not move API communication into stores when the operation is better handled by the API layer and TanStack Query.
- Use TypeScript interfaces or types for store state and actions. Prefer selectors so components subscribe only to the state they need.
- Follow the existing project structure and conventions. Do not introduce a new store architecture or directory structure if an existing pattern is already present.

```ts
import { create } from 'zustand';

interface UIState {
    isSidebarOpen: boolean;
    toggleSidebar: () => void;
}

export const useUIStore = create<UIState>((set) => ({
    isSidebarOpen: false,

    toggleSidebar: () =>
        set((state) => ({
            isSidebarOpen: !state.isSidebarOpen,
        })),
}));
```

### TypeScript

- Use TypeScript strictly (`"strict": true` in `tsconfig.app.json` and `tsconfig.node.json`).
- Prefer precise types over `any`, and `unknown` with proper narrowing over `any`. Explicit `any` is an ESLint error (`@typescript-eslint/no-explicit-any`). If `any` is genuinely unavoidable, disable the rule for that line only and give the reason: `// eslint-disable-next-line @typescript-eslint/no-explicit-any -- <reason>`.
- Do not use type assertions merely to silence TypeScript errors. Understand why a type assertion is safe before using one.
- API boundaries should have explicit types.

### TanStack Query

- Use TanStack Query for server state (data retrieved from or synchronized with backend APIs): fetching, caching, synchronization, mutations, invalidation, request state, and server-side errors. Do not manually recreate caching or request-state behavior that TanStack Query already provides.
- Do not duplicate server state unnecessarily in React state, and do not duplicate it in Zustand.
- Use stable and meaningful query keys. For mutations, ensure affected queries are invalidated or updated appropriately after successful changes.

### API Access

- Do not call `fetch()` directly from React components. Components consume hooks or feature-level functions; API communication goes through the project's API client or an appropriate feature-level abstraction, for example `src/services/apiService/apiService.ts`.
- Keep HTTP concerns, serialization, error handling, authentication, and API-specific behavior outside presentation components.

### Web Streams

- Use Web Streams when the application genuinely needs streaming behavior: AI-generated responses, incremental responses, progress streams, long-running operations, Server-Sent Events, streaming logs, incremental data processing.
- Do not use streams simply because they are technically available. Do not turn ordinary request/response APIs into streaming APIs without a concrete requirement.
- Streaming code should correctly handle cancellation, errors, stream completion, reader cleanup, and decoding. Prefer the platform Web Streams APIs over a dependency when the native API is sufficient.

## Testing

- Test observable behavior rather than implementation details. Use the simplest appropriate testing layer. Add or update tests when behavior changes.
- **Vitest:** unit tests, utilities, business logic, API clients, hooks, and isolated modules.
- **Testing Library:** React component behavior. Tests interact with components the way users do. Prefer `getByRole()`, `getByLabelText()`, `getByText()` over implementation-specific selectors. Avoid testing internal component state or implementation details unless there is a specific reason.
- **Playwright:** browser-level behavior and end-to-end scenarios (complete user journeys, routing, browser behavior, frontend/backend integration, authentication flows, critical application workflows). Do not use it for tests that Vitest or Testing Library can adequately cover. Prefer accessible locators such as `getByRole()`, `getByLabel()`, `getByText()`.

## Validation and Definition of Done

- Before considering a task complete, run `npm run check`. It covers TypeScript, ESLint (including ESLint Stylistic formatting rules), unit/component tests, and the production build. ESLint warnings fail the check (`--max-warnings 0`).
- If browser behavior is affected, also run `npm run test:e2e`.
- A task is complete when the requested functionality is implemented, this validation passes, and documentation is updated when behavior or architecture changes.

## Dependencies

- Do not introduce dependencies unnecessarily. Before adding one, check whether the platform already provides the functionality, whether the project already has a suitable dependency, and whether it can reasonably be implemented without one. Consider the maintenance and security implications. Add it only when it provides meaningful value, and prefer native browser and Node.js APIs when they are sufficient.
- When dependencies change, update the lockfile through npm. Do not manually edit `package-lock.json`.
- Renovate manages dependency updates; allow CI to validate them. Do not manually upgrade unrelated dependencies while working on another task unless explicitly requested.
- Do not blindly bypass dependency or peer-dependency warnings. If an update creates compatibility problems, investigate the dependency tree before forcing installation. Do not use `npm install --force` or `npm install --legacy-peer-deps` to hide dependency problems unless explicitly instructed to do so.

## Formatting and ESLint

- Formatting is handled by ESLint using `@stylistic/eslint-plugin` (rules for arrow parentheses, brace style, indentation, TypeScript member delimiters, quotes, and semicolons). The repository's ESLint configuration is the source of truth. Do not introduce a second formatting system or formatting rules that conflict with it.
- Imports are sorted by `simple-import-sort` in groups: `node:` built-ins, packages, `@/` aliases, relative imports, styles.
- Prefix intentionally unused variables and arguments with `_`. Only `console.warn` and `console.error` are allowed (`no-console`).
- Format with ESLint autofix: `npm run format`. The configured editor should also apply Stylistic fixes on save. In formatting-only changes, do not manually reformat unrelated files.
- Do not disable ESLint rules globally to make a change pass. If a rule genuinely needs to be disabled, keep the scope as narrow as possible and document the reason when appropriate. Do not add unnecessary `eslint-disable` comments; prefer fixing the underlying issue.

## Environment Variables and Secrets

- Never commit secrets or place them in source code. Do not print secrets in logs, test output, or error messages.
- All app code runs in the browser. Never expose API secrets, database credentials, private keys, server-only environment variables, or internal credentials to it, including through Vite. Only environment variables explicitly intended for browser exposure should use the Vite `VITE_` prefix.
- Use `.env.example` to document required environment variables without including secret values.

## Git

- Do not rewrite Git history. Do not create commits unless explicitly requested.
- Do not commit generated files such as `dist/`, `coverage/`, `playwright-report/`, `test-results/`.
- Review the final diff before considering the task complete.
