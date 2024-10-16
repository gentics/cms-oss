import { BinaryContentFileLoadOptions, BinaryFileLoadOptions, ContentFile, ImportBinary } from './common';

/**
 * Gets the base-name of the file: `folder1/folder2/myFile.txt` -> `myFile.txt`
 */
export function toBaseName(fixture: string): string {
    return /((?:^[^/]$|(?:[^/]*$)))/g.exec(fixture)?.[1] || '';
}

export function getExtension(fileName: string): string {
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

export function normalizeToImportBinary(fixturePath: string | ImportBinary): Required<ImportBinary> {
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


export function resolveFixtures(files: (string | ImportBinary)[], options?: BinaryFileLoadOptions): Cypress.Chainable<Record<string, File>>;
export function resolveFixtures(files: (string | ImportBinary)[], options?: BinaryContentFileLoadOptions): Cypress.Chainable<Record<string, ContentFile>>;
export function resolveFixtures(
    files: (string | ImportBinary)[],
    options?: BinaryFileLoadOptions | BinaryContentFileLoadOptions,
): Cypress.Chainable<Record<string, File | ContentFile>> {
    return cy.wrap(new Promise<Record<string, File | ContentFile>>(resolve => {
        let counter = files.length;
        const map: Record<string, File | ContentFile> = {};

        for (const entry of files) {
            const data = normalizeToImportBinary(entry);
            let chain = cy.fixture(data.fixturePath, null);

            if (options?.applyAlias) {
                chain = chain.as(data.fixturePath);
            }

            chain.then((bin: Buffer) => {
                if ((options as BinaryContentFileLoadOptions)?.asContent) {
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
}
