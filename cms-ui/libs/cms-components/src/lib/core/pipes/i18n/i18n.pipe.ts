import { ChangeDetectorRef, Injectable, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { IndexByKey } from '@gentics/cms-models';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { applyShortcuts, translateParamsInstant } from '../../providers/i18n';

/**
 * A wrapper around the ngx-translate TranslatePipe. Adds some convenience shortcuts to allow
 * easier translation of certain common properties:
 *
 * Shortcut Translations
 * =====================
 * Common item types will be short-cut translated, so instead of writing:
 * ```
 * {{ 'common.type_' + item.type | i18n }}
 * ```
 * we can now use:
 * ```
 * {{ item.type | i18n:{ count: items.length } }}
 * ```
 *
 * The `count` param allows the pipe to use the correct pluralized form.
 *
 * The list of words that will be shortcut translated can be found in the method `applyShortcuts()`
 *
 * Translating Params
 * ==================
 * Sometimes it is necessary to translate a param, for example, if we have a button with the English label
 * "Edit Page", and the German label "Seite Bearbeiten", we can use a single translation key:
 * ```
 * edit_type_button:
 *    en: 'Edit {{type}}'
 *    de: '{{type}} bearbeiten'
 * ```
 * In this case we need to translate the "type" param too ("page" or "seite"). To translate a param, simple
 * prefix that param with an underscore:
 * ```
 * {{ 'common.edit_type_button' | i18n:{ _type: item.type } }}
 * ```
 */
@Injectable()
@Pipe({
    name: 'i18n',
    pure: false,
    standalone: false
})
export class GtxI18nPipe implements PipeTransform, OnDestroy {

    static memoized: { [key: string]: string } = {};

    translatePipe: TranslatePipe;

    _lastValue: string;
    _lastParams: any;
    _lastResult: string;
    subscription: Subscription;

    constructor(private translate: TranslateService,
                private changeDetector: ChangeDetectorRef) {
        this.translatePipe = new TranslatePipe(translate, changeDetector);

        this.subscription = translate.onLangChange.subscribe(() => {
            GtxI18nPipe.memoized = {};
            this._lastParams = undefined;
            this._lastValue = undefined;
            this._lastResult = undefined;
            this.changeDetector.markForCheck();
        });
    }

    transform(value: string, params?: { [key: string]: any }): string {
        if (value && value === this._lastValue && params === this._lastParams) {
            return this._lastResult;
        }

        let result: string;
        const token = `${value}:${this.simpleStringify(params)}`;
        const memoized = GtxI18nPipe.memoized[token];
        if (memoized) {
            result = memoized;
        } else {
            const shortcut = applyShortcuts(value, params);
            const translatedParams = translateParamsInstant(params, this.translate);
            result = this.translatePipe.transform(shortcut, translatedParams);
            GtxI18nPipe.memoized[token] = result;
        }
        this._lastValue = value;
        this._lastParams = params;
        this._lastResult = result;
        return result;
    }

    ngOnDestroy(): void {
        this.translatePipe.ngOnDestroy();
        this.subscription.unsubscribe();
    }

    /**
     * Converts a simple string hash object into a string by concatenating
     * key value pairs. Not intended to reproduce JSON.stringify(), rather to
     * just produce a unique string representation of a hash.
     */
    private simpleStringify(params: IndexByKey<string>): string {
        let output = '';
        for (const key in params) {
            if (params.hasOwnProperty(key)) {
                output += `${key}${params[key]}`;
            }
        }
        return output;
    }
}
