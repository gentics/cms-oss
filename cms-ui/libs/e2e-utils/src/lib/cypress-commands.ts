import { normalizeToImportBinary, resolveFixtures } from './binaries';
import { createRange, updateAlohaRange } from './utils';

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

    Cypress.Commands.add('uploadFiles', { prevSubject: false }, (type, fixtureNames, dragNDrop) => {
        cy.intercept({
            method: 'POST',
            pathname: '/rest/file/create',
        }, (req) => {
            // We need the form-data string that is being sent to the CMS
            // The actual FormData object would be best of course, but isn't available here
            let body: string = req.body;

            if (
                // Very hacky, but the types are different between the runtimes which causes this issue
                (body as any) instanceof ArrayBuffer
                || Object.getPrototypeOf(body).constructor.name === 'ArrayBuffer'
            ) {
                // `fatal` has to be false, otherwise it'll break
                const decoder = new TextDecoder('utf-8', { fatal: false });
                body = decoder.decode(body as any as ArrayBuffer);
            }

            // Get the filename from the form-data request
            // The fileName is (or should be) normalized already
            const fileName = /filename="([^"]*)"/.exec(body)?.[1] || '';
            req.alias = `_upload_req_${fileName}`;
        });

        // Wait till elements have been reloaded
        cy.intercept({
            method: 'GET',
            pathname: '/rest/folder/getPages/*',
        }).as('folderLoad');

        return cy.loadBinaries(fixtureNames, { applyAlias: true }).then(binaries => {
            const output: Record<string, any> = {};
            let main: Cypress.Chainable<any>;

            if (dragNDrop) {
                const transfer = new DataTransfer();
                // Put the binaries/Files into the transfer
                Object.values(binaries).forEach(file => {
                    transfer.items.add(file);
                });

                main = cy.get('folder-contents > [data-action="file-drop"]').trigger('drop', {
                    dataTransfer: transfer,
                    force: true,
                });
            } else {
                main = cy.findList(type)
                    .find('.list-header .header-controls [data-action="upload-item"] input[type="file"]')
                    .selectFile(fixtureNames.map(entry => '@' + (typeof entry === 'string' ? entry : entry.fixturePath)), { force: true });
            }

            return main.then(() => {
                for (const entry of fixtureNames) {
                    const data = normalizeToImportBinary(entry);

                    cy.wait(`@_upload_req_${data.name}`).then(intercept => {
                        const res = intercept.response?.body;
                        // eslint-disable-next-line @typescript-eslint/no-unused-expressions
                        expect(res.success).to.be.true;
                        output[data.fixturePath] = res.file;
                    });
                }

                return cy.wait('@folderLoad').then(() => cy.wrap(output, { log: false }));
            })
        });
    });

    Cypress.Commands.add('selectValue', { prevSubject: 'element' }, (subject, valueId) => {
        cy.wrap(subject, { log: false }).click({ force: true });
        cy.get('gtx-dropdown-content.select-context')
            .find(`.select-option[data-id="${valueId}"]`)
            .click({ force: true });
        return cy.wrap(null, { log: false });
    });

    Cypress.Commands.add('openContext', { prevSubject: 'element' }, subject => {
        const ATTR_ID = 'data-context-id';
        const id = subject.attr(ATTR_ID);
        if (!id) {
            cy.log(`Cannot open element context, since attribute "${ATTR_ID}" is missing!`);
            return cy.wrap(null, { log: false });
        }

        cy.wrap(subject).find('gtx-dropdown-trigger [data-context-trigger]').btnClick();

        return cy.get(`gtx-dropdown-content[${ATTR_ID}="${id}"]`);
    });

    const GTX_BUTTONS: string[] = [
        'gtx-button',
        ...[
            'attribute',
            'attribute-toggle',
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
    ];

    Cypress.Commands.add('btn', { prevSubject: 'element' }, (subject, options) => {
        const action = options?.action ?? 'primary';
        const actionBtn = `button[data-action="${action}"]`;
        let buttonSelectors = GTX_BUTTONS.map(tagName => `${tagName} ${actionBtn}`);

        // Split buttons have to be handled differently
        if (subject.is(GTX_SPLIT_BUTTONS.join(','))) {
            if (action === 'secondary') {
                buttonSelectors = [
                    '> .split-button-wrapper .more-trigger button[data-action="primary"]',
                    '.gtx-editor-split-button .split-button-secondary',
                    '.gtx-editor-toggle-split-button .split-button-secondary',
                ];
            } else {
                buttonSelectors = [
                    '> .split-button-wrapper > .primary-button button[data-action="primary"]',
                    '.gtx-editor-split-button .split-button-main',
                    '.gtx-editor-toggle-split-button .split-button-main',
                ];
            }
        }

        let $elem = subject.find(buttonSelectors.join(', '));
        if (!$elem || $elem.length === 0) {
            $elem = subject.find(actionBtn);
        }
        if (!$elem || $elem.length === 0) {
            return cy.wrap(subject, { log: false });
        }

        return cy.wrap($elem);
    });

    Cypress.Commands.add('btnClick', { prevSubject: 'element' }, (subject, options) => {
        // eslint-disable-next-line prefer-const
        let { action, ...cyOptions } = options ?? {};
        return cy.wrap(subject, { log: false })
            .btn({ action })
            .click(cyOptions);
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
}
