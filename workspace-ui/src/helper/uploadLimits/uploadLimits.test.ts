import { describe, expect, it } from 'vitest';

import { checkUploadFile, formatMegabytes, UPLOAD_ACCEPT, UPLOAD_MAX_BYTES } from './uploadLimits';

// A file of `size` bytes without allocating them.
function fileOfSize(name: string, type: string, size: number): File {
    const file = new File([''], name, { type });

    Object.defineProperty(file, 'size', { value: size });

    return file;
}

describe('checkUploadFile', () => {
    it('accepts a file of an accepted type up to the limit', () => {
        expect(checkUploadFile(fileOfSize('brief.pdf', 'application/pdf', UPLOAD_MAX_BYTES))).toBeNull();
    });

    it('refuses a file over the limit', () => {
        expect(checkUploadFile(fileOfSize('brief.pdf', 'application/pdf', UPLOAD_MAX_BYTES + 1))).toEqual({ reason: 'tooLarge' });
    });

    it('refuses a type GenAIx does not accept, and lets an unknown type through', () => {
        expect(checkUploadFile(fileOfSize('setup.exe', 'application/x-msdownload', 10))).toEqual({ reason: 'typeNotAccepted', type: 'application/x-msdownload' });
        expect(checkUploadFile(fileOfSize('notes.md', '', 10))).toBeNull();
    });
});

describe('UPLOAD_ACCEPT', () => {
    it('lists the media types and their extensions for the file input', () => {
        expect(UPLOAD_ACCEPT.split(',')).toEqual(expect.arrayContaining(['application/pdf', 'text/markdown', '.pdf', '.md', '.docx']));
    });
});

describe('formatMegabytes', () => {
    it('formats with one decimal in the language', () => {
        expect(formatMegabytes(32_715_571, 'en')).toBe('31.2 MB');
        expect(formatMegabytes(32_715_571, 'de')).toBe('31,2 MB');
        expect(formatMegabytes(UPLOAD_MAX_BYTES, 'en')).toBe('25 MB');
    });
});
