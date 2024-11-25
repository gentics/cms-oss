// eslint-disable-next-line @typescript-eslint/no-namespace, @typescript-eslint/no-unused-vars
declare namespace Cypress {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface Chainable<Subject> {
        /**
         * Helper to navigate to the application.
         * @param path The route/path in the application to navigate to. Usually leave this empty, unless you need to
         * test the routing of the application.
         * @param raw If the navigation should happen without adding a `skip-sso` to prevent unwilling sso logins.
         */
        navigateToApp(path?: string, raw?: boolean): Chainable<void>;
        /**
         * Login with pre-defined user data or with a cypress alias.
         * @param account The account name in the `auth.json` fixture, or an alias to a credentials object.
         * @param keycloak If this is a keycloak login.
         */
        login(account: string, keycloak?: boolean): Chainable<void>;
        /**
         * As nearly all features of the admin-ui are separated into individual modules,
         * this helper command finds and opens the specified module from the dashboard.
         * If a permission is passed in as well, it'll wait for that to be loaded, as
         * otherwise the module may not be available.
         * @param moduleId The id of the module to navigate to.
         * @param perms The permissions of the module - Should be provided so we wait for the permissions to be loaded.
         */
        navigateToModule(moduleId: string, perms?: string): Chainable<JQuery<HTMLElement>>;
        /**
         * Attempts to get the detail view content.
         */
        getDetailView(): Chainable<JQuery<HTMLElement>>;
        /**
         * Attempts to find the specified mesh entity in the subject and opens the edit modal for it.
         * @param type The type of the mesh entity
         * @param identifier The identifier of the entitiy to interact with.
         */
        editMeshEntity(type: string, identifier: string): Chainable<JQuery<HTMLElement>> | Chainable<null>;
    }
}
