import {I18nService} from './i18n.service';
import {FALLBACK_LANGUAGE} from '../../../common/config/config';

describe('I18nService', () => {

    let i18n: I18nService;
    let mockTranslateService: MockTranslateService;

    beforeEach(() => {
        mockTranslateService = new MockTranslateService();
        i18n = new I18nService(mockTranslateService as any);
    });

    it('passes key and params to TranslateService', () => {
        for (let i = 0; i < 100; i++) {
            let key = Math.random().toString(36);
            let params = {
                foo: i
            };
            i18n.translate(key, params);

            expect(mockTranslateService.instant).toHaveBeenCalledWith(key, params);
        }
    });

    it('applies shortcuts to common types', () => {
        const commonTypes = ['folder', 'page', 'file', 'image', 'tag', 'template', 'node', 'variant'];
        for (let type of commonTypes) {
            i18n.translate(type, {});
            expect(mockTranslateService.instant).toHaveBeenCalledWith(`common.type_${type}`, {});
        }
    });

    it('applies shortcuts to common statuses', () => {
        const commonStatuses = ['published', 'edited', 'offline', 'queue', 'timeframe', 'publishat'];
        for (let status of commonStatuses) {
            i18n.translate(status, {});
            expect(mockTranslateService.instant).toHaveBeenCalledWith(`common.status_${status}`, {});
        }
    });

    it('correctly pluralizes key for common types with count param', () => {
        i18n.translate('folder', { count: -1 });
        expect(mockTranslateService.instant).toHaveBeenCalledWith(`common.type_folders`, { count: -1 });
        i18n.translate('folder', { count: 0 });
        expect(mockTranslateService.instant).toHaveBeenCalledWith(`common.type_folders`, { count: 0 });
        i18n.translate('folder', { count: 1 });
        expect(mockTranslateService.instant).toHaveBeenCalledWith(`common.type_folder`, { count: 1 });
        i18n.translate('folder', { count: 2 });
        expect(mockTranslateService.instant).toHaveBeenCalledWith(`common.type_folders`, { count: 2 });
        i18n.translate('folder', { count: 100 });
        expect(mockTranslateService.instant).toHaveBeenCalledWith(`common.type_folders`, { count: 100 });
        i18n.translate('folder', { count: 1e34 });
        expect(mockTranslateService.instant).toHaveBeenCalledWith(`common.type_folders`, { count: 1e34 });
    });

    it('does not translate params with no leading underscore', () => {
        i18n.translate('foo', { type: 'bar' });
        expect(mockTranslateService.instant).not.toHaveBeenCalledWith('bar');
        expect(mockTranslateService.instant).toHaveBeenCalledWith('foo', { type: 'bar' });
    });

    it('translates params with leading underscore', () => {
        i18n.translate('foo', { _type: 'bar' });
        expect(mockTranslateService.instant).toHaveBeenCalledWith('bar');
        expect(mockTranslateService.instant).toHaveBeenCalledWith('foo', { type: 'bar_translated' });
    });

    it('uses shortcuts and translates common type param with leading underscore', () => {
        i18n.translate('foo', { _type: 'folder' });
        expect(mockTranslateService.instant).toHaveBeenCalledWith('common.type_folder');
        expect(mockTranslateService.instant).toHaveBeenCalledWith('foo', { type: 'common.type_folder_translated' });
    });

    it('uses shortcuts and translates common type param with leading underscore and count', () => {
        i18n.translate('foo', { _type: 'folder', count: 2 });
        expect(mockTranslateService.instant).toHaveBeenCalledWith('common.type_folders');
        expect(mockTranslateService.instant).toHaveBeenCalledWith('foo', { type: 'common.type_folders_translated', count: 2 });
    });

    describe('inferUserLanguage()', () => {

        let originalNavigator: Navigator;
        let mockNavigator: { language: string; languages: string[] };
        let navigatorIsWritable: boolean;

        beforeEach(() => {
            originalNavigator = window.navigator;
            mockNavigator = {
                language: '',
                languages: []
            };
            if (navigatorIsWritable = Object.getOwnPropertyDescriptor(window, 'navigator').writable) {
                (window as any).navigator = mockNavigator;
            } else {
                Object.defineProperty(window, 'navigator', {
                    configurable: true,
                    enumerable: true,
                    value: mockNavigator,
                    writable: false
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
                    writable: false
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

class MockTranslateService {
    instant = jasmine.createSpy('instant').and.callFake((key: string) => {
        return `${key}_translated`;
    });
    setDefaultLang = jasmine.createSpy('setDefaultLang');
}
