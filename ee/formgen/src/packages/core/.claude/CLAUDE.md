# AI & Agent Guidelines

This file provides guidance to all AI agents and LLM-assisted tools when working with code in this repository.

## Scope
`@andp/core` is the shared base for every `feature-*` package and every app in the workspace: theme, styles, fonts, icons, base components, i18n, the axios client and the MicroApp SDK. Paths are relative to this module's folder (the parent of `.claude/`).

## cms-oss vs andp-project
This folder exists in two repos. Check which one you are in: the workspace root `../../` is `ee/formgen/src/` in cms-oss and `andp-cpa/` in andp-project. Workspace rules: `../../.claude/CLAUDE.md`.
- **cms-oss** (the source):
  - Only FormGen uses core (`../feature-formgen`, `../../apps/andp-ma-formgen`). `../feature-formgen/src/screens/essentials/ButtonProvider.tsx` imports core via a relative path; include it in the grep.
  - The andp-cpa consumers are not visible here. A change can break them without any sign in cms-oss, so keep core changes backward-compatible and flag them in the report.
- **andp-project (`andp-cpa/`)**:
  - Core is also used by the other apps in `../../apps/` and the other `feature-*` packages in `../../packages/`. Grep all of them before changing or moving a file.
  - Some feature packages import core via relative paths (`../../../core/src/services/AxiosClient` in the `RestService` of feature-services/-tasks/-messages/-business-cases, `../../../core/src/styles/theme` in `feature-messages`). Include these in the grep.

## Entry point / exports
- `package.json` `main` is `src/index.ts`. That file is only `export * from '../src/'`, which points back at itself, so nothing is exported from the package root.
- Consumers deep-import file paths instead, e.g. `@andp/core/src/AndpUtils`, `@andp/core/src/components/AndpText`, `@andp/core/src/styles/theme`, `@andp/core/src/sdk/MicroAppSDK`, `@andp/core/models/Message`. Renaming or moving any file under `src/` or `models/` breaks those imports across `../../apps/` and `../../packages/`, so grep for the path first.
- `models/` (`Message`, `Task`, `FileData`, `MetaInfoData`) sits **outside** `src/`. It is imported as `@andp/core/models/...`, and from inside core as `../../models/...`.

## Platform-specific files
- `tsconfig.json` sets `moduleSuffixes: [".native", ".web", ""]`. Metro picks the platform file the same way. Import without the suffix (`../styles/theme`, `../styles/common/common-style`): there is no plain `theme.ts` or `common-style.ts`.
- Pairs: `src/styles/theme.{web,native}.ts`, `src/styles/common/common-style.{web,native}.ts`, `src/styles/custom/custom-style.{web,native}.ts`. Both files of a pair export the same names. Add or rename an export in both, or one platform fails to resolve it.

## Styles: common vs custom
- `src/styles/common/` holds the base styles. `src/styles/custom/` holds the per-project overrides, mostly empty objects with the same keys.
- Components combine the two with `mergeStyles(common, custom)` from `src/AndpUtils.tsx`, e.g. `src/components/LoadingComponents.tsx`. It returns a Proxy that flattens the base and the override per key on access, after dropping `null`/`undefined` values. The Proxy wraps `common`, so enumerating it (`Object.keys`) only shows `common`'s keys.
- Feature packages split their own `src/styles/` the same way into `common/` and `custom/` .
- Design tokens: `src/styles/variables.ts` holds the raw colours. `src/styles/theme.*.ts` maps them onto `ITheme` and the per-element themes; the interfaces are in `src/types/interfaces.ts`.

## Fonts
- `src/styles/common/font-config.ts` (Inter, `.ttf` files in `src/styles/common/fonts/`) and `src/styles/custom/font-config.ts` (empty by default) both export `fontFamilies`.
- The micro-apps load `custom` **instead of** `common` as soon as `custom` has any entry, so the two sets are never merged (`../../apps/andp-ma-*/App.tsx`, e.g. `../../apps/andp-ma-formgen/App.tsx` and `index.ts`).
- On web prod builds the micro-app `index.ts` (e.g. `../../apps/andp-ma-formgen/index.ts`) writes `@font-face` rules with the hard-coded path `.../assets/__packages/core/src/styles/<common|custom>/fonts/`. Moving or renaming the font folders breaks web fonts.

## Icons
- `src/components/AndpIcon.tsx` imports each SVG from `src/assets/icons/` and lists it in `iconSet`. `IconName` is derived from that map.
- To add an icon: add the SVG, import it, add a key. SVG imports work through `react-native-svg-transformer` in each app's `metro.config.js`.

## i18n and devTranslations
- `src/services/i18n.tsx`: `initI18nForms` (FormGen) and `initI18nMicroApps` (no caller in cms-oss). Initialised once (`i18n.isInitialized`); on web `isDev` is replaced by `Constants.manifest.extra.devMode`.
- Modes: preview loads from the CMS (`/rest/form/types/<formType>/translations/<lng>` with `sid`) and falls back to local de/en; dev uses only local translations; prod only the http backend.
- Local translations: `devTranslations.ts` imports `../../local_dev_translations/{de,en}/`. `../../metro.dev-translations.js` swaps it for `devTranslations.stub.ts` when `NODE_ENV=production` or the folder is missing. Keep both exporting the same names.
- `i18n.tsx` imports `PreviewInformation` from `feature-formgen`, a known violation of andp-project's dependency rule.

## MicroApp SDK
- `src/sdk/MicroAppSDK.tsx` defines the contract between host and micro-apps: navigation, auth, translation, shared functions/props, alert, toast, error, `config`, `devMode`. Its only provider implementation is `../../apps/andp-mobile/src/sdk/MicroAppSDKMobileProvider.tsx` (andp-project only). No app in cms-oss renders a provider.
- On web there is no provider: `useMicroAppSDK()` returns `{}` instead of throwing, so every API from it (and from `useTranslationApi`/`useAlertApi`/...) is `undefined` there. Guard before calling.
- Adding a required member to `MicroAppSDKValue` / `MicroAppSDKProviderProps` requires updating the mobile provider (andp-project only).

## HTTP
- `src/services/AxiosClient.ts` (`createAxiosClient(url, useInterceptors?)`) sets `withCredentials: true`. With `useInterceptors` on it logs full request/response headers and bodies to the console, so keep it off outside local debugging.

## Documentation
- `documentation.md`: prop tables and examples for `AndpText`, `AndpInputElement`, `AndpIcon`, `DropDownMenu` (screenshots in `doc_images/`). Update it when you change those props.
- `readme.md`: package overview, deep import examples, and package directory layout.

