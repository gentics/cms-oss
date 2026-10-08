# Workspace UI

## Local GenAIx API

The Workspace calls GenAIx only through the CMS proxy, at the same-origin path `/genaix/api/v1`. The proxy adds the installation token (`Authorization: Bearer sk_gnx_...`) and the user's `X-GCMS-Subject`, so the browser never holds either (`.claude/contracts/genaix-docs/09-integration-guide.md`, section 3).

In development the Vite dev server plays the proxy (`server.proxy` in `vite.config.ts`). It forwards `/genaix/api/v1/*` to `GENAIX_API_URL`, removes the browser's `Cookie`, and overwrites `Authorization` and `X-GCMS-Subject`.

1. Check out `git@git.gentics.com:psc/genaix/api-contract.git` next to `cmp`, so that it is at `../../../api-contract` from this folder.
2. Start the GenAIx mock (Python 3.11+; creates `.venv` on first run): `npm run mock:genaix`. It serves `http://localhost:8123/api/v1`, the port of the hand-out's docker compose stack (`.claude/contracts/genaix-docs/deploy-README.md`), so either one can be used.
3. Start the app in a second terminal: `npm run dev`.
4. Call `http://localhost:5173/genaix/api/v1/me` from the browser or with `curl`.

Configuration is read from `.env.local` (see `.env.example`); `.env.example` itself is not loaded. All variables are optional. The GenAIx defaults target the mock:

| Variable | Default | Meaning |
| --- | --- | --- |
| `GENAIX_API_URL` | `http://localhost:8123/api/v1` | GenAIx base URL the proxy forwards to |
| `GENAIX_API_TOKEN` | `sk_gnx_mock` | Installation token (mock fixture) |
| `GENAIX_SUBJECT` | `sub_workspace_dev` | `X-GCMS-Subject` sent for every request |
| `CMS_PROXY_TARGET` | none | CMS that `/rest` is forwarded to, e.g. `https://cms.example.com`. Without it the dev server answers the logged-in user (`GET /rest/user/me`, a fixed dev user, so there is no login) and the CMS API token request (`POST /rest/admin/token`, a test token) itself, so sessions start against the GenAIx mock; every other CMS call (e.g. the @-menu search) has no CMS |

These variables have no `VITE_` prefix, so they stay on the dev server and are never bundled into the client.

In production the UI is served from the CMS host, so `/rest` is same-origin and needs no proxy. In development the proxy keeps the CMS cookies on `localhost` (`cookieDomainRewrite`); the CMS calls are authenticated by the CMS session cookie, so they need a session on that CMS.

## Login

The app is shown only with a CMS session, as the editor and admin UI do (`src/components/LoginGate/`). It asks the CMS for the user of the session cookie (`GET /rest/user/me`); a session from the editor or admin UI on the same host counts, since the cookie is set for `/`. Without one:

- If the CMS has Keycloak (`GET /rest/keycloak`, the `cms-keycloak` module with the feature `keycloak`), the app logs in through Keycloak with `keycloak-js` and turns its token into a CMS session (`GET /rest/auth/ssologin`, `src/helper/keycloak/keycloak.ts`). Without `keycloak.show_sso_button` it redirects to the Keycloak login at once; with it the login form offers "Log in with SSO". The Keycloak client must accept the workspace URL (e.g. `https://cms.example.com/workspace/*`) as a redirect URI. `?skip-sso` in the URL skips Keycloak, as in the editor.
- Otherwise the login form logs in with user name and password (`POST /rest/auth/login`).

The logout in the topbar ends the CMS session (`POST /rest/auth/logout`) and, whenever the CMS has Keycloak and `?skip-sso` is not set, the Keycloak session too, so Keycloak does not log the user straight back in; then the page starts fresh. If the Keycloak logout fails, the CMS session is gone already: the login form appears with a notification. With `CMS_PROXY_TARGET` the login runs against that CMS; without it the dev server's fixed dev user is always logged in.

The client side has one variable of its own, `VITE_GENAIX_API_BASE` (default `/genaix/api/v1`): the same-origin proxy path that `src/services/apiService/apiService.ts` sends every GenAIx request to. It is bundled into the client, so it holds a path and never a secret. The dev proxy above only serves `/genaix/api/v1`, so a different value in development needs a matching `server.proxy` entry.

### API types

`src/services/apiService/genaix/schema.d.ts` is generated from the contract `.claude/contracts/openapi.yaml`. Regenerate it with `npm run generate:api` whenever the contract changes. Import types through the aliases in `src/services/apiService/genaix/types.ts`.

`openapi-typescript` declares a peer dependency on TypeScript 5. The `overrides` entry in `package.json` points it at this project's TypeScript 6. With 7.13.0 this produced byte-identical output to a TypeScript 5.9.3 run. Remove the override once `openapi-typescript` supports TypeScript 6.

## UI components

Reusable components live in `src/components/ui/`: shadcn/ui on [Base UI](https://base-ui.com) (`@base-ui/react`), styled with Tailwind using only the design tokens from `.claude/rules/design.md` (defined in `src/index.css`), with Lucide icons. Tailwind is limited to this folder; ESLint reports Tailwind classes anywhere else. Wrap the app in `UiProvider` (`@/components/ui/provider`) before using them.

The component catalogue is a development page of its own, separate from the app: `npm run catalogue` starts it at `http://localhost:5174` (`vite.catalogue.config.ts`). `npm run dev` and `npm run build` do not include it. It shows every component in its states, with a light/dark/system theme switch. The theme follows the OS unless `data-theme="light"` or `data-theme="dark"` is set on `<html>`.

## Serving the build from the CMS

The CMS serves this app at `/workspace/`, next to `/editor/` and `/admin/`. The trailing `/` is part of the URL: the asset URLs are relative to it.

- `npm run build` writes `dist/`. The Maven module in this folder (`pom.xml`, `assembly/workspace-ui.xml`) only packages that `dist/` into the `workspace-ui` zip under `workspace/`; it does not run npm. Build `dist/` first, as the "Build UI" stage of the `Jenkinsfile` does.
- `cms-oss-server` unpacks the zip into its `webroot`, and `OSSRunner` maps `/workspace/*` to it.

The build works under any path, without rebuilding for it:

- Asset URLs are relative (`base: './'` in `vite.config.ts`). In `index.html`, files from `public/` keep a leading `/` (`/favicon.png`); Vite rewrites them to relative URLs in the build. In code, reference them through `import.meta.env.BASE_URL`.
- Routing uses hash history (`src/router.tsx`), like the CMS's own UIs: a session is at `<app path>/#/sessions/<id>`, so the CMS only ever has to serve `index.html` at the app's own path.

`/rest` and `/genaix/api/v1` stay absolute: both are same-origin paths on the CMS host. The CMS REST calls send no `sid`, only the browser's CMS session cookie; the CMS accepts that, and offers `POST /rest/admin/token`, from the `hotfix-6.6.x` line of `cms-oss` on.
