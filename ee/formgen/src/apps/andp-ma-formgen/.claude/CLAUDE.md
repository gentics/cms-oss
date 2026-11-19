# andp-ma-formgen (FormGen Web app)

## Scope
- Thin bootstrap: reads the host page (or the preview data), picks the endpoint and renders one `FormgenScreen` from `@andp/feature-formgen`. No navigation and no providers; flow steps are switched inside the package.
- Form logic: `../../packages/feature-formgen/.claude/CLAUDE.md`. Workspace rules: `../../.claude/CLAUDE.md`. Fonts, i18n, MicroApp SDK: `../../packages/core/.claude/CLAUDE.md`.
- Paths are relative to this module's folder (the parent of `.claude/`). The workspace root `../../` is `ee/formgen/src/`.

## Start / build
- Start from the workspace root: `npm run start:formgen`, `start:dev:formgen` or `start:mock:formgen`. Only these also copy the pdf worker (`public/pdf.worker.min.mjs`, gitignored). This app's own `start` / `web` copy only the mock files.
- `app.config.js` maps `DEV_MODE` / `MOCK_FORM` to `Constants.manifest.extra.devMode` / `mockForm`, and `APP_BASE_URL` to `experiments.baseUrl`.
- `npm run build` is not plain `expo export`: `prebuild` copies the mock files, `postbuild` copies `pdf.worker.min.mjs` into `dist/_expo/static/js/web/`.
- cms-oss: `ee/formgen/pom.xml` runs `npm ci` in `ee/formgen/src/` and `npm run build` here with `APP_BASE_URL=/formgen`. `ee/formgen/assembly/formgen.xml` zips `dist/` under `/formgen`; `ee/cms-server/pom.xml` unpacks it into its `webroot`.
- `localDeployment.sh` is a personal script with a hard-coded target path. No build uses it.
- `metro.config.js`: set a new custom `resolveRequest` before the `withDevTranslationsStub` wrapper, which chains the resolver already set.

## App.tsx / index.ts
- Endpoint: `formgen.config.json` `meshEndpoint` when `DEV_MODE` or `MOCK_FORM`, otherwise `window.location.origin`.
- Host-page contract: `initFromDocumentElements()` in `App.tsx` (`#root` data attributes, `egov-template--*` elements) plus the query param `businessObjectId`. `index.ts` reads `data-static-serve-path` as font base (default `/static/cpa`). The counterpart is the CMS construct `andp_forminclude`: change ids or attributes on both sides.
- `translationUrl` is passed to `FormgenScreen`, but nothing in `../../packages/` reads it. Translations come from `initI18nForms`.
- `apiToken` is always `''`, so this app never sends an `Authorization` header.
- `<Text>Fehler</Text>` is also the first-render placeholder until `useEffect` has set language and endpoint. It does not by itself mean an error.

## Formgrid preview
- Active with `?preview=true` (dynamic, postMessage) or `window.formPreviewData` (static). Protocol: package CLAUDE.md.
- Effects: `MOCK_FORM` is forced off, `isLoggedIn` is false, the host-page contract is skipped, the font base is `/formgen`. Static preview posts `'formgen.preview.resize'` whenever `document.body` resizes.

## Mock mode (`MOCK_FORM`)
- Needs a dev bundle: the mock helpers return `null` / `{}` when `__DEV__` is false.
- Edit mocks in the package, not in `public/mock-files/` (generated copy). Details: package CLAUDE.md.
