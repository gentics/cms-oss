/* eslint-disable @typescript-eslint/unified-signatures */
/* eslint-disable @typescript-eslint/no-namespace */
/* eslint-disable @typescript-eslint/prefer-namespace-keyword */
type ItemType = 'folder' | 'page' | 'image' | 'file' | 'form';

type RenderableAlohaComponentType =
    'attribute-button' | 'attribute-toggle-button' | 'button'
    | 'checkbox' | 'color-picker' | 'context-button' | 'context-toggle-button'
    | 'date-time-picker' | 'iframe' | 'input' | 'link-target' | 'select' | 'select-menu'
    | 'split-button' | 'symbol-grid' | 'symbol-search-grid' | 'table-size-select'
    | 'toggle-button' | 'toggle-split-button';

interface FindAlohaRendererOptions {
    slot?: string;
    type?: RenderableAlohaComponentType;
}

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
        login(account: string, keycloak?: boolean): Chainable<null>;
        /**
         * Select the specified node in the editor-ui, to display it's content.
         * @param nodeId The node to select
         */
        selectNode(nodeId: number | string): Chainable<null>;
        /**
         * Attempt to find a specified item-type list.
         * @param type The type of list that should be found/searched for.
         */
        findList(type: ItemType): Chainable<JQuery<HTMLElement>>;
        /**
         * Attempt to find a specified item in a list.
         * @param id The id of the element that should be found/searched for.
         */
        findItem(id: string | number): Chainable<JQuery<HTMLElement>>;
        /**
         * Click/Perform an action on an item (iE edit, preview, delete, ...)
         * @param action The action id to click/perform for an item.
         */
        itemAction(action: string): Chainable<null>;
        /**
         * Select the provided object-property - Requires the `editProperties` mode to be active for the item already.
         * @param name The tag-name of the object-property, without the `object.` prefix.
         */
        openObjectPropertyEditor(name: string): Chainable<JQuery<HTMLElement>>;
        /**
         * Finds the tag-editor element(s) which are for controlling the tag value.
         * @param type The part-type of the tag-editor, i.E. 'SELECT' to get the select property inputs.
         */
        findTagEditorElement(type: string): Chainable<JQuery<HTMLElement>>;
        /**
         * Attempts to find a control based on the specified slot.
         * @param options The options for finding the component.
         */
        findAlohaComponent(options?: FindAlohaRendererOptions): Chainable<HTMLElement>;
        /**
         * Attempts to find a dynamic form-modal element.
         * When a ref is provided, it'll try to find the one with the corresponding ref.
         * @param ref The reference data, if any.
         */
        findDynamicFormModal(ref?: string): Chainable<HTMLElement>;
        /**
         * Attempts to find a dynamic dropdown element.
         * When a ref is provided, it'll try to find the one with the corresponding ref.
         * @param ref The reference data, if any.
         */
        findDynamicDropdown(ref?: string): Chainable<HTMLElement>;
        /** Click the specified action in the editor-toolbar. */
        editorAction(action: string): Chainable<null | HTMLElement>;
    }
}