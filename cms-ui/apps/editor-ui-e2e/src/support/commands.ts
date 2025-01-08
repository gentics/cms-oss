import { LoginRequest, LoginResponse } from '@gentics/cms-models';
import { normalizeToImportBinary, registerCommonCommands, RENDERABLE_ALOHA_COMPONENTS, setupAliasOverrides } from '@gentics/e2e-utils';

setupAliasOverrides();
registerCommonCommands();

Cypress.Commands.add('navigateToApp', { prevSubject: false }, (path, raw) => {
    /*
     * The baseUrl is always properly configured via NX.
     * When using the CI however, we use the served UI from the CMS directly.
     * Therefore we also have to use the correct path for it.
     */
    const appBasePath = Cypress.env('CI') ? Cypress.env('CMS_EDITOR_PATH') : '/';
    Cypress.log({
        name: 'navigate',
        type: 'parent',
        message: path || '/',
        consoleProps: () => ({
            // eslint-disable-next-line @typescript-eslint/naming-convention
            'Base Path': appBasePath,
            // eslint-disable-next-line @typescript-eslint/naming-convention
            Raw: raw,
        }),
    });
    return cy.visit(`${appBasePath}${!raw ? '?skip-sso' : ''}#${path || ''}`, { log: false });
});

Cypress.Commands.add('login', { prevSubject: false }, (account, keycloak) => {
    return cy.fixture('auth.json').then(auth => {
        const data = auth[account];
        if (data) {
            return data;
        }
        return cy.get(account);
    }).then(data => {
        Cypress.log({
            name: 'login',
            message: keycloak ? 'via Keycloak SSO' : 'direct',
            type: 'parent',
        });

        cy.get('input[type="text"]', { log: false }).type(data.username, { log: false });
        cy.get('input[type="password"]', { log: false }).type(data.password, { log: false });

        const ALIAS_LOGIN_REQ = '@loginReq';
        cy.intercept({
            method: 'POST',
            pathname: '/rest/auth/login',
        }, req => {
            req.alias = ALIAS_LOGIN_REQ;
        });

        cy.get(`${keycloak ? 'input' : 'button'}[type="submit"]`, { log: false }).click({ log: false });

        if (keycloak) {
            return cy.wrap(null, { log: false });
        }

        return cy.wait<LoginRequest, LoginResponse>(ALIAS_LOGIN_REQ, { log: false }).then(inter => {
            Cypress.log({
                name: 'login success',
                type: 'child',
                message: ` ${inter.response?.body.user.firstName} ${inter.response?.body.user.lastName} (${inter.response?.body.user.id})`,
                consoleProps: () => ({
                    // eslint-disable-next-line @typescript-eslint/naming-convention
                    User: inter.response?.body.user,
                    // eslint-disable-next-line @typescript-eslint/naming-convention
                    SID: inter.response?.body.sid,
                }),
            })
            return inter.response?.body;
        });
    });
});

Cypress.Commands.add('selectNode', { prevSubject: 'optional' }, (subject, nodeId) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.get('folder-contents', { log: false });

    root.then($el => {
        Cypress.log({
            $el: $el as any,
            name: 'select node',
            type: subject ? 'child' : 'parent',
            message: nodeId,
        });
    });

    root.find('node-selector [data-action="select-node"]', { log: false })
        .click({ log: false });
    cy.get('gtx-app-root .node-selector-list', { log: false })
        .find(`[data-id="${nodeId}"], [data-global-id="${nodeId}"]`, { log: false })
        .click({ log: false });
    return cy.wrap(null, { log: false });
});

Cypress.Commands.add('findList', { prevSubject: 'optional' }, (subject, type, options) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.get('folder-contents', { log: false });
    return root
        .find(`item-list .content-list[data-item-type="${type}"]`, { log: false, timeout: 20_000 })
        .then($el => {
            if (options?.log !== false) {
                Cypress.log({
                    $el: subject as any,
                    name: 'find list',
                    message: type,
                    type: subject ? 'child' : 'parent',
                });
            }

            return $el;
        });
});

Cypress.Commands.add('findItem', { prevSubject: 'element' }, (subject, id, options) => {
    return cy.wrap(subject, { log: false })
        .find(`gtx-contents-list-item[data-id="${id}"], masonry-item[data-id="${id}"], repository-browser-list-thumbnail[data-id="${id}"]`, { log: false, timeout: 20_000 })
        .then($el => {
            if (options?.log !== false) {
                Cypress.log({
                    $el: subject,
                    name: 'find item',
                    message: id,
                    type: 'child',
                });
            }

            return $el;
        });
});

Cypress.Commands.add('uploadFiles', { prevSubject: false }, (type, fixtureNames, options) => {
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

    return cy.loadBinaries(fixtureNames, { applyAlias: true }).then(binaries => {
        const output: Record<string, any> = {};
        let main: Cypress.Chainable<any>;

        if (options?.log !== false) {
            Cypress.log({
                type: 'parent',
                name: 'upload files',
                message: options?.dragAndDrop ? 'via drag and drop' : 'via regular upload',
                consoleProps: () => ({
                    files: fixtureNames,
                }),
            });
        }

        if (options?.dragAndDrop) {
            const transfer = new DataTransfer();
            // Put the binaries/Files into the transfer
            Object.values(binaries).forEach(file => {
                transfer.items.add(file);
            });

            main = cy.get('folder-contents > [data-action="file-drop"]', { log: false }).trigger('drop', {
                dataTransfer: transfer,
                force: true,
                log: false,
            });
        } else {
            main = cy.findList(type, { log: false })
                .find('.list-header .header-controls [data-action="upload-item"] input[type="file"]', { log: false })
                .selectFile(fixtureNames.map(entry => '@' + (typeof entry === 'string' ? entry : entry.fixturePath)), { force: true });
        }

        return main.then(() => {
            for (const entry of fixtureNames) {
                const data = normalizeToImportBinary(entry);

                cy.wait(`@_upload_req_${data.name}`, { log: false }).then(intercept => {
                    const res = intercept.response?.body;
                    expect(res.success).to.equal(true);
                    output[data.fixturePath] = res.file;

                    if (options?.log !== false) {
                        Cypress.log({
                            type: 'child',
                            name: 'file uploaded',
                            message: data.name,
                        });
                    }
                });
            }

            return cy.wrap(output, { log: false });
        })
    });
});

Cypress.Commands.add('getAlohaIFrame', { prevSubject: 'optional' }, (subject, options) => {
    const iframeSelector = 'iframe[name="master-frame"][loaded="true"]';
    // High timeout for all of aloha to finish loading
    const iframe: Cypress.Chainable<JQuery<HTMLIFrameElement>> = subject
        ? cy.wrap(subject, { log: false })
            .find(iframeSelector, { timeout: 60_000, log: false })
        : cy.get(`project-editor content-frame ${iframeSelector}`, { timeout: 60_000, log: false });

    return iframe.then($iframe => {
        const body = $iframe[0].contentDocument?.body;

        if (options?.log !== false) {
            Cypress.log({
                $el: subject as any,
                name: 'aloha iframe',
                type: subject ? 'child' : 'parent',
                message: '',
            });
        }

        return cy.wrap(body ? Cypress.$(body) : null, { log: false });
    });
});

Cypress.Commands.add('findAlohaComponent', { prevSubject: 'optional' }, (subject, options) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.get('project-editor content-frame gtx-page-editor-controls', { log: false });
    const slotSelector = options?.slot ? `[data-slot="${options.slot}"]` : '';
    const childSelector = (options?.type ? RENDERABLE_ALOHA_COMPONENTS[options.type] : '*') || '*';

    return root.find(`gtx-aloha-component-renderer${slotSelector} > ${childSelector}`, { log: false }).then($el => {
        if (options?.log !== false) {
            Cypress.log({
                $el: subject as any,
                name: 'aloha component',
                message: options?.slot || 'unknown',
                type: subject ? 'child' : 'parent',
            });
        }

        return $el;
    });
});

Cypress.Commands.add('findDynamicFormModal', { prevSubject: 'optional' }, (subject, ref, options) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.root({ log: false });
    const refSelector = ref ? `[data-ref="${ref}"]` : '';
    return root.find(`gtx-dynamic-modal gtx-dynamic-form-modal${refSelector}`, { log: false }).then($el => {
        if (options?.log !== false) {
            Cypress.log({
                $el: subject as any,
                name: 'dynamic form modal',
                message: ref || '',
                type: subject ? 'child' : 'parent',
            });
        }

        return $el;
    });
});

Cypress.Commands.add('findDynamicDropdown', { prevSubject: 'optional' }, (subject, ref, options) => {
    const root = subject ? cy.wrap(subject, { log: false }) : cy.root({ log: false });
    const refSelector = ref ? `[data-ref="${ref}"]` : '';

    return root.find(`gtx-dynamic-dropdown .gtx-context-menu${refSelector}`, { log: false }).then($el => {
        if (options?.log !== false) {
            Cypress.log({
                $el: subject as any,
                name: 'dynamic dropdown',
                message: ref || '',
                type: subject ? 'child' : 'parent',
            });
        }

        return $el;
    });
});

Cypress.Commands.add('itemAction', { prevSubject: 'element' }, (subject, action) => {
    Cypress.log({
        $el: subject,
        name: 'item action',
        message: action,
        type: 'child',
    });

    switch (action) {
        // For other actions such as selecting or similar
        default:
            // eslint-disable-next-line cypress/unsafe-to-chain-command
            return cy.wrap(subject, { log: false })
                .find('[data-action="item-context"]', { log: false })
                .openContext({ log: false })
                .find(`[data-action="${action}"]`, { log: false })
                .click({ force: true, log: false })
                .end();
    }
});

Cypress.Commands.add('openObjectPropertyEditor', { prevSubject: false }, (name, options) => {
    cy.get(`content-frame combined-properties-editor .tab-link[data-id="object.${name}"]`, { log: false })
        .click({ force: true, log: false });
    return cy.get('content-frame combined-properties-editor .properties-content .tag-editor tag-editor-host', { log: false }).then($el => {
        if (options?.log !== false) {
            Cypress.log({
                $el,
                name: 'open object-property editor',
                message: '',
                type: 'parent',
            });
        }

        return $el;
    });
});

Cypress.Commands.add('findTagEditorElement', { prevSubject: 'element' }, (subject, type, options) => {
    switch (type) {
        // Should always use the `TagPropertyType` values
        case 'select':
        case 'SELECT':
            return cy.wrap(subject, { log: false })
                .find('gentics-tag-editor select-tag-property-editor gtx-select', { log: false })
                .then($el => {
                    if (options?.log !== false) {
                        Cypress.log({
                            $el: subject,
                            name: 'tag-editor element',
                            message: 'select',
                            type: 'child',
                        });
                    }

                    return $el;
                });

        default:
            return cy.wrap(subject, { log: false });
    }
});

Cypress.Commands.add('editorAction', { prevSubject: false }, (action, options) => {
    if (options?.log !== false) {
        Cypress.log({
            name: 'editor action',
            type: 'parent',
            message: action,
        });
    }

    switch (action) {
        case 'editor-context':
            return cy.get('content-frame gtx-editor-toolbar [data-action="editor-context"]', { log: false })
                .openContext();

        default:
            cy.get(`content-frame gtx-editor-toolbar [data-action="${action}"]`, { log: false })
                .click({ log: false });
            return cy.wrap(null, { log: false });
    }
});

Cypress.Commands.add('inputSearchSelect', { prevSubject: 'element' }, (subject, option, options) => {
    cy.wrap(subject, { log: false })
        .find('button.default-trigger, gtx-button', { log: false })
        .click({ log: false });
    cy.wrap(subject, { log: false })
        .find(`.custom-content-menu .custom-content-menu-button[data-value="${option}"]`, { log: false })
        .click({ log: false });

    if (options?.log !== false) {
        Cypress.log({
            name: 'input-select',
            type: 'child',
            $el: subject,
            message: option,
        });
    }

    return cy.wrap(subject, { log: false });
});

Cypress.Commands.add('addSearchChip', { prevSubject: 'element' }, (subject, options) => {
    cy.wrap(subject, { log: false })
        .find('.gtx-chipsearchbar-button-container .gtx-chipsearchbar-menu-filter-properties > gtx-input-select', { log: false })
        .inputSearchSelect(options.property, { log: false });

    // eslint-disable-next-line cypress/no-assigning-return-values
    const findChip = () => cy.wrap(subject, { log: false })
        .find(`.gtx-chip[data-id="${options.property}"]`, { log: false });

    if (options.operator) {
        findChip().find('.gtx-chip-operator > gtx-input-select', { log: false })
            .inputSearchSelect(options.operator, { log: false });
    }

    findChip().find('.gtx-chip-input-value > *').then($el => {
        switch ($el[0].nodeName) {
            case 'GTX-INPUT-SELECT':
                cy.wrap($el, { log: false })
                    .inputSearchSelect(options.value as any, { log: false });
                break;

            case 'INPUT':
                cy.wrap($el, { log: false })
                    .type(`${options.value as any}`, { log: false });
                break;

            case 'GTX-CHECKBOX': {
                const cb = $el.find('input[type="checkbox"]');
                if (
                    (cb.is(':checked') && options.value !== true)
                    || (!cb.is(':checked') && options.value === true)
                ) {
                    cy.wrap(cb, { log: false })
                        .click({ force: true });
                }
                break;
            }

            case 'DIV': {
                cy.wrap($el, { log: false })
                    .find('gtx-date-time-picker', { log: false })
                    .pickDate(options.value as any);
            }
        }
    });

    if (options?.log !== false) {
        Cypress.log({
            name: 'add chip',
            type: 'child',
            $el: subject,
            message: `${options.property} ${options.operator || 'IS'} ${options.value}`,
        });
    }

    return cy.wrap(subject, { log: false });
});

Cypress.Commands.add('removeSearchChip', { prevSubject: 'element' }, (subject, property, options) => {
    cy.wrap(subject, { log: false })
        .find(`.gtx-chip[data-id="${property}"] .gtx-chip-button-remove gtx-button`, { log: false })
        .click({ log: false });

    return cy.wrap(subject, { log: false });
});

Cypress.Commands.add('search', { prevSubject: 'element' }, (subject, options) => {
    cy.wrap(subject, { log: false })
        .find('.gtx-chipsearchbar-button-container .gtx-chipsearchbar-button gtx-button[data-action="search"]', { log: false })
        .click({ log: false });

    if (options?.log !== false) {
        Cypress.log({
            name: 'trigger search',
            type: 'child',
            $el: subject,
            message: '',
        });
    }

    return cy.wrap(null, { log: false });
});
