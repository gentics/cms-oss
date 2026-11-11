# AI & Agent Guidelines

This file provides guidance to all AI agents and LLM-assisted tools when working with code in this repository.

## Scope
- FormGen is the form renderer of Gentics CMS (Expo / React Native web micro-app). It is **not** ANDP-specific.
- It serves two backends, chosen by `pluginName`: `forms` (CMS forms, Mesh forms plugin) and `acta-nova` (ANDP). A change must work for both.
- npm-workspaces root with one app and two packages: `apps/andp-ma-formgen` (web bootstrap), `packages/feature-formgen` (renderer), `packages/core` (shared base). Module details are in each module's `.claude/CLAUDE.md`. Paths are relative to the folder that contains `.claude/`.
- The names `andp-*`, `@andp/*` and `andp-cpa` are historical. Don't rename them: consumers deep-import by these paths, and the ANDP copy job uses the folder names.

## Two consumers
- **CMS:** the Maven module `ee/formgen` builds the app and ships it in the `ee/cms-server` webroot under `/formgen`.
- **ANDP:** the andp Jenkins job (`copyFormgenSourcesToAndpProject()`, psc/andp `ci/Jenkinsfile`) replaces `packages/core`, `packages/feature-formgen`, `apps/andp-ma-formgen`, `package.json`, `package-lock.json` and the `tsconfig*.json` in andp-project (`psc/andp-project`) `andp-cpa/` with this folder's state. Changes made only there are lost.


## Shared root files / dependency updates
- `package.json` and `package-lock.json` are copies of the andp-cpa workspace root files from andp-project, which has more apps. They therefore contain scripts and lock entries for andp-cpa-only apps (`andp-mobile`, `start:ma-bc`, ...). Don't remove them.
- Update dependencies only with the skill `/npm-deps-update` (Skill inside ANDP Stack git repo): it runs the update in andp-project `andp-cpa/` and copies the resulting `package.json` / `package-lock.json` back here. Don't change dependencies here directly.

## Backends
- Runtime calls and translations go to the Mesh plugin (`/api/<pluginName>/...`), not to the CMS. The CMS is only called by the formgrid preview (`/rest/form/types/<formType>/translations/<lng>`).

## Translations
Add new or changed FormGen keys, with `de` and `en`:
- CMS forms: config `forms.formGenTranslations` (global) or `formGenTranslations` of a form type under `forms.types`.
- ANDP: andp-project `cms/conf/extraconfig/forms-translations.yml`, under `forms.formGenTranslations`.
- Local dev/mock mode reads `local_dev_translations/{de,en}/` (see `packages/core/.claude/CLAUDE.md`).

## Commands
- Run npm scripts from this folder.

## Tests
- There are no working tests: `npm test` calls jest, which is neither installed nor configured, and Maven runs no frontend tests.
- Verify by build and a manual check in mock mode for both backends, and say in the report that no tests ran.

## Documentation
- `../../../cms-oss-doc/src/main/source/formgen.textile`: customizing guide (elements, hooks, buttons, overlays, screens, styling, fonts). Read it before changing `packages/feature-formgen/src/custom/` or `src/styles/custom/`; update it when you change those contracts.
- `../../../cms-oss-doc/src/main/source/feature_forms.textile`: forms configuration (`forms.yml`) and Forms Translation. Update it when you add a control, a `formgenSettings` option, a `reactClass` screen/button or translation behaviour.

## Branches
- In cms-oss, FormGen (`ee/formgen`) exists only from CMS 9.0 (`dev`); `master` and the `hotfix-6.x` branches have no `ee/formgen/`. For CMS 6.5/6.6 it lives in psc/cms `formgen/` (from `hotfix-6.5.x`).
