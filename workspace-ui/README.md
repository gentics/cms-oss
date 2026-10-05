# Workspace UI

## Local GenAIx API

The Workspace calls GenAIx only through the CMS proxy, at the same-origin path `/genaix/api/v1`. The proxy adds the installation token (`Authorization: Bearer sk_gnx_...`) and the user's `X-GCMS-Subject`, so the browser never holds either (`.claude/contracts/genaix-docs/09-integration-guide.md`, section 3).

In development the Vite dev server plays the proxy (`server.proxy` in `vite.config.ts`). It forwards `/genaix/api/v1/*` to `GENAIX_API_URL`, removes the browser's `Cookie`, and overwrites `Authorization` and `X-GCMS-Subject`.

1. Check out `git@git.gentics.com:psc/genaix/api-contract.git` next to `cmp`, so that it is at `../../../api-contract` from this folder.
2. Start the GenAIx mock (Python 3.11+; creates `.venv` on first run): `npm run mock:genaix`. It serves `http://localhost:8080/api/v1`.
3. Start the app in a second terminal: `npm run dev`.
4. Call `http://localhost:5173/genaix/api/v1/me` from the browser or with `curl`.

Configuration is read from `.env.local` (see `.env.example`). All three variables are optional, and their defaults target the mock:

| Variable | Default | Meaning |
| --- | --- | --- |
| `GENAIX_API_URL` | `http://localhost:8080/api/v1` | GenAIx base URL the proxy forwards to |
| `GENAIX_API_TOKEN` | `sk_gnx_mock` | Installation token (mock fixture) |
| `GENAIX_SUBJECT` | `sub_workspace_dev` | `X-GCMS-Subject` sent for every request |

These variables have no `VITE_` prefix, so they stay on the dev server and are never bundled into the client.

### API types

`src/services/apiService/genaix/schema.d.ts` is generated from the contract `.claude/contracts/openapi.yaml`. Regenerate it with `npm run generate:api` whenever the contract changes. Import types through the aliases in `src/services/apiService/genaix/types.ts`.

`openapi-typescript` declares a peer dependency on TypeScript 5. The `overrides` entry in `package.json` points it at this project's TypeScript 6. With 7.13.0 this produced byte-identical output to a TypeScript 5.9.3 run. Remove the override once `openapi-typescript` supports TypeScript 6.
