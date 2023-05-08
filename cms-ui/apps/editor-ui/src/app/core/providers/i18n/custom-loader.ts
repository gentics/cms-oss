import { Inject, Injectable, Optional } from '@angular/core';
import { GTX_TOKEN_EXTENDED_TRANSLATIONS } from '@gentics/cms-components';
import { TranslateLoader } from '@ngx-translate/core';
import { Observable } from 'rxjs';

// Parse the yaml files into a JS object.
const CORE_TRANSLATIONS: any = {
    common: require('./translations/common.translations.yml'),
    date: require('./translations/date.translations.yml'),
    editor: require('./translations/editor.translations.yml'),
    lang: require('./translations/lang.translations.yml'),
    message: require('./translations/message.translations.yml'),
    modal: require('./translations/modal.translations.yml'),
    tag_editor: require('./translations/tag-editor.translations.yml'),
    user: require('./translations/user.translations.yml'),
    search: require('./translations/search.translations.yml'),
    template: require('./translations/template.translations.yml'),
};

/**
 * A custom language loader which splits apart a translations object in the format:
 * {
 *   SECTION: {
 *     TOKEN: {
 *       lang1: "...",
 *       lang2: "....
 *     }
 *   }
 * }
 */
@Injectable()
export class CustomLoader implements TranslateLoader {

    private allTranslations: {
        [key: string]: {
            default: {
                [key: string]: {
                    [key: string]: string;
                };
            };
        };
    };

    constructor(
        @Optional()
        @Inject(GTX_TOKEN_EXTENDED_TRANSLATIONS)
        private readonly extendedTranslations: JSON[],
    ) {
        this.allTranslations = CORE_TRANSLATIONS;
        if (Array.isArray(this.extendedTranslations) && this.extendedTranslations.length > 0) {
            Object.assign(this.allTranslations, ...this.extendedTranslations);
        }
    }

    getTranslation(lang: string): Observable<any> {
        let output: any = {};

        for (let section in this.allTranslations) {
            output[section] = {};
            
            const i18nObj = this.allTranslations[section].default;
            for (let token in i18nObj) {
                output[section][token] = i18nObj[token][lang];
            }
        }
        return Observable.of(output);
    }
}
