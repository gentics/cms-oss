import { TestBed } from '@angular/core/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { takeUntil } from 'rxjs/operators';

import { FALLBACK_LANGUAGE } from '../../../common/config/config';
import { ObservableStopper } from '../../../common/utils/observable-stopper/observable-stopper';
import { I18nService } from './i18n.service';

describe('I18nService', () => {

    let i18n: I18nService;
    let ngxTranslate: TranslateService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                TranslateModule.forRoot(),
            ],
            providers: [
                I18nService,
            ],
        });

        ngxTranslate = TestBed.get(TranslateService);
        spyOn(ngxTranslate, 'instant').and.callFake((key: string) => {
            return `${key}_translated_${ngxTranslate.currentLang}`;
        });
        spyOn(ngxTranslate, 'setDefaultLang').and.callThrough();
        ngxTranslate.use('en');

        i18n = TestBed.get(I18nService);
    });

    describe('instant()', () => {

        it('passes key and params to TranslateService', () => {
            for (let i = 0; i < 100; i++) {
                const key = Math.random().toString(36);
                const params = {
                    foo: i,
                };
                i18n.instant(key, params);

                expect(ngxTranslate.instant).toHaveBeenCalledWith(key, params);
            }
        });

        it('applies shortcuts to common types', () => {
            const commonTypes = ['folder', 'page', 'file', 'image', 'tag', 'template', 'node', 'variant'];
            for (const type of commonTypes) {
                i18n.instant(type, {});
                expect(ngxTranslate.instant).toHaveBeenCalledWith(`common.type_${type}`, {});
            }
        });

        it('applies shortcuts to common statuses', () => {
            const commonStatuses = ['published', 'edited', 'offline', 'queue', 'timeframe', 'publishat'];
            for (const status of commonStatuses) {
                i18n.instant(status, {});
                expect(ngxTranslate.instant).toHaveBeenCalledWith(`common.status_${status}`, {});
            }
        });

        it('correctly pluralizes key for common types with count param', () => {
            i18n.instant('folder', { count: -1 });
            expect(ngxTranslate.instant).toHaveBeenCalledWith(`common.type_folders`, { count: -1 });
            i18n.instant('folder', { count: 0 });
            expect(ngxTranslate.instant).toHaveBeenCalledWith(`common.type_folders`, { count: 0 });
            i18n.instant('folder', { count: 1 });
            expect(ngxTranslate.instant).toHaveBeenCalledWith(`common.type_folder`, { count: 1 });
            i18n.instant('folder', { count: 2 });
            expect(ngxTranslate.instant).toHaveBeenCalledWith(`common.type_folders`, { count: 2 });
            i18n.instant('folder', { count: 100 });
            expect(ngxTranslate.instant).toHaveBeenCalledWith(`common.type_folders`, { count: 100 });
            i18n.instant('folder', { count: 1e34 });
            expect(ngxTranslate.instant).toHaveBeenCalledWith(`common.type_folders`, { count: 1e34 });
        });

        it('does not translate params with no leading underscore', () => {
            i18n.instant('foo', { type: 'bar' });
            expect(ngxTranslate.instant).not.toHaveBeenCalledWith('bar');
            expect(ngxTranslate.instant).toHaveBeenCalledWith('foo', { type: 'bar' });
        });

        it('translates params with leading underscore', () => {
            i18n.instant('foo', { _type: 'bar' });
            expect(ngxTranslate.instant).toHaveBeenCalledWith('bar');
            expect(ngxTranslate.instant).toHaveBeenCalledWith('foo', { type: 'bar_translated_en' });
        });

        it('uses shortcuts and translates common type param with leading underscore', () => {
            i18n.instant('foo', { _type: 'folder' });
            expect(ngxTranslate.instant).toHaveBeenCalledWith('common.type_folder');
            expect(ngxTranslate.instant).toHaveBeenCalledWith('foo', { type: 'common.type_folder_translated_en' });
        });

        it('uses shortcuts and translates common type param with leading underscore and count', () => {
            i18n.instant('foo', { _type: 'folder', count: 2 });
            expect(ngxTranslate.instant).toHaveBeenCalledWith('common.type_folders');
            expect(ngxTranslate.instant).toHaveBeenCalledWith('foo', { type: 'common.type_folders_translated_en', count: 2 });
        });

    });

    describe('get()', () => {

        let instantSpy: jasmine.Spy;
        let stopper: ObservableStopper;

        beforeEach(() => {
            instantSpy = spyOn(i18n, 'instant').and.callThrough();
            stopper = new ObservableStopper();
        });

        afterEach(() => {
            stopper.stop();
        });

        it('the returned observable immediately emits the translation in the current language', () => {
            const translation$ = i18n.get('testKey', { type: 'param' }).pipe(
                takeUntil(stopper.stopper$),
            );
            let translation: string;
            translation$.subscribe(val => translation = val);

            expect(ngxTranslate.instant).toHaveBeenCalledTimes(1);
            expect(ngxTranslate.instant).toHaveBeenCalledWith('testKey', { type: 'param' });
            expect(translation).toEqual('testKey_translated_en');
        });

        it('the returned observable emits the new translation whenever the language changes', () => {
            const translation$ = i18n.get('testKey', { type: 'param' }).pipe(
                takeUntil(stopper.stopper$),
            );
            let translation: string;
            translation$.subscribe(val => translation = val);

            expect(ngxTranslate.instant).toHaveBeenCalledTimes(1);
            expect(ngxTranslate.instant).toHaveBeenCalledWith('testKey', { type: 'param' });
            expect(translation).toEqual('testKey_translated_en');

            // Change the language to German.
            i18n.setLanguage('de');
            expect(ngxTranslate.instant).toHaveBeenCalledTimes(2);
            expect(ngxTranslate.instant).toHaveBeenCalledWith('testKey', { type: 'param' });
            expect(translation).toEqual('testKey_translated_de');

            // Change the language back to English.
            i18n.setLanguage('en');
            expect(ngxTranslate.instant).toHaveBeenCalledTimes(3);
            expect(ngxTranslate.instant).toHaveBeenCalledWith('testKey', { type: 'param' });
            expect(translation).toEqual('testKey_translated_en');
        });

    });

    describe('inferUserLanguage()', () => {

        let originalNavigator: Navigator;
        let mockNavigator: { language: string; languages: string[] };
        let navigatorIsWritable: boolean;

        beforeEach(() => {
            originalNavigator = window.navigator;
            mockNavigator = {
                language: '',
                languages: [],
            };
            navigatorIsWritable = Object.getOwnPropertyDescriptor(window, 'navigator').writable;
            if (navigatorIsWritable) {
                (window as any).navigator = mockNavigator;
            } else {
                Object.defineProperty(window, 'navigator', {
                    configurable: true,
                    enumerable: true,
                    value: mockNavigator,
                    writable: false,
                });
            }
        });
        afterEach(() => {
            if (navigatorIsWritable) {
                (window as any).navigator = originalNavigator;
            } else {
                Object.defineProperty(window, 'navigator', {
                    configurable: true,
                    enumerable: true,
                    value: originalNavigator,
                    writable: false,
                });
            }
        });

        it('returns "en" when the browser reports "en"', () => {
            mockNavigator.language = 'en';
            expect(i18n.inferUserLanguage()).toBe('en');
        });

        it('returns "en" when the browser reports "en-GB"', () => {
            mockNavigator.language = 'en-GB';
            expect(i18n.inferUserLanguage()).toBe('en');
        });

        it('returns "en" when the browser reports "en-US"', () => {
            mockNavigator.language = 'en-US';
            expect(i18n.inferUserLanguage()).toBe('en');
        });

        it('returns "de" when the browser reports "de"', () => {
            mockNavigator.language = 'de';
            expect(i18n.inferUserLanguage()).toBe('de');
        });

        it('returns "de" when the browser reports "de-DE"', () => {
            mockNavigator.language = 'de-DE';
            expect(i18n.inferUserLanguage()).toBe('de');
        });

        it('returns "de" when the browser reports "de-AT"', () => {
            mockNavigator.language = 'de-AT';
            expect(i18n.inferUserLanguage()).toBe('de');
        });

        it('returns "en" when the browser reports ["fr", "en"]', () => {
            mockNavigator.language = 'fr';
            mockNavigator.languages = ['fr', 'en'];
            expect(i18n.inferUserLanguage()).toBe('en');
        });

        it('returns "en" when the browser reports ["fr", "en-US"]', () => {
            mockNavigator.language = 'fr';
            mockNavigator.languages = ['fr', 'en-US'];
            expect(i18n.inferUserLanguage()).toBe('en');
        });

        it('returns "de" when the browser reports ["it", "de"]', () => {
            mockNavigator.language = 'it';
            mockNavigator.languages = ['it', 'de'];
            expect(i18n.inferUserLanguage()).toBe('de');
        });

        it('returns "de" when the browser reports ["it", "de-AT"]', () => {
            mockNavigator.language = 'it';
            mockNavigator.languages = ['it', 'de-AT'];
            expect(i18n.inferUserLanguage()).toBe('de');
        });

        it(`defaults to "${FALLBACK_LANGUAGE}"`, () => {
            mockNavigator.languages = undefined;
            mockNavigator.language = 'es'; // Spanish
            expect(i18n.inferUserLanguage()).toBe(FALLBACK_LANGUAGE);

            mockNavigator.language = 'fr'; // French
            expect(i18n.inferUserLanguage()).toBe(FALLBACK_LANGUAGE);

            mockNavigator.language = 'eo'; // Esperanto
            expect(i18n.inferUserLanguage()).toBe(FALLBACK_LANGUAGE);

            mockNavigator.language = 'tlh'; // Klingon
            expect(i18n.inferUserLanguage()).toBe(FALLBACK_LANGUAGE);
        });

    });

});
