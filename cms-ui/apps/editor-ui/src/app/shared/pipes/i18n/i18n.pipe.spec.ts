import {EventEmitter} from '@angular/core';
import {I18nPipe} from './i18n.pipe';

describe('I18nPipe', () => {

    let i18nPipe: I18nPipe;
    let mockTranslatePipe: MockTranslatePipe;
    let mockTranslateService: MockTranslateService;

    beforeEach(() => {
        mockTranslatePipe = new MockTranslatePipe();
        mockTranslateService = new MockTranslateService();
        i18nPipe = new I18nPipe(mockTranslateService as any, {} as any);
        i18nPipe.translatePipe = mockTranslatePipe as any;
    });

    afterEach(() => {
        I18nPipe.memoized = {};
    });

   it('passes key and params to TranslatePipe', () => {
        for (let i = 0; i < 100; i++) {
            let key = Math.random().toString(36);
            let params = {
                foo: i
            };
            i18nPipe.transform(key, params);

            expect(mockTranslatePipe.transform).toHaveBeenCalledWith(key, params);
        }
    });

    it('applies shortcuts to common types', () => {
        const commonTypes = ['folder', 'page', 'file', 'image', 'tag', 'template', 'node', 'variant'];
        for (let type of commonTypes) {
            i18nPipe.transform(type, {});
            expect(mockTranslatePipe.transform).toHaveBeenCalledWith(`common.type_${type}`, {});
        }
    });

    it('applies shortcuts to common statuses', () => {
        const commonStatuses = ['published', 'edited', 'offline', 'queue', 'timeframe', 'publishat'];
        for (let status of commonStatuses) {
            i18nPipe.transform(status, {});
            expect(mockTranslatePipe.transform).toHaveBeenCalledWith(`common.status_${status}`, {});
        }
    });

    it('correctly pluralizes key for common types with count param', () => {
        i18nPipe.transform('folder', { count: -1 });
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith(`common.type_folders`, { count: -1 });
        i18nPipe.transform('folder', { count: 0 });
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith(`common.type_folders`, { count: 0 });
        i18nPipe.transform('folder', { count: 1 });
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith(`common.type_folder`, { count: 1 });
        i18nPipe.transform('folder', { count: 2 });
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith(`common.type_folders`, { count: 2 });
        i18nPipe.transform('folder', { count: 100 });
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith(`common.type_folders`, { count: 100 });
        i18nPipe.transform('folder', { count: 1e34 });
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith(`common.type_folders`, { count: 1e34 });
    });

    it('does not translate params with no leading underscore', () => {
        i18nPipe.transform('foo', { type: 'bar' });
        expect(mockTranslateService.instant).not.toHaveBeenCalledWith('bar');
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith('foo', { type: 'bar' });
    });

    it('translates params with leading underscore', () => {
        i18nPipe.transform('foo', { _type: 'bar' });
        expect(mockTranslateService.instant).toHaveBeenCalledWith('bar');
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith('foo', { type: 'bar_translated' });
    });

    it('uses shortcut and translates common type param with leading underscore', () => {
        i18nPipe.transform('foo', { _type: 'folder' });
        expect(mockTranslateService.instant).toHaveBeenCalledWith('common.type_folder');
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith('foo', { type: 'common.type_folder_translated' });
    });

    it('uses shortcut and translates common type param with leading underscore and count', () => {
        i18nPipe.transform('foo', { _type: 'folder', count: 2 });
        expect(mockTranslateService.instant).toHaveBeenCalledWith('common.type_folders');
        expect(mockTranslatePipe.transform).toHaveBeenCalledWith('foo', { type: 'common.type_folders_translated', count: 2 });
    });

});

class MockTranslateService {
    instant = jasmine.createSpy('instant').and.callFake((key: string, params: Object) => {
        return `${key}_translated`;
    });
    onLangChange = new EventEmitter<any>();
}

class MockTranslatePipe {
    transform = jasmine.createSpy('transform').and.returnValue('bar');
    _dispose = jasmine.createSpy('_dispose');
}
