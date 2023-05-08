import { Inject, Injectable, InjectionToken, OnDestroy } from '@angular/core';
import { IndexByKey } from '@gentics/cms-models';
import { GcmsUiLanguage as UILanguage } from '@gentics/cms-models';
import { TranslateService } from '@ngx-translate/core';
import { Observable, Subscription } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { FALLBACK_LANGUAGE, UI_LANGUAGES } from '../../../common/config/config';
import { ServiceBase } from '../service-base';
import { applyShortcuts, translateParamsInstant } from './i18n-utils';
import { LocalTranslateLoader } from './local-translate-loader';

/**
 * This injection token is used to provide an Observable for the session ID to be used in the GCMS API requests.
 */
export const GCMS_COMMON_LANGUAGE = new InjectionToken<Observable<UILanguage>>('GCMS_COMMON_LANGUAGE');

export { GcmsUiLanguage as UILanguage } from '@gentics/cms-models';

export interface ParametizedI18nKey {
    /** The i18n key that will be passed to the I18nService. */
    key: string;

    /** Parameters that should be passed to the I18nService. */
    params: IndexByKey<any>;
}

/** Type alias for specifying that a parameter or property needs to be set to an i18n key. */
export type I18nKey = string | ParametizedI18nKey;

/**
 * Provides translation services using ngx-translate. Do not use
 * ngx-translate's TranslateService directly, use this service instead.
 */
@Injectable()
export class I18nService extends ServiceBase implements OnDestroy {

    private subscriptions = new Subscription();

    readonly language$: Observable<UILanguage>;

    constructor(
        private ngxTranslate: TranslateService,
        private loader: LocalTranslateLoader,
        @Inject(GCMS_COMMON_LANGUAGE) language$: Observable<UILanguage>,
    ) {
        super();

        this.loadTranslations();

        ngxTranslate.setDefaultLang(FALLBACK_LANGUAGE);

        this.language$ = language$;

        // Set language based on the passed language
        this.subscriptions.add(
            language$.subscribe(language => this.setLanguage(language)),
        );
    }

    loadTranslations(): void {
        UI_LANGUAGES.forEach((lang) => {
            this.ngxTranslate.setTranslation(lang, this.loader.exportTranslations(lang), true);
        });
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    /**
     * Set the UI language
     */
    setLanguage(language: UILanguage): void {
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
     *
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
    inferUserLanguage(): UILanguage {
        const browserLanguage = navigator.language.split('-')[0] ;
        if (UI_LANGUAGES.indexOf(browserLanguage) >= 0) {
            return browserLanguage;
        }

        if (navigator.languages) {
            const languages = navigator.languages.map(lang => lang.split('-')[0] );
            for (const lang of languages) {
                if (UI_LANGUAGES.indexOf(lang) >= 0) {
                    return lang;
                }
            }
        }

        return FALLBACK_LANGUAGE;
    }

    private preProcessKey(key: string | string[], params?: any): string | string[] {
        if (typeof key === 'string') {
            return applyShortcuts(key, params);
        } else {
            return key.map(k => applyShortcuts(k));
        }
    }

}
