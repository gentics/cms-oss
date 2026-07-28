import { Pipe, PipeTransform } from '@angular/core';
import { CmsFormElementI18nValue } from '@gentics/cms-models';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { FormEditorService } from '../../providers/form-editor/form-editor.service';

export type I18nFgSource
/** Get text for current UI language. */
  = 'ui'
/** Get text for current content language. */
    | 'content';

/**
 * # Gentics Form Generator translation pipe
 * Converts label_i18n property into requested translation value.
 * Needs to be asynchronous to update on language change.
 * @example
 * ```html
 * <div class="form-element-label">
 *     {{ element?.label_i18n | i18nfg$:'content' | async }}
 * </div>
 * ```
 *
 * You can also specify the other language-source as a fallback-language, by appending an underscore:
 * @example
 * ```html
 * <div class="form-element-label">
 *     {{ element?.label_i18n | i18nfg$:'content':'_ui' | async }}
 * </div>
 * ```
 */
@Pipe({
    name: 'i18nfg$',
    pure: false,
    standalone: false,
})
export class I18nFgPipe implements PipeTransform {

    constructor(
        private formEditorService: FormEditorService,
    ) {}

    transform(labelI18n: CmsFormElementI18nValue<string | number | boolean | null>, source: I18nFgSource, fallbackLangCode?: string): Observable<any> {
        switch (source) {
            case 'ui':
                return this.formEditorService.activeUiLanguageCode$.pipe(
                    switchMap((langCode) => {
                        if (fallbackLangCode === '_content') {
                            return this.formEditorService.activeContentLanguageCode$.pipe(
                                map((contentLangCode) => this.getI18n(labelI18n, langCode, contentLangCode)),
                            );
                        }

                        return of(this.getI18n(labelI18n, langCode, fallbackLangCode));
                    }),
                );

            case 'content':
                return this.formEditorService.activeContentLanguageCode$.pipe(
                    switchMap((langCode) => {
                        if (fallbackLangCode === '_ui') {
                            return this.formEditorService.activeUiLanguageCode$.pipe(
                                map((uiLangCode) => this.getI18n(labelI18n, langCode, uiLangCode)),
                            );
                        }

                        return of(this.getI18n(labelI18n, langCode, fallbackLangCode));
                    }),
                );

            default:
                throw new Error(`Invalid language argument: "${source}".`);
        }
    }

    private getI18n(labelI18n: CmsFormElementI18nValue<string | number | boolean | null>, targetLangCode: string, fallbackLangCode?: string): any {
        if (!labelI18n) {
            return null;
        }

        const translation = labelI18n[targetLangCode || fallbackLangCode] || labelI18n[fallbackLangCode];

        return translation || null;
    }

}
