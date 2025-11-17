import { ChangeDetectorRef, Injectable, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { IndexByKey } from '@gentics/cms-models';
import { Subscription } from 'rxjs';
import { I18nService } from '../../providers/i18n/i18n.service';

/**
 * A wrapper around the ngx-translate TranslatePipe. Adds some convenience shortcuts to allow
 * easier translation of certain common properties:
 *
 * Shortcut Translations
 * =====================
 * Common item types will be short-cut translated, so instead of writing:
 * ```
 * {{ 'common.type_' + item.type | gtxI18n }}
 * ```
 * we can now use:
 * ```
 * {{ item.type | gtxI18n:{ count: items.length } }}
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
 * {{ 'common.edit_type_button' | gtxI18n:{ _type: item.type } }}
 * ```
 */
@Injectable()
@Pipe({
    name: 'gtxI18n',
    pure: false,
    standalone: false,
})
export class I18nPipe implements PipeTransform, OnDestroy {

    static memoized: { [key: string]: string } = {};

    private lastValue: string;
    private lastParam: any;
    private lastResult: string;
    private subscription: Subscription;

    constructor(
        private translate: I18nService,
        private changeDetector: ChangeDetectorRef,
    ) {
        this.subscription = translate.onLanguageChange().subscribe(() => {
            I18nPipe.memoized = {};
            this.lastParam = undefined;
            this.lastValue = undefined;
            this.lastResult = undefined;
            this.changeDetector.markForCheck();
        });
    }

    transform(value: string, params?: { [key: string]: any }): string {
        if (value && value === this.lastValue && params === this.lastParam) {
            return this.lastResult;
        }

        let result: string;
        const token = `${value}:${this.simpleStringify(params)}`;
        const memoized = I18nPipe.memoized[token];
        if (memoized) {
            result = memoized;
        } else {
            const shortcut = this.translate.applyShortcuts(value, params);
            const translatedParams = this.translate.translateParamsInstant(params);
            result = this.translate.instant(shortcut, translatedParams);
            I18nPipe.memoized[token] = result;
        }
        this.lastValue = value;
        this.lastParam = params;
        this.lastResult = result;
        return result;
    }

    ngOnDestroy(): void {
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
