# Workspace UI

## Local GenAIx API

The Workspace calls GenAIx only through the CMS proxy, at the same-origin path `/genaix/api/v1`. The proxy adds the installation token (`Authorization: Bearer sk_gnx_...`) and the user's `X-GCMS-Subject`, so the browser never holds either (`.claude/contracts/genaix-docs/09-integration-guide.md`, section 3).

In development the Vite dev server plays the proxy (`server.proxy` in `vite.config.ts`). It forwards `/genaix/api/v1/*` to `GENAIX_API_URL`, removes the browser's `Cookie`, and overwrites `Authorization` and `X-GCMS-Subject`.

1. Check out `git@git.gentics.com:psc/genaix/api-contract.git` next to `cmp`, so that it is at `../../../api-contract` from this folder.
2. Start the GenAIx mock (Python 3.11+; creates `.venv` on first run): `npm run mock:genaix`. It serves `http://localhost:8123/api/v1`, the port of the hand-out's docker compose stack (`.claude/contracts/genaix-docs/deploy-README.md`), so either one can be used.
3. Start the app in a second terminal: `npm run dev`.
4. Call `http://localhost:5173/genaix/api/v1/me` from the browser or with `curl`.

Configuration is read from `.env.local` (see `.env.example`). All three variables are optional, and their defaults target the mock:

| Variable | Default | Meaning |
| --- | --- | --- |
| `GENAIX_API_URL` | `http://localhost:8123/api/v1` | GenAIx base URL the proxy forwards to |
| `GENAIX_API_TOKEN` | `sk_gnx_mock` | Installation token (mock fixture) |
| `GENAIX_SUBJECT` | `sub_workspace_dev` | `X-GCMS-Subject` sent for every request |

These variables have no `VITE_` prefix, so they stay on the dev server and are never bundled into the client.

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
