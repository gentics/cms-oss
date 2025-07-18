// Extra Playwright Types which aren't exposed or are inlined

export interface ClickOptions {
    /**
     * Defaults to `left`.
     */
    button?: 'left'|'right'|'middle';

    /**
     * defaults to 1. See [UIEvent.detail].
     */
    clickCount?: number;

    /**
     * Time to wait between `mousedown` and `mouseup` in milliseconds. Defaults to 0.
     */
    delay?: number;

    /**
     * Whether to bypass the [actionability](https://playwright.dev/docs/actionability) checks. Defaults to `false`.
     */
    force?: boolean;

    /**
     * Modifier keys to press. Ensures that only these modifiers are pressed during the operation, and then restores
     * current modifiers back. If not specified, currently pressed modifiers are used. "ControlOrMeta" resolves to
     * "Control" on Windows and Linux and to "Meta" on macOS.
     */
    modifiers?: Array<'Alt'|'Control'|'ControlOrMeta'|'Meta'|'Shift'>;

    /**
     * Actions that initiate navigations are waiting for these navigations to happen and for pages to start loading. You
     * can opt out of waiting via setting this flag. You would only need this option in the exceptional cases such as
     * navigating to inaccessible pages. Defaults to `false`.
     * @deprecated This option will default to `true` in the future.
     */
    noWaitAfter?: boolean;

    /**
     * A point to use relative to the top-left corner of element padding box. If not specified, uses some visible point of
     * the element.
     */
    position?: {
        x: number;

        y: number;
    };

    /**
     * Maximum time in milliseconds. Defaults to `0` - no timeout. The default value can be changed via `actionTimeout`
     * option in the config, or by using the
     * [browserContext.setDefaultTimeout(timeout)](https://playwright.dev/docs/api/class-browsercontext#browser-context-set-default-timeout)
     * or [page.setDefaultTimeout(timeout)](https://playwright.dev/docs/api/class-page#page-set-default-timeout) methods.
     */
    timeout?: number;

    /**
     * When set, this method only performs the [actionability](https://playwright.dev/docs/actionability) checks and skips the action. Defaults
     * to `false`. Useful to wait until the element is ready for the action without performing it. Note that keyboard
     * `modifiers` will be pressed regardless of `trial` to allow testing elements which are only visible when those keys
     * are pressed.
     */
    trial?: boolean;
}
