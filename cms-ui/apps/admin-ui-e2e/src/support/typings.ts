declare namespace Cypress {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface Chainable<Subject> {
        navigateToApp(path?: string, raw?: boolean): Chainable<void>;
        login(account: string, keycloak?: boolean): Chainable<void>;
        editEntity(type: string, identifier: string): Chainable<JQuery<HTMLElement>> | Chainable<null>;
    }
}
