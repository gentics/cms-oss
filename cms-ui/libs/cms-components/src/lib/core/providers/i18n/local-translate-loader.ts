import { IndexByKey } from '@gentics/cms-models';
import { TranslateLoader } from '@ngx-translate/core';
import { Observable, of as observableOf } from 'rxjs';

import { Injectable } from '@angular/core';

import * as COMMON_TRANSLATIONS from './translations/common.translations.json';
import * as USER_TRANSLATIONS from './translations/user.translations.json';

function getTranslations(jsonModule: any): any {
    return jsonModule.default;
}

const ALL_TRANSLATIONS = {
    common: getTranslations(COMMON_TRANSLATIONS),
    user: getTranslations(USER_TRANSLATIONS),
};

/** Translations for a single language. */
interface SingleLanguageTranslationsSet {
    [section: string]: IndexByKey<string>;
}

// Use a 'cc' prefix for the translations
const I18N_PREFIX = 'cc';

@Injectable()
export class LocalTranslateLoader implements TranslateLoader {

    /**
     * Gets the translation object for the specified language.
     */
    getTranslation(lang: string): Observable<any> {
        return observableOf(this.getTranslationsForLanguage(lang));
    }

    /*
     * Export translation object for the specified language
     */
    exportTranslations(lang: string): SingleLanguageTranslationsSet {
        const translation = {};

        translation[I18N_PREFIX] = this.getTranslationsForLanguage(lang);

        return translation;
    }

    private getTranslationsForLanguage(lang: string): SingleLanguageTranslationsSet {
        const translation: SingleLanguageTranslationsSet = {};

        Object.keys(ALL_TRANSLATIONS).forEach(section => {
            const srcSection = ALL_TRANSLATIONS[section];
            const destSection: IndexByKey<string> = {};

            Object.keys(srcSection).forEach(key => {
                destSection[key] = srcSection[key][lang];
            });

            translation[section] = destSection;
        });

        return translation;
    }

}
