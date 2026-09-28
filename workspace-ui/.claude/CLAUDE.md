Runtime
Project Instructions

This repository is a React + TypeScript application using Vite, TanStack Query, a Node.js backend API, Web Streams, Vitest, Testing Library, Playwright, ESLint with ESLint Stylistic, npm, GitHub Actions, and Renovate.

Use npm as the package manager.

Do not change the package manager to pnpm, Yarn, Bun, or another tool unless explicitly requested.

The project is developed with AI assistance. Follow these instructions for every task.

Core Principles
Inspect before modifying

Before changing code:

Inspect the relevant files.

Understand the existing architecture and conventions.

Search for existing implementations that solve a similar problem.

Reuse existing patterns where appropriate.

Only then make changes.

Do not modify files based on assumptions about how the project works.

Preserve the existing architecture

Do not restructure the project unless the task requires it.

Do not introduce new architectural patterns merely because they are personally preferred.

When extending functionality, follow the existing structure and conventions.

If an architectural change appears necessary, explain why before making a broad change.

Prefer the smallest change

Make the smallest change that correctly solves the requested problem.

Avoid:

unrelated refactoring

unnecessary renaming

speculative abstractions

large-scale rewrites

formatting unrelated files

introducing dependencies for trivial functionality

A task should not become an excuse to "clean up" unrelated code.

Do Not Invent Things
Do not invent APIs

Never invent:

API endpoints

request fields

response fields

authentication mechanisms

environment variables

database schemas

library APIs

configuration options

undocumented behavior

If an API or behavior is unclear, inspect the repository and available documentation first.

If it still cannot be determined, state the uncertainty instead of guessing.

Do not fabricate results

Never claim that:

tests passed

lint passed

type checking passed

a build succeeded

an API works

a browser test passed

unless the relevant command was actually executed and succeeded.

If a command could not be run, explicitly say so.

Technology Guidelines
React

Use modern React patterns.

Prefer:

functional components

hooks

composition

small focused components

semantic HTML

Avoid unnecessary component abstractions.

Do not introduce global state when local state or TanStack Query is sufficient.

Do not put business logic directly into large UI components when it can reasonably live in a feature or domain module.

TypeScript

Use TypeScript strictly.

Prefer precise types over any.

Avoid any unless there is a clear and documented reason.

Prefer:

unknown

with proper narrowing over:

any

Do not use type assertions merely to silence TypeScript errors.

Understand why a type assertion is safe before using one.

API boundaries should have explicit types.

TanStack Query

Use TanStack Query for server state.

Server state includes data retrieved from or synchronized with backend APIs.

Prefer TanStack Query for:

fetching server data

caching

synchronization

mutations

invalidation

request state

server-side errors

Do not duplicate server state unnecessarily in React state.

Use stable and meaningful query keys.

For mutations, ensure affected queries are invalidated or updated appropriately after successful changes.

Do not manually recreate caching or request-state behavior that TanStack Query already provides.

API Access

Do not call fetch() directly from React components.

API communication should go through the project's API client or appropriate feature-level abstraction.

For example:

src/lib/api.ts

Components should consume hooks or feature-level functions rather than implementing HTTP requests themselves.

Keep:

HTTP concerns

serialization

error handling

authentication

API-specific behavior

outside presentation components.

Do not invent endpoints or API contracts.

Web Streams

Use Web Streams when the application genuinely needs streaming behavior.

Appropriate use cases include:

AI-generated responses

incremental responses

progress streams

long-running operations

Server-Sent Events

streaming logs

incremental data processing

Do not use streams simply because they are technically available.

Do not turn ordinary request/response APIs into streaming APIs without a concrete requirement.

Streaming code should correctly handle:

cancellation

errors

stream completion

reader cleanup

decoding

Prefer the platform Web Streams APIs rather than adding a dependency when the native API is sufficient.

Backend

Backend code belongs in the backend/server area of the project.

Do not move server-only functionality into browser code.

Never expose:

API secrets

database credentials

private keys

server-only environment variables

internal credentials

to the browser.

Only environment variables explicitly intended for browser exposure should use the Vite VITE\_ prefix.

Validate external input at backend boundaries.

Do not trust client-provided data.

Keep HTTP handlers focused. Move substantial business logic into appropriate modules rather than creating large request handlers.

Testing
Testing philosophy

Test observable behavior rather than implementation details.

Use the simplest appropriate testing layer.

Vitest

Use Vitest for:

unit tests

utilities

business logic

API clients

hooks

isolated modules

Testing Library

Use Testing Library for React component behavior.

Tests should interact with components the way users do.

Prefer:

getByRole()
getByLabelText()
getByText()

over implementation-specific selectors.

Avoid testing internal component state or implementation details unless there is a specific reason.

Playwright

Use Playwright for browser-level behavior and end-to-end scenarios.

Use it for:

complete user journeys

routing

browser behavior

frontend/backend integration

authentication flows

critical application workflows

Do not use Playwright for tests that can be adequately covered by Vitest or Testing Library.

Prefer accessible Playwright locators such as:

getByRole()
getByLabel()
getByText()

Validation

Before considering a task complete, run the relevant validation.

For normal changes, run:

npm run check

If browser behavior is affected, also run:

npm run test:e2e

The complete check should cover:

TypeScript

ESLint, including ESLint Stylistic formatting rules

unit/component tests

production build

Do not claim validation succeeded unless the commands actually ran.

If a test cannot be run because of the environment, explain the limitation.

Dependencies

Do not introduce dependencies unnecessarily.

Before adding a dependency:

Check whether the platform already provides the functionality.

Check whether the existing project already has a suitable dependency.

Check whether the functionality can reasonably be implemented without a dependency.

Consider the maintenance and security implications.

Add the dependency only when it provides meaningful value.

Prefer native browser and Node.js APIs when they are sufficient.

Use npm as the package manager.

Do not switch to pnpm, Yarn, Bun, or another package manager unless explicitly requested.

When dependencies change, update the appropriate lockfile through npm.

Do not manually edit package-lock.json.

Dependency Updates

Renovate manages dependency updates.

Do not manually upgrade unrelated dependencies while working on another task unless explicitly requested.

When Renovate creates a dependency update, allow CI to validate it.

Do not blindly bypass dependency or peer-dependency warnings.

If an update creates compatibility problems, investigate the dependency tree before forcing installation.

Do not use:

npm install --force

or:

npm install --legacy-peer-deps

as a way to hide dependency problems unless explicitly instructed to do so.

Formatting

Formatting is handled by ESLint using @stylistic/eslint-plugin.

Do not introduce a second formatting system.

Follow the formatting rules defined in the repository's ESLint configuration.

The repository's Stylistic configuration currently includes rules for:

arrow parentheses

brace style

indentation

TypeScript member delimiters

quotes

semicolons

The configured formatting style should be treated as the source of truth.

Use ESLint autofix to format files:

npm run format

ESLint should also apply Stylistic fixes when saving files in the configured editor.

When making a formatting-only change, do not manually reformat unrelated files.

Do not introduce formatting rules that conflict with the repository's existing ESLint Stylistic configuration.

ESLint

Follow the repository's ESLint configuration.

Formatting is provided by @stylistic/eslint-plugin.

Do not disable ESLint rules globally to make a change pass.

If an ESLint rule genuinely needs to be disabled, keep the scope as narrow as possible and document the reason when appropriate.

Do not add unnecessary eslint-disable comments.

Prefer fixing the underlying issue rather than suppressing a rule.

When formatting code manually, follow the existing ESLint Stylistic configuration rather than introducing personal formatting preferences.

Environment Variables and Secrets

Never commit secrets.

Never place secrets in source code.

Never expose server-only secrets through Vite.

Use:

.env.example

to document required environment variables without including secret values.

Do not print secrets in logs, test output, or error messages.

Git

Keep changes focused.

Do not modify unrelated files.

Do not rewrite Git history.

Do not create commits unless explicitly requested.

Do not commit generated files such as:

dist/
coverage/
playwright-report/
test-results/

Review the final diff before considering the task complete.

AI-Assisted Development

This repository is intentionally designed for AI-assisted development.

When working on a task:

Inspect before modifying.

Understand the existing implementation.

Search for existing patterns.

Make the smallest appropriate change.

Do not invent APIs or requirements.

Do not introduce unnecessary dependencies.

Preserve the existing architecture.

Add or update tests when behavior changes.

Run the relevant validation.

Report what was actually verified.

When requirements are ambiguous:

use existing project conventions where possible

do not invent behavior

ask for clarification when the ambiguity materially affects the implementation

Do not silently make major architectural decisions.

Definition of Done

A task is complete when:

The requested functionality is implemented.

Existing architecture is preserved.

No unnecessary dependencies were introduced.

TypeScript passes.

ESLint passes.

ESLint Stylistic formatting rules pass.

Relevant unit/component tests pass.

Relevant E2E tests pass when applicable.

The production build succeeds.

No secrets were introduced.

Documentation is updated when behavior or architecture changes.

The final response accurately reports what was actually tested.

Most importantly:

Never claim something was tested, verified, built, or deployed unless it actually was.
