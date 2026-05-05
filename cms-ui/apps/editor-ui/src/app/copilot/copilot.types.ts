/**
 * Type definitions for the Content Copilot feature.
 *
 * The Copilot is configured via a JSON file dropped into the customer's
 * UI configuration folder (`{ui-conf}/copilot.json`). The shape defined
 * here is the contract for that file.
 *
 * Adding new actions later is a pure configuration change — no UI rebuild
 * required — provided the action types remain compatible with what the
 * sidebar already knows how to render.
 */

/**
 * A per-language string lookup, e.g. `{ "de": "Zusammenfassen", "en": "Summarise" }`.
 *
 * Used wherever a configuration value must remain translatable but cannot
 * piggy-back on the bundled i18n keys (because the value lives in customer
 * config, outside the editor-ui build).
 */
export type I18nString = Record<string, string>;

/** Top-level shape of `copilot.json`. */
export interface CopilotConfig {
    /**
     * Master switch for the whole feature. When `false` (or the file is
     * missing/invalid) the toolbar button stays hidden and the sidebar
     * never mounts.
     */
    enabled: boolean;

    /**
     * List of actions that will appear inside the sidebar. Empty for the
     * initial UI scaffolding — kept here on the type level so editors get
     * autocompletion / type-checking the moment a customer adds entries.
     */
    actions: CopilotAction[];
}

/**
 * A single action exposed by the Copilot sidebar. Behaviour beyond
 * displaying the action (e.g. dispatching the prompt) is intentionally
 * not wired up yet — that is the next iteration after the UI scaffolding
 * has been signed off.
 */
export interface CopilotAction {
    /** Stable identifier, used as the Angular `trackBy` key. */
    id: string;

    /**
     * Per-language label rendered on the action card. Resolved against the
     * active UI language via the `gtxI18nObject` pipe.
     */
    labelI18n: I18nString;

    /** Optional Material-Symbol icon name shown next to the label. */
    icon?: string;

    /**
     * Optional per-language short description shown under the label.
     * Same resolution rules as `labelI18n`.
     */
    descriptionI18n?: I18nString;

    /**
     * Free-form prompt template. Reserved for the next iteration that
     * actually executes the action — kept on the type so JSON files
     * authored today are forward-compatible.
     */
    prompt?: string;
}

/** Default config used when no JSON file is present or parsing fails. */
export const DEFAULT_COPILOT_CONFIG: CopilotConfig = {
    enabled: false,
    actions: [],
};
