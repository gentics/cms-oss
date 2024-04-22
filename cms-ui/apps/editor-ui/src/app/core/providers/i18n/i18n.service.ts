import { Injectable } from '@angular/core';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { TranslateService } from '@ngx-translate/core';
import { FALLBACK_LANGUAGE, UI_LANGUAGES } from '../../../common/config/config';
import { applyShortcuts, translateParams } from './i18n-utils';

@Injectable()
export class I18nService {

    constructor(private ngxTranslate: TranslateService) {
        ngxTranslate.setDefaultLang(FALLBACK_LANGUAGE);
    }

    /**
     * Set the UI language
     */
    setLanguage(language: GcmsUiLanguage): void {
        this.ngxTranslate.use(language);
    }

    /**
     * Translate the given key.
     */
    translate(key: string | string[], params?: any): string {
        let shortcutKeys: any;
        if (typeof key === 'string') {
            shortcutKeys = applyShortcuts(key, params);
        } else {
            shortcutKeys = key.map(k => applyShortcuts(k));
        }
        let translatedParams = translateParams(params, this.ngxTranslate);
        return this.ngxTranslate.instant(shortcutKeys, translatedParams);
    }

    /**
     * Attempt to infer the user language from the browser's navigator object. If the result is not
     * amongst the valid UI languages, default to the fallback language instead.
     */
    inferUserLanguage(): GcmsUiLanguage {
        const browserLanguage = navigator.language.split('-')[0] as GcmsUiLanguage;
        if (UI_LANGUAGES.indexOf(browserLanguage) >= 0) {
            return browserLanguage;
        }

        if ((<any> navigator).languages) {
            const languages: string[] = (<any> navigator).languages;
            for (let lang of languages.map(lang => lang.split('-')[0]) as GcmsUiLanguage[]) {
                if (UI_LANGUAGES.indexOf(lang) >= 0) {
                    return lang;
                }
            }
        }

        return FALLBACK_LANGUAGE;
    }
}
