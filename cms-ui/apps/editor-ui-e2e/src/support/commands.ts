// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

import { createRange, normalizeToImportBinary, RENDERABLE_ALOHA_COMPONENTS, resolveFixtures, updateAlohaRange } from '@gentics/e2e-utils';

/*
 * Override of the original `as` query, which allows the prefix `@` to be provided/ignored.
 * This is to make it possible to use constants for aliases easier like this:
 * @example
```ts
    const ALIAS_FOO = '@test';

    cy.get('.something .foo-bar').as(ALIAS_FOO);
    cy.get(ALIAS_FOO).then($doStuff => {
        // ...
    });
```
 * Here we can use the constant `ALIAS_FOO` for both setting and accessing the alias, instead of having to either
 * cut it off or add the `@` before every `cy.get` call, which is just very verbose and unintuitive.
 */
Cypress.Commands.overwriteQuery('as', function (originalFn, alias, options) {
    if (alias.startsWith('@')) {
        alias = alias.substring(1);
    }
    const fn = originalFn.apply(this, [alias, options]);

    // eslint-disable-next-line @typescript-eslint/no-unsafe-call
    return (subject) => fn(subject);
});

Cypress.Commands.add('muteXHR', () => {
    // Disable logging of XHR/Fetch requests, since they just spam everything
    return cy.intercept({ resourceType: /xhr|fetch/ }, { log: false });
});

Cypress.Commands.add('loadBinaries', (files, options) => {
    return resolveFixtures(files, options);
});

Cypress.Commands.add('navigateToApp', { prevSubject: false }, (path, raw) => {
    /*
     * The baseUrl is always properly configured via NX.
     * When using the CI however, we use the served UI from the CMS directly.
     * Therefore we also have to use the correct path for it.
     */
    const appBasePath = Cypress.env('CI') ? Cypress.env('CMS_EDITOR_PATH') : '/';
    cy.visit(`${appBasePath}${!raw ? '?skip-sso' : ''}#${path || ''}`);
});

Cypress.Commands.add('login', { prevSubject: false }, (account, keycloak) => {
    return cy.fixture('auth.json').then(auth => {
        const data = auth[account];
        if (data) {
            return data;
        }
        return cy.get(account);
    }).then(data => {
        cy.get('input[type="text"]').type(data.username);
        cy.get('input[type="password"]').type(data.password);

        cy.get(`${keycloak ? 'input' : 'button'}[type="submit"]`).click();
    });
});

Cypress.Commands.add('selectNode', { prevSubject: 'optional' }, (subject, nodeId) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.get('folder-contents');
    root.find('node-selector [data-action="select-node"]')
        .click();
    cy.get('gtx-app-root .node-selector-list')
        .find(`[data-id="${nodeId}"], [data-global-id="${nodeId}"]`)
        .click();
    return cy.wrap(null, { log: false });
});

Cypress.Commands.add('findList', { prevSubject: 'optional' }, (subject, type) => {
    const root = subject ? cy.wrap(subject) : cy.get('folder-contents');
    return root.find(`item-list .content-list[data-item-type="${type}"]`);
});

Cypress.Commands.add('findItem', { prevSubject: 'element' }, (subject, id) => {
    return cy.wrap(subject, { log: false })
        .find(`gtx-contents-list-item[data-id="${id}"], masonry-item[data-id="${id}"]`);
});

Cypress.Commands.add('findAlohaComponent', { prevSubject: 'optional' }, (subject, options) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.get('project-editor content-frame gtx-page-editor-controls');
    const slotSelector = options?.slot ? `[data-slot="${options.slot}"]` : '';
    const childSelector = (options?.type ? RENDERABLE_ALOHA_COMPONENTS[options.type] : '*') || '*';
    return root.find(`gtx-aloha-component-renderer${slotSelector} > ${childSelector}`);
});

Cypress.Commands.add('findDynamicFormModal', { prevSubject: 'optional' }, (subject, ref) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.get('gtx-app-root');
    const refSelector = ref ? `[data-ref="${ref}"]` : '';
    return root.find(`gtx-dynamic-modal gtx-dynamic-form-modal${refSelector}`);
});

Cypress.Commands.add('findDynamicDropdown', { prevSubject: 'optional' }, (subject, ref) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.get('gtx-app-root');
    const refSelector = ref ? `[data-ref="${ref}"]` : '';
    return root.find(`gtx-dynamic-dropdown .gtx-context-menu${refSelector}`);
});

Cypress.Commands.add('itemAction', { prevSubject: 'element' }, (subject, action) => {
    switch (action) {
        // For other actions such as selecting or similar
        default:
            cy.wrap(subject, { log: false })
                .find('.context-menu gtx-button[data-action="open-item-context-menu"]')
                .btnClick();
            cy.get('gtx-app-root .item-context-menu-content')
                .find(`[data-action="${action}"]`)
                .click({ force: true });
            return cy.wrap(null);
    }
});

Cypress.Commands.add('openObjectPropertyEditor', { prevSubject: false }, (name) => {
    cy.get(`content-frame combined-properties-editor .tab-link[data-id="object.${name}"]`)
        .click({ force: true });
    return cy.get('content-frame combined-properties-editor .properties-content .tag-editor tag-editor-host');
});

Cypress.Commands.add('findTagEditorElement', { prevSubject: 'element' }, (subject, type) => {
    switch (type) {
        // Should always use the `TagPropertyType` values
        case 'select':
        case 'SELECT':
            return subject.find('gentics-tag-editor select-tag-property-editor gtx-select .select-input');

        default:
            return subject;
    }
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

Cypress.Commands.add('editorSave', { prevSubject: false }, () => {
    cy.get('content-frame gtx-editor-toolbar .save-button').btnClick();
    return cy.wrap(null, { log: false });
});

Cypress.Commands.add('editorClose', { prevSubject: false }, () => {
    cy.get('content-frame gtx-editor-toolbar [data-action="close"]').btnClick();
    return cy.wrap(null, { log: false });
});

const GTX_BUTTONS: string[] = [
    'gtx-button',
    ...[
        'attribute',
        'attribute-toggle',
        '', // will be correct in assembly
        'context',
        'context-toggle',
        'split',
        'toggle',
        'toggle-split',
    ].map(name => `gtx-aloha-${name}${name !== '' ? '-' : ''}button-renderer`),
];

Cypress.Commands.add('btn', { prevSubject: 'element' }, (subject, options) => {
    const action = options?.action ?? 'primary';
    const actionBtn = `button[data-action="${action}"]`;

    let $elem = subject.find(GTX_BUTTONS.map(tagName => `${tagName} ${actionBtn}`).join(', '));
    if (!$elem || $elem.length === 0) {
        $elem = subject.find(actionBtn);
    }
    if (!$elem || $elem.length === 0) {
        return cy.wrap(subject, { log: false });
    }
    return cy.wrap($elem, { log: false });
});

Cypress.Commands.add('btnClick', { prevSubject: 'element' }, (subject, options) => {
    // eslint-disable-next-line prefer-const
    let { action, ...cyOptions } = options ?? {};
    return cy.wrap(subject, { log: false }).btn({ action }).click(cyOptions);
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
