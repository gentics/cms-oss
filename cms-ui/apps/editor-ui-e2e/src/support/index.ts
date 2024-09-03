/* eslint-disable @typescript-eslint/unified-signatures */
/* eslint-disable @typescript-eslint/no-namespace */
/* eslint-disable @typescript-eslint/prefer-namespace-keyword */
type ItemType = 'folder' | 'page' | 'image' | 'file' | 'form';

interface ImportBinary {
    /** The path to the fixture file to load. */
    fixturePath: string;
    /** The File name. If left empty, it'll be determined from the fixture-path. */
    name?: string;
    /** The mime-type of the binary, because cypress doesn't provide it. */
    type: string;
}
interface ContentFile {
    contents: string | Buffer;
    fileName: string;
    mimeType: string;
}

interface ButtonOptions {
    action?: 'primary' | 'secondary';
}

interface ButtonClickOptions extends ButtonOptions, Partial<Cypress.ClickOptions> {}

interface BinaryLoadOptions {
    applyAlias?: boolean;
}

interface BinaryFileLoadOptions extends BinaryLoadOptions {}

interface BinaryContentFileLoadOptions extends BinaryLoadOptions {
    asContent: true;
}

declare module Chai {
    interface Assertion {
        /**
         * Assertion to check if a value has specific formats applied.
         *
         * @example
        ```ts
        expect($someElement).to.have.formatting('text', ['b']);
        cy.get('.some-element').should('have.formatting', 'text', ['b']);
        ```
         */
        formatting(text: string, format: string[]): Assertion;
        /**
         * Assertion to check if a value is included in the provided array.
         * If the array and the value are both string, then the array value
         * checks with `includes` if the value is included, i.E. `arrVal.includes(expectValue)`,
         * instead of the regular equality check.
         *
         * @example
        ```ts
        expect(123).to.be.included([123, 456, 789]);
        // > true
        expect('hello').to.be.included(['hello', 'world']);
        // > true
        expect('hello').to.be.included(['hello world', 'foo bar']);
        // > true
        cy.wrap(456).should('be.included', [123, 456, 789]);
        ```
         */
        included(array: any[]): Assertion;
    }
}

declare namespace Cypress {
    interface Chainer<Subject> {
        /**
         * Assertion to check if a value has specific formats applied.
         *
         * @example
        ```ts
        expect($someElement).to.have.formatting('text', ['b']);
        cy.get('.some-element').should('have.formatting', 'text', ['b']);
        ```
         */
        (chainer: 'have.formatting', text: string, formats: string[]): Chainable<Subject>;

        /**
         * Assertion to check if a value does not have a specific formats applied.
         *
         * @example
        ```ts
        expect($someElement).not.to.have.formatting('text', ['b']);
        cy.get('.some-element').should('not.have.formatting', 'text', ['b']);
        ```
         */
        (chainer: 'not.have.formatting', text: string, formats: string[]): Chainable<Subject>;

        /**
         * Assertion to check if a value is included in the provided array.
         * If the array and the value are both string, then the array value
         * checks with `includes` if the value is included, i.E. `arrVal.includes(expectValue)`,
         * instead of the regular equality check.
         *
         * @example
        ```ts
        expect(123).to.be.included([123, 456, 789]);
        // > true
        expect('hello').to.be.included(['hello', 'world']);
        // > true
        expect('hello').to.be.included(['hello world', 'foo bar']);
        // > true
        cy.wrap(456).should('be.included', [123, 456, 789]);
        ```
         */
        (chainer: 'be.included', array: any[]): Chainable<Subject>;
        (chainer: 'to.be.included', array: any[]): Chainable<Subject>;

        /**
         * Assertion to check if a value is not included in the provided array.
         * If the array and the value are both string, then the array value
         * checks with `includes` if the value is included, i.E. `arrVal.includes(expectValue)`,
         * instead of the regular equality check.
         *
         * @example
        ```ts
        expect(420).not.to.be.included([123, 456, 789]);
        // > true
        expect('example').not.to.be.included(['hello', 'world']);
        // > true
        expect('world').not.to.be.included(['hello world', 'foo bar']);
        // > false
        cy.wrap(420).should('not.be.included', [123, 456, 789]);
        ```
         */
        (chainer: 'not.be.included', array: any[]): Chainable<Subject>;
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface Chainable<Subject> {
        /**
         * Prevents the logging of XHR/Fetch requests (unless intercepted/aliased).
         * Useful to reduce amount of cypress logs to only show relevant ones, as the UIs
         * request a lot.
         */
        muteXHR(): Chainable<null>;
        /**
         * Loads the defined fixtures and returns a map of the loaded binaries as usable map.
         * @param files The fixture paths or import-binaries to load.
         * @param options The options to use when loading the fixtures/binaries and how to process them.
         */
        loadBinaries(files: (string | ImportBinary)[], options?: BinaryFileLoadOptions): Chainable<Record<string, File>>;
        loadBinaries(files: (string | ImportBinary)[], options?: BinaryContentFileLoadOptions): Chainable<Record<string, ContentFile>>;
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
         * Uploads the specified fixture-names as files or images.
         * @param type If the upload should be done as "file" or "image" to the CMS (Only relevant for which list button to press)
         * @param fixtureNames The names of the fixtures/import-binaries to upload. See `loadBinaries` command.
         * @param dragNDrop If the upload should be done via the drag-n-drop functionality.
         */
        uploadFiles(type: 'file' | 'image', fixtureNames: (string | ImportBinary)[], dragNDrop?: boolean): Chainable<Record<string, any>>;
        /**
         * Requires the subject to be a `gtx-select`.
         * Will select the option with the corresponding `valueId`.
         * @param valueId The value/option to select.
         */
        selectValue(valueId: any): Chainable<null>;
        /** Click the save button in the editor-toolbar */
        editorSave(): Chainable<null>;
        /** Closes the editor */
        editorClose(): Chainable<null>;
        /** */
        btn(options?: ButtonOptions): Chainable<HTMLElement>;
        /** Helper function to press the actual button element of various custom buttons */
        btnClick(options?: ButtonClickOptions): Chainable<HTMLElement>;
        /**
         * Applies the range as selection, and optionally to aloha as well.
         *
         * @param start The start position to start the selection from
         * @param end The end poinsition where the selection ends. When `null`/nothing is provided, it'll
         * set the range till the end of the element. A negative value will count backwards from the end instead.
         * @param aloha If it should apply the range to aloha as well.
         */
        rangeSelection(start: number, end?: number | null, aloha?: boolean): Chainable<HTMLElement>;
        /**
         * Applies the text/substring as selection (if found), and optionally to aloha as well.
         *
         * @param text The string/text that should get selected
         * @param aloha If it should apply the range to aloha as well.
         */
        textSelection(text: string, aloha?: boolean): Chainable<HTMLElement>;
        /**
         * Attempts to find a control based on the specified slot.
         * @param slot The slot of the component to find.
         */
        toolbarFindControl(slot: string): Chainable<HTMLElement>;
    }
}
