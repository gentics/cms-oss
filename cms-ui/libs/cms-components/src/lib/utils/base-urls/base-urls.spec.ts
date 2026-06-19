import { urlsRelativeTo } from './base-urls';

// The tests below represent known customer configurations. Add tests for un-covered cases.

describe('url resolving', () => {

    it('works for domain/.Node/ui/', () => {
        const result = urlsRelativeTo('/.Node/ui/');

        expect(result.alohaPage).toBe('/alohapage');
        expect(result.imageStore).toBe('/GenticsImageStore');
        expect(result.restAPI).toBe('/rest');
    });

    it('works for domain/.Node/ui/index.html', () => {
        const result = urlsRelativeTo('/.Node/ui/index.html');

        expect(result.alohaPage).toBe('/alohapage');
        expect(result.imageStore).toBe('/GenticsImageStore');
        expect(result.restAPI).toBe('/rest');
    });

    it('works for domain/ui/', () => {
        const result = urlsRelativeTo('/ui/');

        expect(result.alohaPage).toBe('/alohapage');
        expect(result.imageStore).toBe('/GenticsImageStore');
        expect(result.restAPI).toBe('/rest');
    });

    it('works for domain/ui/index.html', () => {
        const result = urlsRelativeTo('/ui/index.html');

        expect(result.alohaPage).toBe('/alohapage');
        expect(result.imageStore).toBe('/GenticsImageStore');
        expect(result.restAPI).toBe('/rest');
    });

    it('works for domain/', () => {
        const result = urlsRelativeTo('/');

        expect(result.alohaPage).toBe('/alohapage');
        expect(result.imageStore).toBe('/GenticsImageStore');
        expect(result.restAPI).toBe('/rest');
    });

    it('works for domain/with/subfolders/.Node/ui/', () => {
        const result = urlsRelativeTo('/with/subfolders/.Node/ui/');

        expect(result.alohaPage).toBe('/with/subfolders/alohapage');
        expect(result.imageStore).toBe('/with/subfolders/GenticsImageStore');
        expect(result.restAPI).toBe('/with/subfolders/rest');
    });

    it('works for domain/with/subfolders/.Node/ui/index.html', () => {
        const result = urlsRelativeTo('/with/subfolders/.Node/ui/index.html');

        expect(result.alohaPage).toBe('/with/subfolders/alohapage');
        expect(result.imageStore).toBe('/with/subfolders/GenticsImageStore');
        expect(result.restAPI).toBe('/with/subfolders/rest');
    });
});
