import { TestBed } from '@angular/core/testing';
import { InterpolationParameters, LangChangeEvent, provideTranslateService, TranslateService } from '@ngx-translate/core';
import { I18nService } from '../../providers/i18n/i18n.service';
import { I18nPipe } from './i18n.pipe';
import { NEVER, Observable } from 'rxjs';

class MockTranslateService implements Partial<TranslateService> {
    get onLangChange(): Observable<LangChangeEvent> {
        return NEVER;
    }

    instant(key: string | string[], interpolateParams?: InterpolationParameters) {
        return `${key}_translated`;
    }
}

describe('I18nPipe', () => {

    let i18nPipe: I18nPipe;
    let spy: jasmine.Spy;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: TranslateService, useClass: MockTranslateService },
                I18nService,
            ],
        });

        const translateService = TestBed.inject(TranslateService);
        const i18n = TestBed.inject(I18nService);

        spy = spyOn(translateService, 'instant').and.callThrough();

        i18nPipe = new I18nPipe(i18n, {} as any);
    });

    afterEach(() => {
        I18nPipe.memoized = {};
    });

    it('passes key and params to TranslatePipe', () => {
        for (let i = 0; i < 100; i++) {
            const key = Math.random().toString(36);
            const params = {
                foo: i,
            };
            i18nPipe.transform(key, params);

            expect(spy).toHaveBeenCalledWith(key, params);
        }
    });

    it('applies shortcuts to common types', () => {
        const commonTypes = ['folder', 'page', 'file', 'image', 'tag', 'template', 'node', 'variant'];
        for (const type of commonTypes) {
            i18nPipe.transform(type, {});
            expect(spy).toHaveBeenCalledWith(`common.type_${type}`, {});
        }
    });

    it('applies shortcuts to common statuses', () => {
        const commonStatuses = ['published', 'edited', 'offline', 'queue', 'timeframe', 'publishat'];
        for (const status of commonStatuses) {
            i18nPipe.transform(status, {});
            expect(spy).toHaveBeenCalledWith(`common.status_${status}`, {});
        }
    });

    it('correctly pluralizes key for common types with count param', () => {
        i18nPipe.transform('folder', { count: -1 });
        expect(spy).toHaveBeenCalledWith('common.type_folders', { count: -1 });
        i18nPipe.transform('folder', { count: 0 });
        expect(spy).toHaveBeenCalledWith('common.type_folders', { count: 0 });
        i18nPipe.transform('folder', { count: 1 });
        expect(spy).toHaveBeenCalledWith('common.type_folder', { count: 1 });
        i18nPipe.transform('folder', { count: 2 });
        expect(spy).toHaveBeenCalledWith('common.type_folders', { count: 2 });
        i18nPipe.transform('folder', { count: 100 });
        expect(spy).toHaveBeenCalledWith('common.type_folders', { count: 100 });
        i18nPipe.transform('folder', { count: 1e34 });
        expect(spy).toHaveBeenCalledWith('common.type_folders', { count: 1e34 });
    });

    it('does not translate params with no leading underscore', () => {
        i18nPipe.transform('foo', { type: 'bar' });
        expect(spy).not.toHaveBeenCalledWith('bar');
        expect(spy).toHaveBeenCalledWith('foo', { type: 'bar' });
    });

    it('translates params with leading underscore', () => {
        i18nPipe.transform('foo', { _type: 'bar' });
        expect(spy).toHaveBeenCalledWith('bar');
        expect(spy).toHaveBeenCalledWith('foo', { type: 'bar_translated' });
    });

    it('uses shortcut and translates common type param with leading underscore', () => {
        i18nPipe.transform('foo', { _type: 'folder' });
        expect(spy).toHaveBeenCalledWith('common.type_folder');
        expect(spy).toHaveBeenCalledWith('foo', { type: 'common.type_folder_translated' });
    });

    it('uses shortcut and translates common type param with leading underscore and count', () => {
        i18nPipe.transform('foo', { _type: 'folder', count: 2 });
        expect(spy).toHaveBeenCalledWith('common.type_folders');
        expect(spy).toHaveBeenCalledWith('foo', { type: 'common.type_folders_translated', count: 2 });
    });

});
