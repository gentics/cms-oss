import { inject, Injectable } from '@angular/core';
import { InterpolationParameters, TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { JoinOptions } from '../../../common/models';

const DEFAULT_JOIN_OPTIONS: JoinOptions = {
    withLast: true,
    quoted: false,
};

/**
 * Use this class instead of the TranslateService!
 * Wrapper for the TranslateService, to make helper functions available
 * and to have changes of the library in one place.
 */
@Injectable()
export class I18nService {

    private translate = inject(TranslateService);

    public getCurrentLanguage(): string {
        return this.translate.getCurrentLang();
    }

    public getAvailableLangues(): readonly string[] {
        return this.translate.getLangs();
    }

    public onLanguageChange(): Observable<string> {
        return this.translate.onLangChange.pipe(
            map((event) => event.lang),
        );
    }

    /**
     * Set the UI language
     */
    public setLanguage(language: string): void {
        this.translate.use(language);
    }

    /**
     * Translate the given key instantly.
     */
    public instant(key: string | string[], params?: InterpolationParameters): string {
        if (key == null) {
            return '';
        }

        if ((typeof key !== 'string' || key === '') && !Array.isArray(key)) {
            console.warn('Invalid translation key provided!', key);
            return '';
        }

        key = this.preProcessKey(key, params);
        const translatedParams = this.translateParamsInstant(params);
        return this.translate.instant(key, translatedParams);
    }

    /**
     * Translate the given key.
     * @returns An observable that emits once immediately and
     * again whenever the current language is changed with the translation in the
     * respective language.
     */
    public get(key: string | string[], params?: InterpolationParameters): Observable<string> {
        const initialTranslation = this.instant(key, params);
        return this.translate.onLangChange.pipe(
            map(() => this.instant(key, params)),
            startWith(initialTranslation),
        );
    }

    /**
     * Attempt to infer the user language from the browser's navigator object. If the result is not
     * amongst the valid UI languages, default to the fallback language instead.
     */
    public inferUserLanguage(): string {
        const browserLanguage = navigator.language.split('-')[0];
        const availableLangs = this.translate.getLangs();

        if (availableLangs.includes(browserLanguage)) {
            return browserLanguage;
        }

        if (navigator.languages) {
            const languages = navigator.languages.map((lang) => lang.split('-')[0]);
            for (const lang of languages) {
                if (availableLangs.includes(lang)) {
                    return lang;
                }
            }
        }

        return this.translate.getFallbackLang();
    }

    public join(parts: string[], options: JoinOptions = DEFAULT_JOIN_OPTIONS): string {
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
            return this.applyShortcuts(key, params);
        }
        if (Array.isArray(key)) {
            return key.map((k) => this.applyShortcuts(k));
        }
        return key;
    }

    /**
     * In many places we need to localize the page type and status. This method allows us to use the string `folder`
     * rather than `common.type_folder`. In the case of types, it also allows the use of a `count` param to create
     * the correct pluralized translation key.
     */
    public applyShortcuts(value: string, params?: InterpolationParameters): string {
        switch (value) {
            case 'contenttag':
            case 'file':
            case 'folder':
            case 'form':
            case 'image':
            case 'node':
            case 'object':
            case 'page':
            case 'tag':
            case 'template':
            case 'templateTag':
            case 'linkedPage':
            case 'linkedFile':
            case 'linkedImage':
            case 'variant':{
                let key = `common.type_${value}`;
                if (params && params.hasOwnProperty('count') && 1 !== params.count) {
                    key += 's';
                }
                return key;
            }
            case 'published':
            case 'edited':
            case 'offline':
            case 'queue':
            case 'timeframe':
            case 'publishat':
                return `common.status_${value}`;
            default:
                return value;
        }
    }

    public translateParamsInstant(params: InterpolationParameters): InterpolationParameters {
        const translated: { [key: string]: any } = {};
        for (const key in params) {
            if (key === '_lang' || key === '_language') {
                translated[key.substr(1)] = this.translate.instant('lang.' + params[key]);
            } else if (key[0] === '_') {
                translated[key.substr(1)] = this.translateParamValue(params[key], params);
            } else {
                translated[key] = params[key];
            }
        }
        return translated;
    }

    /**
     * If a param value is one of the common pages, we translate it implicitly.
     */
    private translateParamValue(value: any, params: InterpolationParameters): any {
        return this.translate.instant(this.applyShortcuts(value, params));
    }

}
