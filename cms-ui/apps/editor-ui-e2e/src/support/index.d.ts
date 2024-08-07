import { ContentFile, ImportBinary, ItemType } from './models';

declare namespace Cypress {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface Chainable<Subject> {
        /** Loads the defined fixtures and returns a map of the loaded binaries as usable map. */
        loadBinaries(files: (string | ImportBinary)[], options?: BinaryLoadOptions): Chainable<Record<string, ContentFile | File>>;
        /** Helper to navigate to the application */
        navigateToApp(path?: string): Chainable<void>;
        /** Login with pre-defined user data or with a cypress alias */
        login(account: string): Chainable<void>;
        /** Select a certain node in the editor-ui */
        selectNode(nodeId: number | string): Chainable<JQuery<HTMLElement>>;
        /** Attempt to find a specified item-type list */
        findList(type: ItemType): Chainable<JQuery<HTMLElement>>;
        /** Attempt to find a specified item of a type in the item-type list (uses `findList`) */
        findItem(type: ItemType, id: number): Chainable<JQuery<HTMLElement>>;
        /** Click/Perform an action on an item (iE edit, preview, delete, ...) */
        itemAction(type: ItemType, id: number, action: string): Chainable<JQuery<HTMLElement>>;
        /** Select the provided object-property - Requires the `editProperties` mode to be active for the item already. */
        openObjectPropertyEditor(name: string): Chainable<JQuery<HTMLElement>>;
        /** Finds the tag-editor element(s) which are for controlling the tag value */
        findTagEditorElement(type: string): Chainable<JQuery<HTMLElement>>;
        /** Uploads the specified fixture-names as files or images */
        uploadFiles(type: 'file' | 'image', fixtureNames: (string | ImportBinary)[], dragNDrop?: boolean): Chainable<Record<string, any>>;
        /** Selects the specified value in the select subject */
        selectValue(valueId: any): Chainable<void>;
    }
}
