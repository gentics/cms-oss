import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { KnownDateFormatName, knownFormats } from './known-date-formats';

declare var navigator: Navigator &  {
    languages: string[];
};

/**
 * A pipe written for `ngx-translate` to translate dates and times into the user's date/time format
 * Unlike angulars DatePipe, it allows for the locale to be changed at runtime without refreshing
 * the page, by listenening to ngx-translate's `TranslateService`.
 *
 * Usage:
 * `<p>Last changed: {{ dateObject | i18nPipe:'date' }}`
 * `<p>Last changed: {{ timestamp | i18nPipe:'dateTime' }}`
 * `<p>The current time is {{ timestamp | i18nPipe:'time' }}`
 *
 * When changing language manually to a different language than the user agents default language,
 * the list from `navigator.languages` is used to determine a best-match format.
 * Example:
 * - User agent reports ['de', 'en-GB', 'en-US', 'en']
 * - Language is set from 'de' to 'en'
 * - => Date and time are formatted using `en-GB`, not `en-US`
 */
@Pipe({
    name: 'i18nDate',
    pure: false,
})
export class GtxI18nDatePipe implements OnDestroy, PipeTransform {

    static formatCache: { [name: string]: (date: Date) => string } = {};

    private subscription: Subscription;
    private lastValue: any;
    private lastFormat: string;
    private lastResult: string;
    private formatFunction: (date: Date) => string;

    constructor(
        private translate: TranslateService,
        private changeDetector: ChangeDetectorRef,
    ) {
        this.subscription = translate.onLangChange.subscribe((event: LangChangeEvent) => {
            this.lastValue = undefined;
            this.lastResult = undefined;
            this.formatFunction = undefined;
            this.changeDetector.markForCheck();
        });
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    transform(value: Date | number, format: KnownDateFormatName = 'date'): string {
        if (value === this.lastValue && this.lastFormat === format) {
            return this.lastResult;
        }

        if (typeof value === 'string') {
            const tmp = new Date(value);
            // Only reliable way to check if parsing worked.
            if (tmp.toString() === 'Invalid Date') {
                return '';
            }
            value = tmp;
        }

        if (value == null ||
            (typeof value !== 'number' && !(value instanceof Date)) ||
            (typeof value === 'number' && value < 1)
        ) {
            return '';
        }

        if (!this.formatFunction || this.lastFormat !== format) {
            this.formatFunction = this.getFormatFunction(this.translate.currentLang, format);
        }

        // Handle dates which are passed as a second timestamp instead of milliseconds
        let date: Date;
        if (typeof value === 'number') {
            date = new Date(value > 300000000000 ? value : value * 1000);
        } else {
            date = value as Date;
        }

        return this.formatFunction(date);
    }

    private getFormatFunction(locale: string, formatName: KnownDateFormatName): (date: Date) => string {
        const cached = GtxI18nDatePipe.formatCache[`${formatName}(${locale})`];
        if (cached) {
            return cached;
        } else if (typeof Intl !== 'object') {
            throw new Error('No Intl support on the current user agent and no polyfill used.');
        }

        // If we get passed "en" and the user prefers "en-GB" over "en-US", use that.
        let specializedLocale: string;
        if (locale.indexOf('-') > 0) {
            specializedLocale = locale;
        } else {
            const userLocales = navigator.languages || [navigator.language];
            const matchingLocale = userLocales.filter(loc => loc.startsWith(locale + '-'))[0];
            specializedLocale = matchingLocale || locale;
        }

        // Use number formatting provided by the format (e.g. 'number' vs '2-digit').
        const formatOptions = knownFormats[formatName] || knownFormats['date'];
        const originalOptions = new Intl.DateTimeFormat([specializedLocale, locale, 'en']).resolvedOptions();
        const options: any = {};
        Object.keys(formatOptions).forEach(key => {
            options[key] = originalOptions[key] || formatOptions[key];
        });
        const dateFormat = new Intl.DateTimeFormat([specializedLocale, locale, 'en'], options);

        // Cache the result
        const formatFunction = this.formatWithoutBrowserQuirks(dateFormat.format.bind(dateFormat), dateFormat);
        GtxI18nDatePipe.formatCache[`${formatName}(${locale})`] = formatFunction;
        return formatFunction;
    }

    private formatWithoutBrowserQuirks(formatter: (date: Date) => string, locale: Intl.DateTimeFormat): (date: Date) => string {
        // Internet Explorer (not Edge) formats dates with { day: 'numeric' } like { day: '2-digit' }.
        const resolvedOptions = locale.resolvedOptions();
        const userAgentIgnoresNumberFormat: boolean = /Trident.+ rv:/.test(navigator.userAgent) &&
            resolvedOptions.locale.startsWith('de') &&
            resolvedOptions.day === 'numeric' &&
            locale.format(new Date(2000, 0, 5)).indexOf('05') > 0;

        return (date: Date) => {
            let str = formatter(date);

            // Internet Explorer and Edge keep LTR or RTL characters in the string.
            // While technically correct, we remove them for consistency with other browsers.
            str = str.replace(/\u200e|\u200f/g, '');

            if (userAgentIgnoresNumberFormat) {
                // Replace 03.05.2017 => 3.5.2017
                str = str.replace(/^(?:0|([1-9]))(\d[^\d])(?:0|([1-9]))(\d[^\d]\d\d\d\d)/, '$1$2$3$4');
            }

            // IE/Edge format as '01.02.2016 10:11:12', others as '01.02.2016, 10:11:12' with comma.
            // Use the "more correct" format of Chrome / FF with a comma for consistency.
            str = str.replace(/(\d\d\d\d) (\d)/g, '$1, $2');

            return str;
        };
    }
}
