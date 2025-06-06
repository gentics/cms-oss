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
            return Cypress.$(Array.from($subject.find([
                '> .split-button-wrapper .more-trigger button[data-action="primary"]',
                '.gtx-editor-split-button .split-button-secondary',
                '.gtx-editor-toggle-split-button .split-button-secondary',
            ].join(','))));
        } else {
            return Cypress.$(Array.from($subject.find([
                '> .split-button-wrapper > .primary-button button[data-action="primary"]',
                '.gtx-editor-split-button .split-button-main',
                '.gtx-editor-toggle-split-button .split-button-main',
            ].join(','))));
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

        const newSubject = resolveClickable(subject, actualOptions);

        // Remove the action property after we used it here
        if (actualOptions) {
            delete actualOptions.action;
        }

        if (newSubject === subject) {
            return (originalFn as Cypress.CommandOriginalFnWithSubject<'click', any>)(subject, optionsOrPositionOrX, optionsOrY, options);
        }

        if (actualOptions?.log !== false) {
            Cypress.log({
                $el: newSubject,
                name: 'redirect',
                type: 'child',
                message: newSubject,
                consoleProps: () => ({
                    'Original Target': subject,
                    'New Target': newSubject,
                }),
            });
        }

        // Funny thing: no matter which subject is passed to `originalFn`, it'll call the subject
        // from the state instead. The state can't easily be modified, which is why we wrap the new subject
        // and then call the original function, which fixes this issue.
        cy.wrap(newSubject, { log: false })
            .then($el => {
                // Hacky way to fix the name of the click function logging
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                (cy as any).state('current').attributes.name = 'click';
                return (originalFn as Cypress.CommandOriginalFnWithSubject<'click', any>)($el, optionsOrPositionOrX, optionsOrY, options);
            })

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

    Cypress.Commands.overwrite('check', ((originalFn, subject, options) => {
        if (!isJQueryElement(subject)) {
            return originalFn(subject, options);
        }

        if (subject.is('gtx-checkbox')) {
            const input = subject.find('input[type="checkbox"]');
            const log = () => {
                Cypress.log({
                    $el: subject,
                    name: 'check',
                    type: 'child',
                    message: subject,
                });
            };

            if (!input.is(':checked')) {
                return cy.wrap(subject.find('label'), { log: false })
                    .click({ log: false })
                    .then(() => {
                        log();
                        return subject;
                    });
            } else {
                log();
                return cy.wrap(subject, { log: false });
            }
        }
        // TODO: Add gtx-radio-button handling

        return originalFn(subject, options);
    }));

    Cypress.Commands.overwrite('uncheck', ((originalFn, subject, options) => {
        if (!isJQueryElement(subject)) {
            return originalFn(subject, options);
        }

        if (subject.is('gtx-checkbox')) {
            const input = subject.find('input[type="checkbox"]');
            const log = () => {
                Cypress.log({
                    $el: subject,
                    name: 'uncheck',
                    type: 'child',
                    message: '',
                });
            };

            if (input.is(':checked')) {
                return cy.wrap(subject.find('label'), { log: false })
                    .click({ log: false })
                    .then(() => {
                        log();
                        return subject;
                    });
            } else {
                log();
                return cy.wrap(subject, { log: false });
            }
        }
        // TODO: Add gtx-radio-button handling

        return originalFn(subject, options);
    }));

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

    Cypress.Commands.add('findTableRowContainingText', {prevSubject: 'element'}, (subject, text) => {
        return cy.wrap(subject, { log: false })
            .find('.grid-row')
            .contains(text)
            .parents('.grid-row');
    });

    Cypress.Commands.add('selectTableRow', {prevSubject: 'element'}, (subject) => {
        return cy.wrap(subject, { log: false })
            .find('gtx-checkbox.selection-checkbox')
            .click();
    });

    Cypress.Commands.add('expandTrableRow', {prevSubject: 'element'}, (subject) => {
        return cy.wrap(subject, { log: false })
            .find('.row-expansion-wrapper')
            .click();
    });

    Cypress.Commands.add('pickDate', { prevSubject: 'element' }, (subject, date, options) => {
        if (subject.is('gtx-date-time-picker')) {
            cy.wrap(subject, { log: false })
                .find('.input-element', { log: false })
                .click({ log: false });
            return cy.get('gtx-dynamic-modal gtx-date-time-picker-modal').then($modal => {
                return cy.wrap($modal.find('gtx-date-time-picker-controls'), { log: false })
                    .pickDate(date, options)
                    .then(() => {
                        cy.wrap($modal, { log: false })
                            .find('.modal-footer [data-action="confirm"]', { log: false })
                            .click({ log: false });
                    });
            });
        }

        expect(subject[0].nodeName).to.equal('GTX-DATE-TIME-PICKER-CONTROLS');

        const $content = subject.find('.controls-content');
        const $prevMonthBtn = $content.find('.rd-date .rd-month button.rd-back');
        const $nextMonthBtn = $content.find('.rd-date .rd-month button.rd-next');
        let year: number;
        let month: number;

        const refreshControlData = () => {
            year = parseInt($content.attr('data-value-year'), 10);
            month = parseInt($content.attr('data-value-month'), 10);
        }
        refreshControlData();

        const targetYear = date.getFullYear();
        const targetMonth = date.getMonth();

        // Select the year first
        if (year !== targetYear) {
            // Check if the year selector is available
            const $selector = subject.find('.year-selector gtx-select');
            if ($selector.length > 0) {
                cy.wrap($selector, { log: false })
                    .select(targetYear, { log: false });
            } else {
                // Otherwise navigate using the months (tedious, but works)
                while (year < targetYear) {
                    $nextMonthBtn.trigger('click');
                    refreshControlData();
                }
                while (year > targetYear) {
                    $prevMonthBtn.trigger('click');
                    refreshControlData();
                }
            }
        }

        // Set the month correct
        while (month < targetMonth) {
            $nextMonthBtn.trigger('click');
            refreshControlData();
        }
        while (month > targetMonth) {
            $prevMonthBtn.trigger('click');
            refreshControlData();
        }

        // Select the correct day
        let targetDate = `${date.getDate()}`;
        if (targetDate.length === 1) {
            targetDate = `0${targetDate}`;
        }
        cy.wrap($content, { log: false })
            .find('.rd-date .rd-days .rd-days-body', { log: false })
            .contains(targetDate, { log: false })
            .click({ force: true, log: false });

        // Enter the date if available
        const $timePicker = subject.find('.time-picker');
        if ($timePicker.length > 0) {
            cy.wrap($timePicker, { log: false })
                .find('.hours .input-element', { log: false })
                .type(`{selectAll}${date.getHours()}`, { log: false });
            cy.wrap($timePicker, { log: false })
                .find('.minutes .input-element', { log: false })
                .type(`{selectAll}${date.getMinutes()}`, { log: false });

            const $seconds = $timePicker.find('.seconds');
            if ($seconds.length > 0) {
                cy.wrap($seconds, { log: false })
                    .find('.input-element', { log: false })
                    .type(`{selectAll}${date.getSeconds()}`, { log: false });
            }
        }

        if (options?.log !== false) {
            Cypress.log({
                name: 'pick date',
                type: 'child',
                $el: subject,
                message: `${date.toISOString()} / ${date.getTime()}`,
            });
        }

        return cy.wrap(subject, { log: false });
    });

}
