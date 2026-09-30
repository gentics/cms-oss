# React + TypeScript + Vite

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

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
]);
```

You can also install [eslint-plugin-react-x](https://npmx.dev/package/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://npmx.dev/package/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x';
import reactDom from 'eslint-plugin-react-dom';

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
]);
```
