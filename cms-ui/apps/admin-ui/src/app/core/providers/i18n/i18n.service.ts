import { Injectable } from '@angular/core';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { FALLBACK_LANGUAGE, UI_LANGUAGES } from '../../../common/config/config';
import { ServiceBase } from '../../../shared/providers/service-base/service.base';
import { applyShortcuts, translateParamsInstant } from './i18n-utils';

export interface JoinOptions {
    withLast?: boolean;
    quoted?: boolean;
    separator?: string;
}

const DEFAULT_JOIN_OPTIONS: JoinOptions = {
    withLast: true,
    quoted: false,
};


/**
 * Provides translation services using ngx-translate. Do not use
 * ngx-translate's TranslateService directly, use this service instead.
 */
@Injectable()
export class I18nService extends ServiceBase {

    constructor(private ngxTranslate: TranslateService) {
        super();
        ngxTranslate.setDefaultLang(FALLBACK_LANGUAGE);
    }

    /**
     * Set the UI language
     */
    setLanguage(language: GcmsUiLanguage): void {
        this.ngxTranslate.use(language);
    }

    /**
     * Translate the given key instantly.
     */
    instant(key: string | string[], params?: any): string {
        key = this.preProcessKey(key, params);
        const translatedParams = translateParamsInstant(params, this.ngxTranslate);
        return this.ngxTranslate.instant(key, translatedParams);
    }

    /**
     * Translate the given key.
     * @returns An observable that emits once immediately and
     * again whenever the current language is changed with the translation in the
     * respective language.
     */
    get(key: string | string[], params?: any): Observable<string> {
        const initialTranslation = this.instant(key, params);
        return this.ngxTranslate.onLangChange.pipe(
            map(() => this.instant(key, params)),
            startWith(initialTranslation),
        );
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

        if (navigator.languages) {
            const languages = navigator.languages.map(lang => lang.split('-')[0] as GcmsUiLanguage);
            for (const lang of languages) {
                if (UI_LANGUAGES.indexOf(lang) >= 0) {
                    return lang;
                }
            }
        }

        return FALLBACK_LANGUAGE;
    }

    join(parts: string[], options: JoinOptions = DEFAULT_JOIN_OPTIONS): string {
        if (!parts || parts.length < 1) {
            return '';
        }

        const partsCopy = [...parts];
        const { withLast, quoted, separator } = { ...DEFAULT_JOIN_OPTIONS, ...(options || {}) };

        if (parts.length === 1) {
            return parts[0];
        }

        let activeSeparator = separator ?? this.instant('shared.join_separator');
        const open = this.instant('shared.quote_open');
        const close = this.instant('shared.quote_close');

        if (quoted) {
            activeSeparator = close + activeSeparator + open;
        }

        if (!withLast) {
            const joined = partsCopy.join(activeSeparator);
            return quoted ? `${open}${joined}${close}` : joined;
        }

        const last = partsCopy.pop();
        let joined = partsCopy.join(activeSeparator);

        activeSeparator = quoted ? close + this.instant('shared.join_last_separator') + open : this.instant('shared.join_separator');
        joined += activeSeparator + last;

        return quoted ? `${open}${joined}${close}` : joined;
    }

    private preProcessKey(key: string | string[], params?: any): string | string[] {
        if (typeof key === 'string') {
            return applyShortcuts(key, params);
        }
        if (Array.isArray(key)) {
            return key.map(k => applyShortcuts(k));
        }
        return key;
    }

}
