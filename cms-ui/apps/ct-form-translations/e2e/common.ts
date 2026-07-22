/**
 * Common constants used across the form-translations e2e tests.
 *
 * Keep this file small — only put things that are referenced from MORE THAN ONE
 * spec file. Test-local constants belong in the spec file itself.
 */

/**
 * Default credentials for tests that don't need a custom user setup.
 *
 * Prefer creating a dedicated test user via the EntityImporter where possible
 * (see the integration-test guide). Use this admin only for smoke tests where
 * permissions aren't the thing under test.
 */
export const AUTH = {
    admin: {
        username: 'node',
        password: 'cms_integrationTest#node',
    },
};

/**
 * Sentinel id of the global translations scope. Matches `GLOBAL_SCOPE_ID` in
 * `src/app/models/translations.model.ts`.
 */
export const GLOBAL_SCOPE_ID = 'global';

/**
 * A handful of well-known placeholder keys we expect to be present in the
 * default global translations. These are referenced from multiple specs as
 * targets for "edit this row" actions and should be safe to rely on in a
 * minimal test fixture.
 */
export const KNOWN_PLACEHOLDERS = {
    SUBMIT_BUTTON: 'form_submit_button',
    CANCEL_BUTTON: 'form_cancel_button',
    LOADING:       'form_loading',
} as const;
