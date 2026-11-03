// What a composer accepts as an attachment, checked when a file is added, before anything is sent.
// Fixed values: the contract's default limit (`openapi.yaml`, `Capabilities.upload_max_bytes`,
// "Default 26214400 (25 MB)") and the media types of the GenAIx mock (`genaix_mock/config.py`,
// `UPLOAD_MEDIA_TYPES`). An installation may configure others (`GET /me`, `capabilities`); GenAIx
// still enforces its own on upload (`413 file_too_large`, `unsupported_media_type`).

/** Largest file in bytes: 25 MB. */
export const UPLOAD_MAX_BYTES = 26_214_400;

/** Media types GenAIx accepts for session uploads. */
export const UPLOAD_MEDIA_TYPES = [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/markdown',
    'text/plain',
    'text/html',
    'application/json',
    'text/csv',
    'image/png',
    'image/jpeg',
    'image/webp',
    'application/zip',
] as const;

/**
 * The file input's `accept`: the media types, and their file name extensions, for a system that does
 * not know a type (`.md` is often not mapped to `text/markdown`).
 */
export const UPLOAD_ACCEPT = [
    ...UPLOAD_MEDIA_TYPES,
    '.pdf', '.docx', '.md', '.txt', '.html', '.htm', '.json', '.csv', '.png', '.jpg', '.jpeg', '.webp', '.zip',
].join(',');

/** Why a file cannot be attached, or `null` when it can. */
export type UploadProblem = { reason: 'tooLarge' } | { reason: 'typeNotAccepted'; type: string } | null;

/**
 * Checks `file` against the limits. A file whose type the browser does not know (`''`) passes: GenAIx
 * tells the type from the file itself.
 */
export function checkUploadFile(file: File): UploadProblem {
    if (file.size > UPLOAD_MAX_BYTES) {
        return { reason: 'tooLarge' };
    }

    if (file.type !== '' && !(UPLOAD_MEDIA_TYPES as readonly string[]).includes(file.type)) {
        return { reason: 'typeNotAccepted', type: file.type };
    }

    return null;
}

/** `bytes` as megabytes for a message, e.g. "31.2 MB" ("31,2 MB" in German). */
export function formatMegabytes(bytes: number, language: string): string {
    return `${new Intl.NumberFormat(language, { maximumFractionDigits: 1 }).format(bytes / 1_048_576)} MB`;
}
