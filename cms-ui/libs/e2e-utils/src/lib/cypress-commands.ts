import { resolveFixtures } from './binaries';
import { createRange, isJQueryElement, updateAlohaRange } from './utils';

/**
 * Override of the original `as` query, and the `intercept` function.
 * This is done to allow the prefix `@` to be provided/ignored, as we use constants for our aliases.
 *
 * Here we can use the constant `ALIAS_FOO` for both setting and accessing the alias, instead of having to either
 * cut it off or add the `@` before every `cy.get` call, which is just very verbose and unintuitive.
 *
 * @example ```ts
 * const ALIAS_FOO = '@test';
 * const ALIAS_BAR = '@req';
 *
 * cy.get('.something .foo-bar').as(ALIAS_FOO);
 * cy.get(ALIAS_FOO).then($elem => {
 *      // ...
 * });
 *
 * cy.intercept('/some/path', req => {
 *     req.alias = ALIAS_FOO;
 * });
 * cy.wait(ALIAS_FOO).then(intercept => {
 *      // ...
 * });
 *```
 */
export function setupAliasOverrides(): void {
    Cypress.Commands.overwriteQuery('as', function (originalFn, alias, options) {
        if (alias.startsWith('@')) {
            alias = alias.substring(1);
        }

        const fn = originalFn.call(this, alias, options);

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        return subject => fn(subject);
    });

    /*
     * Same as the `as` override above, but for `intercept` calls, where the alias is applied in the matcher.
     * This has to be patched here as well, as for some reason, using `cy.intercept(...).as()` with our patched
     * function, doesn't actually apply the alias for whatever reason.
     */
    /* eslint-disable @typescript-eslint/no-unsafe-call */
    Cypress.Commands.overwrite('intercept', (originalFn , urlOrMethodOrRouteMatcher, routeHandlerOrMatcher, routeHandler) => {
        const wrapHandler = (handlerFn: any) => {
            const handleAlias = (handledRequest: any) => {
                if (typeof handledRequest?.alias === 'string' && handledRequest.alias.startsWith('@')) {
                    handledRequest.alias = handledRequest.alias.substring(1);
                }
                return handledRequest;
            };
            return (requestToHandle: any) => {
                const handledRequest = handlerFn(requestToHandle);

                // Hacky prototype check, as instanceof doesn't work across multiple processes
                if (
                    handledRequest != null
                    && typeof handledRequest === 'object'
                    && Object.getPrototypeOf(handledRequest).constructor.name === 'Promise'
                ) {
                    return handledRequest.then((data: any) => {
                        handleAlias(requestToHandle);
                        return data;
                    });
                }

                handleAlias(requestToHandle);

                return handledRequest;
            }
        };

        if (typeof routeHandler === 'function') {
            routeHandler = wrapHandler(routeHandler);
        } else if (typeof routeHandlerOrMatcher === 'function') {
            routeHandlerOrMatcher = wrapHandler(routeHandlerOrMatcher) as any;
        }

        return originalFn(urlOrMethodOrRouteMatcher, routeHandlerOrMatcher, routeHandler);
    });
    /* eslint-enable @typescript-eslint/no-unsafe-call */
}

const GTX_BUTTONS: string[] = [
    'gtx-button',
    ...[
        '', // will be correct in assembly
        'context',
        'context-toggle',
        'toggle',
    ].map(name => `gtx-aloha-${name}${name !== '' ? '-' : ''}button-renderer`),
];

const GTX_SPLIT_BUTTONS: string[] = [
    'gtx-split-button',
    'gtx-aloha-split-button-renderer',
    'gtx-aloha-toggle-split-button-renderer',
    'gtx-aloha-attribute-button-renderer',
    'gtx-aloha-attribute-toggle-button-renderer',
];

/**
 * Helper function to get the actual clickable element of a element.
 * This is done for custom button components or similar, where the tests shouldn't have
 * to learn how the component works.
 * Makes it a lot easier to use this way and doesn't require special knowledge of the underlying layout
 * of each clickable element.
 *
 * @param $subject The current element that should be clicked.
 * @param options Options to find the clickable element.
 * @returns If it's a special gtx component (gtx-button, gtx-aloha-button, ...), which aren't inheriently
 * clickable, then the actual clickable element is returned. Otherwise the `$subject`.
 */
function resolveClickable($subject: JQuery<HTMLElement>, options?: ClickableOptions): JQuery<HTMLElement> {
    if (!isJQueryElement($subject)) {
        return $subject;
    }

    const action = options?.action ?? 'primary';

    // Split buttons have to be handled differently
    if ($subject.is(GTX_SPLIT_BUTTONS.join(','))) {
        if (action === 'secondary') {
            return $subject.find([
                '> .split-button-wrapper .more-trigger button[data-action="primary"]',
                '.gtx-editor-split-button .split-button-secondary',
                '.gtx-editor-toggle-split-button .split-button-secondary',
            ].join(','));
        } else {
            return $subject.find([
                '> .split-button-wrapper > .primary-button button[data-action="primary"]',
                '.gtx-editor-split-button .split-button-main',
                '.gtx-editor-toggle-split-button .split-button-main',
            ].join(','));
        }
    } else if ($subject.is(GTX_BUTTONS.join(','))) {
        return $subject.find(`button[data-action="${action}"]`);
    }

    return $subject;
}

/**
 * This function will register all commands from the `commands` defintion.
 * These commands are common commands which are shared between all app e2e tests.
 */
export function registerCommonCommands(): void {
    Cypress.Commands.add('muteXHR', () => {
        // Disable logging of XHR/Fetch requests, since they just spam everything
        return cy.intercept({ resourceType: /xhr|fetch/ }, { log: false });
    });

    Cypress.Commands.add('loadBinaries', (files, options) => {
        return resolveFixtures(files, options);
    });

    Cypress.Commands.add('openContext', { prevSubject: 'element' }, (subject, options) => {
        const ATTR_ID = 'data-context-id';
        const id = subject.attr(ATTR_ID);
        if (!id) {
            Cypress.log({
                $el: subject,
                name: 'open context',
                message: `Cannot open element context, since attribute "${ATTR_ID}" is missing!`,
                end: true,
                type: 'child',
            });

            return cy.wrap(null, { log: false }).end();
        }

        cy.wrap(subject, { log: false })
            .find('gtx-dropdown-trigger [data-context-trigger]', { log: false })
            .click({ log: false });

        return cy.get(`gtx-dropdown-content[${ATTR_ID}="${id}"]`, { log: false })
            .then($el => {
                if (options?.log !== false) {
                    Cypress.log({
                        $el: subject,
                        name: 'open context',
                        type: 'child',
                        consoleProps: () => ({
                            // eslint-disable-next-line @typescript-eslint/naming-convention
                            Context: $el,
                        }),
                    });
                }

                return $el;
            });
    });

    /**
     * Overrides the default `click` command, to swap out the subject if needed.
     * This is because angular templates/components are often nested and don't directly
     * show the actual clickable button.
     * @see resolveClickable
     *
     * @example
     * ```html
     * <!-- Angular template -->
     * <gtx-button id="click-me">Hello World</gtx-button>
     *
     * <!-- Actually rendered HTML -->
     * <gtx-button id="click-me">
     *  <div class="button-event-wrapper">
     *      <button
     *          class="btn"
     *          data-action="primary"
     *          type="button"
     *      >Hello World</button>
     *  </div>
     * </gtx-button>
     * ```
     * ```ts
     * // Without the override, the click would happen on our custom component,
     * // which isn't actually clickable, and we would get an error from cypress.
     * cy.get('@click-me').click()
     * // Therefore, you'ld need to actually reference the button within the button
     * cy.get('#click-me button').click();
     *
     * // Since this is tedious, prone to error, and doesn't cover split-buttons
     * // or other variations, this override is here to fix this.
     * // With the override, we can now use it as expected:
     * cy.get('#click-me').click();
     * ```
     */
    Cypress.Commands.overwrite('click', ((originalFn, subject, optionsOrPositionOrX, optionsOrY, options) => {
        let actualOptions;
        if (options != null && typeof options === 'object') {
            actualOptions = options;
        } else if (optionsOrY != null && typeof optionsOrY === 'object') {
            actualOptions = optionsOrY;
        } else if (optionsOrPositionOrX != null && typeof optionsOrPositionOrX === 'object') {
            actualOptions = optionsOrPositionOrX;
        }

        subject = resolveClickable(subject, actualOptions);

        return (originalFn as Cypress.CommandOriginalFnWithSubject<'click', any>)(subject, optionsOrPositionOrX, optionsOrY, options);
    }) as any);

    /**
     * Overrides the default `select` command, to check if the subject is a custom select component.
     * This is to make them behave/useable the same way as native selects, as custom select components
     * are *not* actually using a native one in the background, and therefore require quite
     * some efford to actualy use.
     *
     * If the subject however, is not a special case, the default/original `select` command-function will be used.
     */
    Cypress.Commands.overwrite('select', ((originalFn, subject, value, options) => {
        if (!isJQueryElement(subject)) {
            return (originalFn as Cypress.CommandOriginalFnWithSubject<'select', any>)(subject, value, options);
        }

        if (subject.is('gtx-select')) {
            return cy.wrap(subject.find('> gtx-dropdown-list'), { log: false })
                .openContext()
                .then(elem => {
                    const $container = Cypress.$(elem);
                    if (!Array.isArray(value)) {
                        return $container.find(`[data-id="${value}"]`);
                    }
                    const $out = Cypress.$();

                    for (const singleValue of value) {
                        const $found = $container.find(`[data-id="${singleValue}"]`);
                        if ($found.length !== 1) {
                            const errMsg = `Could not find any select option for value "${singleValue}"!`;
                            Cypress.log({
                                $el: subject,
                                end: true,
                                name: 'select',
                                message: errMsg,
                                type: 'child',
                            });
                            throw new Error(errMsg);
                        }
                        $out.add($found);
                    }

                    return $out;
                })
                .click({ force: true, multiple: true })
                .end();
        }

        return (originalFn as Cypress.CommandOriginalFnWithSubject<'select', any>)(subject, value, options);
    }) as any);

    // TODO: Override the default `check`/`uncheck` command to include `gtx-checkbox` and `gtx-radio-button` behaviour.

    Cypress.Commands.add('btn', { prevSubject: 'element' }, (subject, options) => {
        return cy.wrap(resolveClickable(subject, options), { log: false });
    });

    Cypress.Commands.add('rangeSelection', { prevSubject: 'element' }, (subject, start, end, aloha) => {
        const doc = subject[0].ownerDocument;
        const win = doc.defaultView;

        // create the new selection
        const range = createRange(subject[0], start, end);
        if (!range) {
            return;
        }
        // apply the range
        win?.getSelection()?.removeAllRanges();
        win?.getSelection()?.addRange(range);

        if (aloha) {
            // Update the range in aloha
            updateAlohaRange(win as any, range);
        }

        return cy.wrap(subject, { log: false });
    });

    Cypress.Commands.add('textSelection', { prevSubject: 'element' }, (subject, text, aloha) => {
        const doc = subject[0].ownerDocument;
        const win = doc.defaultView;
        const idx = subject[0].textContent?.indexOf(text);

        if (!idx || idx === -1) {
            return cy.wrap(subject, { log: false });
        }

        // create the new selection
        const range = createRange(subject[0], idx, idx + text.length);
        if (!range) {
            return;
        }
        // apply the range
        win?.getSelection()?.removeAllRanges();
        win?.getSelection()?.addRange(range);


        if (aloha) {
            // Update the range in aloha
            updateAlohaRange(win as any, range);
        }

        return cy.wrap(subject, { log: false });
    });

    Cypress.Commands.add('selectTab', { prevSubject: 'element' }, (subject, tabId) => {
        cy.wrap(subject, { log: false })
            .find(`.tab-links .tab-link[data-id="${tabId}"]`)
            .then($tab => {
                if (!$tab.hasClass('is-active')) {
                    cy.wrap($tab, { log: false }).click();
                }
            });
        return cy.wrap(subject, { log: false })
            .find(`gtx-tab > .tab-content[data-id="${tabId}"]`);
    });

    Cypress.Commands.add('findTableRow', { prevSubject: 'element' }, (subject, id) => {
        return cy.wrap(subject, { log: false })
            .find(`.grid-row.data-row[data-id="${id}"]`);
    });

    Cypress.Commands.add('findTableAction', { prevSubject: 'element' }, (subject, id) => {
        const rows = subject.filter('.data-row');
        const tables = subject.not('.data-row');

        if (rows.length > 0) {
            return cy.wrap(rows, { log: false })
                .find(`.action-column .action-button[data-id="${id}"]`)
                .then($actions => {
                    if (tables.length !== 0) {
                        cy.wrap(tables, { log: false })
                            .find(`.header-row .action-column .action-button[data-id="${id}"]`)
                            .then($multiActions => {
                                $actions.add($multiActions);
                            });
                    }

                    return $actions;
                });
        }

        if (tables.length === 0) {
            return cy.wrap(subject, { log: false });
        }

        return cy.wrap(tables, { log: false })
            .find(`.header-row .action-column .action-button[data-id="${id}"]`);
    });
}
