/* eslint-disable @typescript-eslint/unified-signatures */
/* eslint-disable @typescript-eslint/no-namespace */
/* eslint-disable @typescript-eslint/prefer-namespace-keyword */
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
         * ```ts
         * expect($someElement).to.have.formatting('text', ['b']);
         * cy.get('.some-element').should('have.formatting', 'text', ['b']);
         * ```
         */
        formatting(text: string, format: string[]): Assertion;
        /**
         * Assertion to check if a value is included in the provided array.
         * If the array and the value are both string, then the array value
         * checks with `includes` if the value is included, i.E. `arrVal.includes(expectValue)`,
         * instead of the regular equality check.
         *
         * @example
         * ```ts
         * expect(123).to.be.included([123, 456, 789]);
         * // > true
         * expect('hello').to.be.included(['hello', 'world']);
         * // > true
         * expect('hello').to.be.included(['hello world', 'foo bar']);
         * // > true
         * cy.wrap(456).should('be.included', [123, 456, 789]);
         * ```
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
         * ```ts
         * expect($someElement).to.have.formatting('text', ['b']);
         * cy.get('.some-element').should('have.formatting', 'text', ['b']);
         * ```
         */
        (chainer: 'have.formatting', text: string, formats: string[]): Chainable<Subject>;

        /**
         * Assertion to check if a value does not have a specific formats applied.
         *
         * @example
         * ```ts
         * expect($someElement).not.to.have.formatting('text', ['b']);
         * cy.get('.some-element').should('not.have.formatting', 'text', ['b']);
         * ```
         */
        (chainer: 'not.have.formatting', text: string, formats: string[]): Chainable<Subject>;

        /**
         * Assertion to check if a value is included in the provided array.
         * If the array and the value are both string, then the array value
         * checks with `includes` if the value is included, i.E. `arrVal.includes(expectValue)`,
         * instead of the regular equality check.
         *
         * @example
         * ```ts
         * expect(123).to.be.included([123, 456, 789]);
         * // > true
         * expect('hello').to.be.included(['hello', 'world']);
         * // > true
         * expect('hello').to.be.included(['hello world', 'foo bar']);
         * // > true
         * cy.wrap(456).should('be.included', [123, 456, 789]);
         * ```
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
         * ```ts
         * expect(420).not.to.be.included([123, 456, 789]);
         * // > true
         * expect('example').not.to.be.included(['hello', 'world']);
         * // > true
         * expect('world').not.to.be.included(['hello world', 'foo bar']);
         * // > false
         * cy.wrap(420).should('not.be.included', [123, 456, 789]);
         * ```
         */
        (chainer: 'not.be.included', array: any[]): Chainable<Subject>;
    }

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
         * Uploads the specified fixture-names as files or images.
         * @param type If the upload should be done as "file" or "image" to the CMS (Only relevant for which list button to press)
         * @param fixtureNames The names of the fixtures/import-binaries to upload. See `loadBinaries` command.
         * @param dragNDrop If the upload should be done via the drag-n-drop functionality.
         */
        uploadFiles(type: 'file' | 'image', fixtureNames: (string | ImportBinary)[], dragNDrop?: boolean): Chainable<Record<string, any>>;
        /**
         * Current element needs to be a `gtx-dropdown-list` element.
         * Will click the trigger-element (`data-context-trigger`) with `btnClick`,
         * and gets the associated `gtx-dropdown-content` based on the `data-context-id` attributes.
         *
         * @example
         * ```html
         * <gtx-dropdown-list
         *      class="my-dropdown"
         *      data-context-id="my-context-id"
         * >
         *      <gtx-dropdown-trigger>
         *          <gtx-button data-context-trigger>Click me</gtx-button>
         *      </gtx-dropdown-trigger>
         *
         *      <gtx-dropdown-content data-context-id="my-context-id">
         *          <gtx-dropdown-item data-action="whatever"></gtx-dropdown-item>
         *          <!-- ... -->
         *      </gtx-dropdown-content>
         * </gtx-dropdown-list>
         * ```
         *
         * ---
         *
         * ```ts
         * // my-test.cy.ts
         * cy.get('.my-dropdown')
         *      .openContext()
         *      .find('[data-action="whatever"]')
         *      .click();
         * ```
         */
        openContext(): Chainable<HTMLElement>;
        /**
         * Requires the subject to be a `gtx-select`.
         * Will select the option with the corresponding `valueId`.
         * @param valueId The value/option to select.
         */
        selectValue(valueId: any): Chainable<null>;
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
    }
}
