// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

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

interface BinaryLoadOptions {
    applyAlias?: boolean;
}

interface BinaryFileLoadOptions extends BinaryLoadOptions {}

interface BinaryContentFileLoadOptions extends BinaryLoadOptions {
    asContent: true;
}

// eslint-disable-next-line @typescript-eslint/no-namespace
declare namespace Cypress {
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
    }
}

/**
 * Gets the base-name of the file: `folder1/folder2/myFile.txt` -> `myFile.txt`
 */
function toBaseName(fixture: string): string {
    return /((?:^[^/]$|(?:[^/]*$)))/g.exec(fixture)?.[1] || '';
}

function getExtension(fileName: string): string {
    return /\.(\w+)$/g.exec(fileName)?.[1] || fileName;
}

// Partial list from https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
// Should be removed with the mime-type checks/agnostics, which are done in the `upload-conflict.service`,
// as mime-types are inherently unreliable and just cause testing overhead here.
const EXT_MIME_MAP: Record<string, string> = {
    apng: 'image/apng',
    avi: 'video/x-msvideo',
    bin: 'application/octet-stream',
    bmp: 'image/bmp',
    bz: 'application/x-bzip',
    bz2: 'application/x-bzip2',
    css: 'text/css',
    csv: 'text/csv',
    doc: 'application/msword',
    docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    gz: 'application/gzip',
    gif: 'image/gif',
    htm: 'text/html',
    html: 'text/html',
    ico: 'image/vnd.microsoft.icon',
    jpeg: 'image/jpeg',
    jpg: 'image/jpeg',
    json: 'application/json',
    mp3: 'audio/mpeg',
    mp4: 'video/mp4',
    mpeg: 'video/mpeg',
    ogg: 'audio/ogg',
    ogv: 'video/ogg',
    png: 'image/png',
    pdf: 'application/pdf',
    ppt: 'application/vnd.ms-powerpoint',
    pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    rar: 'application/vnd.rar',
    rtf: 'application/rtf',
    svg: 'image/svg+xml',
    tar: 'application/x-tar',
    tif: 'image/tiff',
    tiff: 'image/tiff',
    txt: 'text/plain',
    wav: 'audio/wav',
    weba: 'audio/webm',
    webm: 'video/webm',
    webp: 'image/webp',
    xls: 'application/vnd.ms-excel',
    xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    xml: 'application/xml',
    zip: 'application/zip',
    '7z': 'application/x-7x-compressed',
};

function normalizeToImportBinary(fixturePath: string | ImportBinary): Required<ImportBinary> {
    if (typeof fixturePath !== 'string') {
        if (!fixturePath.name) {
            fixturePath.name = toBaseName(fixturePath.fixturePath);
        }
        return fixturePath as Required<ImportBinary>;
    }

    const name = toBaseName(fixturePath);
    const type = EXT_MIME_MAP[getExtension(name)] || 'unknown/unknown';

    return {
        fixturePath,
        name,
        type,
    };
}

Cypress.Commands.add('muteXHR', () => {
    // Disable logging of XHR/Fetch requests, since they just spam everything
    return cy.intercept({ resourceType: /xhr|fetch/ }, { log: false });
});

Cypress.Commands.add('loadBinaries', (files, options) => {
    return cy.wrap(new Promise(resolve => {
        let counter = files.length;
        const map: Record<string, File | ContentFile> = {};

        for (const entry of files) {
            const data = normalizeToImportBinary(entry);
            // eslint-disable-next-line cypress/no-assigning-return-values
            let chain = cy.fixture(data.fixturePath, null);

            if (options?.applyAlias) {
                // Check if it is defined before attempting to save it again
                if (this[data.fixturePath] == null) {
                    chain = chain.as(data.fixturePath);
                }
            }

            chain.then((bin: Buffer) => {
                if (options?.asContent) {
                    map[data.fixturePath] = {
                        contents: bin.buffer,
                        fileName: data.name,
                        mimeType: data.type,
                    } as ContentFile;
                } else {
                    const blob = new Blob([bin], { type: data.type });
                    // Create the file with the binary data, and the correct name, and type.
                    map[data.fixturePath] = new File([blob], data.name, { type: data.type });
                }

                // Cypress is stupid, and doesn't chain the commands in the correct order.
                // It'd resolve with an empty map, and then execute this `then` block.
                // Therefore this hacky promise to wait for all fixtures to be properly loaded.
                counter--;
                if (counter === 0) {
                    resolve(map);
                }
            });
        }
    }));
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
    const root = subject ? cy.wrap(subject) : cy.get('folder-contents');
    root.find('node-selector [data-action="select-node"]')
        .click();
    cy.get('gtx-app-root .node-selector-list')
        .find(`[data-id="${nodeId}"], [data-global-id="${nodeId}"]`)
        .click();
    return cy.wrap(null);
});

Cypress.Commands.add('findList', { prevSubject: 'optional' }, (subject, type) => {
    const root = subject ? cy.wrap(subject) : cy.get('folder-contents');
    return root.find(`item-list .content-list[data-item-type="${type}"]`);
});

Cypress.Commands.add('findItem', { prevSubject: 'element' }, (subject, id) => {
    return cy.wrap(subject)
        .find(`gtx-contents-list-item[data-id="${id}"], masonry-item[data-id="${id}"]`);
});

Cypress.Commands.add('itemAction', { prevSubject: 'element' }, (subject, action) => {
    switch (action) {
        // For other actions such as selecting or similar
        default:
            cy.wrap(subject)
                .find('.context-menu gtx-button[data-action="open-item-context-menu"]')
                .click({ force: true });
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

            return cy.wait('@folderLoad').then(() => cy.wrap(output));
        })
    });
});

Cypress.Commands.add('selectValue', { prevSubject: 'element' }, (subject, valueId) => {
    cy.wrap(subject).click({ force: true });
    cy.get('gtx-dropdown-content.select-context')
        .find(`.select-option[data-id="${valueId}"]`)
        .click({ force: true });
    return cy.wrap(null);
});

Cypress.Commands.add('editorSave', { prevSubject: false }, () => {
    cy.get('content-frame gtx-editor-toolbar .save-button [data-action="primary"]')
        .click({ force: true });
    return cy.wrap(null);
});

Cypress.Commands.add('editorClose', { prevSubject: false }, () => {
    cy.get('content-frame gtx-editor-toolbar [data-action="close"]')
        .click({ force: true });
    return cy.wrap(null);
});
