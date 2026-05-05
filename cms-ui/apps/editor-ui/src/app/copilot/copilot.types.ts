/**
 * Type definitions for the Content Copilot feature.
 *
 * The Copilot is configured via a YAML file dropped into the customer's
 * UI configuration folder (`{ui-conf}/config/copilot.yml`). The shape
 * defined here is the contract for that file. Adding new actions later
 * is therefore a pure configuration change — no UI rebuild required —
 * provided the action types remain compatible with what the sidebar
 * already knows how to render.
 */

/** Top-level shape of `copilot.yml`. */
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
     * Translation key OR literal label. Convention: if it contains a
     * dot it's treated as an i18n key, otherwise rendered as-is. Keeps
     * configuration files concise without precluding localisation.
     */
    label: string;

    /** Optional Material-icon name shown next to the label. */
    icon?: string;

    /**
     * Short description shown under the label. Same dot-vs-literal
     * heuristic as `label`.
     */
    description?: string;

    /**
     * Free-form prompt template. Reserved for the next iteration that
     * actually executes the action — kept on the type so YAML files
     * authored today are forward-compatible.
     */
    prompt?: string;
}

/** Default config used when no YAML file is present or parsing fails. */
export const DEFAULT_COPILOT_CONFIG: CopilotConfig = {
    enabled: false,
    actions: [],
};
