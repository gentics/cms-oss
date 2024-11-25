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

interface ClickableOptions {
    action?: 'primary' | 'secondary';
}

interface BinaryLoadOptions {
    applyAlias?: boolean;
}

interface BinaryFileLoadOptions extends BinaryLoadOptions {}

interface BinaryContentFileLoadOptions extends BinaryLoadOptions {
    asContent: true;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
declare module Chai {
    interface Assertion {
        /**
         * Assertion to check if a value has specific formats applied.
         *
         * @example
         * ```html
         * <div class="some-element">
         *      <p>Hello World! This <b>text</b> is <i>formatted</i>!
         * </div>
         * ```
         * ```ts
         * expect($('.some-element)).to.have.formatting('text', ['b']);
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
        /**
         * Assertion to check if an element is actually displayed to the user.
         * This is done with the `IntersectionObserver` API to determine it's intersection with the
         * current viewport.
         * @param threshold How much of the element needs to be visible in order to be considered displayed.
         *
         * @example
         * ```ts
         * cy.get('.some-element').should('be.displayed');
         * ```
         */
        displayed(options?: IntersectionObserverInit): Assertion;
    }
}

declare namespace Cypress {
    interface Chainer<Subject> {
        /**
         * Assertion to check if a value has specific formats applied.
         *
         * @example
         * ```ts
         * expect($('.some-element')).to.have.formatting('text', ['b']);
         * cy.get('.some-element').should('have.formatting', 'text', ['b']);
         * ```
         */
        (chainer: 'have.formatting', text: string, formats: string[]): Chainable<Subject>;

        /**
         * Assertion to check if a value does not have a specific formats applied.
         *
         * @example
         * ```ts
         * expect($('.some-element')).not.to.have.formatting('text', ['b']);
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

        /**
         * Assertion to check if an element is actually displayed to the user.
         * This is done with the `IntersectionObserver` API to determine it's intersection with the
         * current viewport.
         * @param threshold How much of the element needs to be visible in order to be considered displayed.
         *
         * @example
         * ```ts
         * cy.get('.some-element').should('be.displayed');
         * ```
         */
        (chainer: 'be.displayed', threshold?: number | string): Chainable<Subject>;
        (chainer: 'to.be.displayed', threshold?: number | string): Chainable<Subject>;

        /**
         * Assertion to check if an element is actually displayed to the user.
         * This is done with the `IntersectionObserver` API to determine it's intersection with the
         * current viewport.
         * @param threshold How much of the element needs to be visible in order to be considered displayed.
         *
         * @example
         * ```ts
         * cy.get('.some-element').should('not.be.displayed');
         * ```
         */
        (chainer: 'not.be.displayed', options?: IntersectionObserverInit): Chainable<Subject>;
        (chainer: 'not.to.be.displayed', options?: IntersectionObserverInit): Chainable<Subject>;
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
        openContext(options?: Partial<Cypress.Loggable>): Chainable<HTMLElement>;
        /**
         * Resolves the actual clickable button of an element.
         * Only intended for custom button components like `gtx-button`, `gtx-split-button`, and the sort.
         * @param options Options to resolve the button
         */
        btn(options?: ClickableOptions): Chainable<HTMLElement>;
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
         * Requires the subject to be a gtx-table and will find the (first) row containing the given text
         * @param text The text which should be found
         */
        findTableRowContainingText(text: string): Chainable<JQuery<HTMLElement>>;
        /**
         * Requires the subject to be a row in a gtx-table and will select that row
         */
        selectTableRow(): Chainable<JQuery<HTMLElement>>;
        /**
         * Requires the subject to be a row in a trable and will expand that row
         */
        expandTrableRow(): Chainable<JQuery<HTMLElement>>;
        /**
         * Requires the subject to be a `gtx-tabs`.
         * Will select the tab with the corresponding `tabId`, and yield the tab body.
         * @param tabId The ID of the Tab to select.
         */
        // TODO: Override the default `select` command to include this behaviour.
        selectTab(tabId: string): Chainable<HTMLElement>;
        /**
         * Requires the subject to only contain one `gtx-table`.
         * Will find the row of the table with the corresponding `id`.
         * @param id The id of the row.
         */
        findTableRow(id: string): Chainable<HTMLElement>;
        /**
         * Requires the subject to be a table row to find a `single` action,
         * otherwise will look for multi actions.
         * @param id The id of the action.
         */
        findTableAction(id: string): Chainable<HTMLElement>;

        /*
         * OVERRIDES
         *
         * Overwritten commands, which have changed typings.
         * *****************************************************************************/

        /**
         * Click a DOM element.
         *
         * @see https://on.cypress.io/click
         * @example
         *    cy.get('button').click()          // Click on button
         *    cy.focused().click()              // Click on el with focus
         *    cy.contains('Welcome').click()    // Click on first el containing 'Welcome'
         */
        click(options?: Partial<Cypress.ClickOptions & ClickableOptions>): Chainable<Subject>;
        /**
         * Click a DOM element at specific corner / side.
         *
         * @param {PositionType} position - The position where the click should be issued.
         * The `center` position is the default position.
         * @see https://on.cypress.io/click
         * @example
         *    cy.get('button').click('topRight')
         */
        click(position: PositionType, options?: Partial<Cypress.ClickOptions & ClickableOptions>): Chainable<Subject>;
        /**
         * Click a DOM element at specific coordinates
         *
         * @param {number} x The distance in pixels from the element's left to issue the click.
         * @param {number} y The distance in pixels from the element's top to issue the click.
         * @see https://on.cypress.io/click
         * @example
        ```
        // The click below will be issued inside of the element
        // (15px from the left and 40px from the top).
        cy.get('button').click(15, 40)
        ```
         */
        click(x: number, y: number, options?: Partial<Cypress.ClickOptions & ClickableOptions>): Chainable<Subject>;
    }
}
