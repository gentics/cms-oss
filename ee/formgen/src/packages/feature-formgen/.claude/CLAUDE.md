# AI & Agent Guidelines

This file provides guidance to all AI agents and LLM-assisted tools when working with code in this repository.

## Scope
- Turns a plugin's `schema` + `uiSchema` + `flows` + `data` into a multi-step form.
- Two backends, chosen by `pluginName`: `acta-nova` (AN) and `forms` (CMS forms).
- Paths are relative to this module's folder (the parent of `.claude/`). The workspace root `../../` is `ee/formgen/src/`.
- Workspace rules: `../../.claude/CLAUDE.md`. Core styles, i18n and the SDK: `../core/.claude/CLAUDE.md`.

## Entry point
- The package root exports nothing usable (`export *` drops the default export). Consumers deep-import files under `src/`. Grep `../../apps/` before moving or renaming them.
- `../core` imports this package's `src/types/preview` and the boolean element styles from `src/styles/`. Renaming those breaks core.
- If a caller passes `t` / `features`, `FormgenScreen` skips the i18n init / the features fetch. The web app passes neither.

## Layout (`src/`)
- `screens/`: `FormgenScreen` loads features (AN only) and i18n, then renders. `RendererScreen` does almost everything else (load, flows, validation, submit, overlays).
- `custom/`: the extension points. Maps from `reactClass` to flow screens and flow buttons, overlay types, element maps, and `pre*`/`post*` hooks (mostly empty; the captcha runs in `preSubmitForm`). How each one works is described in `../../../../../cms-oss-doc/src/main/source/formgen.textile`; read it before changing them.
- `components/elements/`: element types. `an/` is AN-only, `cms/` is CMS-forms-only, `shared/` serves both, `helper/` holds wrappers and bridges.
- `components/overlays/`: overlay content, opened through `src/utils/OverlayUtils.tsx`.
- `services/RestService.ts`: all backend calls. AN and CMS requests differ (headers, compression, query params); see the file. Methods marked `@ANOnly` are AN-only.
- `tests/ExampleService.test.ts` is a placeholder; there is no test runner (see workspace rules).
- `readme.md` lists `context/`, `hooks/` and `store/`; these directories don't exist.

## State
- `RendererScreen` keeps `formState` / `uiState` in `useState`, but code mutates them in place and then calls `updateState(...)`, which copies them to trigger a render. A mutation without `updateState` doesn't render.
- `updateState` also pushes to the read-only zustand mirror in `state/`. Don't treat the mirror as a source of truth.
- The flow comes with the form-load response (picked by `flowId`, else the first). Missing flows show the `LOAD_FORM` overlay.

## Data fetching
- `RestService` never throws for HTTP errors; check `success`.
- Before most POSTs, `cleanUp()` strips UI-only fields (`editable`, `possibleValues`, …; `visible` except for CMS). It is skipped for save, submit, triggerForm, csvupload and deleteFile. Don't rely on these fields server-side.
- The `Authorization` header is only sent in `__DEV__` and only with a non-empty `apiToken`.

## Element types
- Element maps per plugin are in `src/custom/ElementTypes.tsx`, keyed by the **lowercase** schema type (`ElementRenderer` lowercases before the lookup). The plugin map wins over the shared map. An unknown key is not rendered (only a `console.warn`).
- AN overrides the shared string/number/catalog elements, so the shared input and catalog elements effectively serve only CMS forms.
- Every element type must carry the statics `validate` and `getSummaryValue` (`IElementType`). They are called without an existence check. For `.web`/`.native` pairs, attach them in both files.
- To add a type: component in the right `src/components/elements/` subfolder, export it from `src/custom/ElementDefinitions.tsx`, add a lowercase key to the right map.
- `PluginNameEnum` is `@deprecated`: don't add logic based on plugin names. Prefer the element maps or features.

## Preview bridge (formgrid editor)
- Web only. Types and event names are in `src/types/preview.ts`, the senders in `src/components/elements/helper/formgridPreviewBridge.ts`, the receiver in `../../apps/andp-ma-formgen/App.tsx`.
- Messages go to the app's **own** window, not `parent`, and are routed by their `receiver` field.
- Keep the misspelled constant `PREVIEW_INTIALIZATION_EVENT_NAME`; the app imports it by that name.
- In preview there is no API load, and `previewInformation.features` is never read: the features come from the mock file, which yields nothing in a non-`__DEV__` bundle.

## Platform-specific files
- Platform pairs are `X.web.tsx` + `X.native.tsx` with an `index.tsx` barrel. Import the folder or `./X`, never the suffixed file. Keep both files' exports and statics in sync.
- `useMicroAppSDK()` returns `{}` on web, so its members are `undefined` there. On web, `formID` / `initializeData` come from `localStorage` instead of the mobile host.

## Style Architecture (`common` vs `custom`)
- **Folder separation:** Every package splits styles into:
  - `src/styles/common/`: Base theme and default component styles.
  - `src/styles/custom/`: Project-specific style overrides (mostly empty default objects).
- **Merge mechanism:** Components combine them with `mergeStyles(common, custom)` from `@andp/core/src/AndpUtils.tsx` (overrides flatten over common keys on property access).
- **Platform pairs:** The `.ts` style files exist in pairs (`*-style.web.ts` and `*-style.native.ts`); the `.css` files next to them are web-only. When adding a style key, **add it to both files**: a missing key silently gets no style on that platform (`mergeStyles` returns `{}`), and TypeScript only checks the `.native` file (`moduleSuffixes`).

## Mocks
- Edit mocks in `src/mock-files/`. `../../apps/andp-ma-formgen/public/mock-files/` is a generated copy (`copy-mock-files`). A new mock file also needs a `require` case in `src/utils/MockFormularUtils.tsx` for native.
- AN mock behaviour is keyed to hard-coded element ids in `MockFormularUtils` and `RestService`.
