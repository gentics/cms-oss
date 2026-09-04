/**
 * View-side concepts for the form-translations tool. Wire models
 * (DTOs / Response shapes) live in `@gentics/cms-models`.
 */

/** Sentinel id of the global translations scope. */
export const GLOBAL_SCOPE_ID = 'global';
export type ScopeId = typeof GLOBAL_SCOPE_ID | string;

/**
 * A scope tab in the UI. `Global` is always present; the remaining scopes are
 * derived from the CMS form-type configurations.
 */
export interface Scope {
    /** Either `'global'` or a form-type key (e.g. `'andp'`). */
    id: ScopeId;
    /** Label shown in the tab strip. */
    label: string;
    /** Description shown in the scope card. */
    description: string;
    /** `true` for `'global'`, `false` for form-type scopes. */
    isGlobal: boolean;
}

/** UI-side filter mode for the placeholder list. */
export type FilterMode = 'all' | 'incomplete';
