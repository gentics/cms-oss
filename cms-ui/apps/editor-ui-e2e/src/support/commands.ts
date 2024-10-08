import { registerCommonCommands, RENDERABLE_ALOHA_COMPONENTS, setupAliasOverrides } from '@gentics/e2e-utils';

setupAliasOverrides();
registerCommonCommands();

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
                .find('[data-action="item-context"]')
                .openContext()
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

Cypress.Commands.add('editorAction', { prevSubject: false }, action => {
    switch (action) {
        case 'editor-context':
            return cy.get('content-frame gtx-editor-toolbar [data-action="editor-context"]')
                .openContext();

        default:
            cy.get(`content-frame gtx-editor-toolbar [data-action="${action}"]`).btnClick();
            return cy.wrap(null, { log: false });
    }
});
