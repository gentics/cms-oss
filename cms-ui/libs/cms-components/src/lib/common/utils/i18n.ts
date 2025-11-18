import { KnownDateFormatName, KNOWN_FORMATS } from '../models';

type FormatFn = (date: Date) => string;
type RelativeFn = (value: number) => string;

interface RelativeTime {
    value: number;
    unit: Intl.RelativeTimeFormatUnitSingular;
}

const REGULAR_FORMAT_CACHE: Record<string, FormatFn> = {};
const RELATIVE_FORMAT_CACHE: Record<string, RelativeFn> = {};

export function formatI18nDate(value: Date | number, lang: string, format: KnownDateFormatName = 'date'): string {
    if (typeof value === 'string') {
        const tmp = new Date(value);
        // Only reliable way to check if parsing worked.
        if (tmp.toString() === 'Invalid Date') {
            return '';
        }
        value = tmp;
    }

    if (
        value == null
        || (typeof value !== 'number' && !(value instanceof Date))
        || (typeof value === 'number' && value < 1)
    ) {
        return '';
    }

    const formatFunction = getFormatFunction(lang, format);

    // Handle dates which are passed as a second timestamp instead of milliseconds
    let date: Date;
    if (typeof value === 'number') {
        date = new Date(value > 300000000000 ? value : value * 1000);
    } else {
        date = value;
    }

    return formatFunction(date);
}

export function formatRelativeI18nDate(value: Date | number, lang: string): string {
    if (value instanceof Date) {
        if (value.toString() === 'Invalid Date') {
            return '';
        }
        value = value.getTime() / 1000;
    }

    const cached = RELATIVE_FORMAT_CACHE[lang];
    if (cached) {
        return cached(value);
    } else if (typeof Intl !== 'object') {
        throw new Error('No Intl support on the current user agent and no polyfill used.');
    }

    const formatter = new Intl.RelativeTimeFormat(getLocales(lang));
    const formatFunction: RelativeFn = (value) => {
        const time = toRelativeTime(value);
        return formatter.format(time.value, time.unit);
    };
    cached[lang] = formatFunction;

    return formatFunction(value);
}

function toRelativeTime(value: number): RelativeTime {
    // Make it positive, because lookup below is easier with just positive values
    let mul = 1;
    if (value < 0) {
        mul = -1;
        value = value * -1;
    }

    let tmp = Math.floor(value);
    const seconds = tmp % 60;
    tmp = Math.floor(tmp / 60);
    const minutes = tmp % 60;
    tmp = Math.floor(tmp / 60);
    const hours = tmp % 24;
    const days = Math.floor(tmp / 24);
    const weeks = days / 7;
    const months = days / 30;
    const years = days / 365;

    if (years) {
        return { value: years * mul, unit: 'year' };
    } else if (months) {
        return { value: months * mul, unit: 'month' };
    } else if (weeks) {
        return { value: weeks * mul, unit: 'week' };
    } else if (days) {
        return { value: days * mul, unit: 'day' };
    } else if (hours) {
        return { value: hours * mul, unit: 'hour' };
    } else if (minutes) {
        return { value: minutes * mul, unit: 'minute' };
    } else {
        return { value: seconds * mul, unit: 'second' };
    }
}

// If we get passed "en" and the user prefers "en-GB" over "en-US", use that.
function getLocales(locale: string): string[] {
    if (locale.includes('-')) {
        return [locale, 'en'];
    }

    const userLocales = navigator.languages || [navigator.language];
    const matchingLocale = userLocales.find((loc) => loc.startsWith(locale + '-'));
    if (matchingLocale) {
        return [matchingLocale, locale, 'en'];
    }

    return [locale, 'en'];
}

function getFormatFunction(lang: string, formatName: KnownDateFormatName): FormatFn {
    const cached = REGULAR_FORMAT_CACHE[`${formatName}(${lang})`];
    if (cached) {
        return cached;
    } else if (typeof Intl !== 'object') {
        throw new Error('No Intl support on the current user agent and no polyfill used.');
    }

    const locales = getLocales(lang);
    debugger;

    // Use number formatting provided by the format (e.g. 'number' vs '2-digit').
    const formatOptions = KNOWN_FORMATS[formatName] || KNOWN_FORMATS['date'];
    const originalOptions = new Intl.DateTimeFormat(locales).resolvedOptions();
    const options: any = {};
    Object.keys(formatOptions).forEach((key) => {
        options[key] = originalOptions[key] || formatOptions[key];
    });
    const dateFormat = new Intl.DateTimeFormat(locales, options);

    const formatFunction = formatWithoutBrowserQuirks(dateFormat.format.bind(dateFormat), dateFormat);

    // Cache the result
    REGULAR_FORMAT_CACHE[`${formatName}(${lang})`] = formatFunction;

    return formatFunction;
}

function formatWithoutBrowserQuirks(formatter: (date: Date) => string, locale: Intl.DateTimeFormat): (date: Date) => string {
    // Internet Explorer (not Edge) formats dates with { day: 'numeric' } like { day: '2-digit' }.
    const resolvedOptions = locale.resolvedOptions();
    const userAgentIgnoresNumberFormat: boolean = /Trident.+ rv:/.test(navigator.userAgent)
      && resolvedOptions.locale.startsWith('de')
      && resolvedOptions.day === 'numeric'
      && locale.format(new Date(2000, 0, 5)).indexOf('05') > 0;

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
