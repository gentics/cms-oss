import { Injectable } from '@angular/core';
import { NEVER, Observable } from 'rxjs';
import { DateTimePickerStrings, DEFAULT_DATE_TIME_PICKER_STRINGS } from '../../common';

export interface DateTimePickerFormatProvider {
    changed$: Observable<any>;
    strings: DateTimePickerStrings;
    getDateOrder(): 'dmy' | 'ymd' | 'mdy';
    format(date: Date, displayTime: boolean, displaySeconds: boolean): string;
}

/**
 * Format provider to localize the DateTimePicker component.
 */
@Injectable()
export class DateTimePickerFormatProviderService implements DateTimePickerFormatProvider {

    protected SIMPLE_FORMATTER: Intl.DateTimeFormat;

    protected TIME_FORMATTER: Intl.DateTimeFormat;

    protected SECONDS_FORMATTER: Intl.DateTimeFormat;

    protected LOCALE: Intl.Locale;

    /** Texts uses by the DateTimePicker modal. */
    strings: DateTimePickerStrings;

    /** May emit a value when the translations or the date format changed. */
    changed$: Observable<any> = NEVER;

    constructor() {
        this.updateLocale(navigator.language.split('-')[0]);
    }

    protected updateLocale(locale: string): void {
        this.LOCALE = new Intl.Locale(locale);
        this.SIMPLE_FORMATTER = new Intl.DateTimeFormat(locale, {
            dateStyle: 'long',
        });
        this.TIME_FORMATTER = new Intl.DateTimeFormat(locale, {
            dateStyle: 'long',
            timeStyle: 'short',
        });
        this.SECONDS_FORMATTER = new Intl.DateTimeFormat(locale, {
            dateStyle: 'long',
            timeStyle: 'medium',
        });
        this.strings = {
            ...DEFAULT_DATE_TIME_PICKER_STRINGS,
            // Cast to any as we don't have the types for it yet
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            weekStart: (this.LOCALE as any).getWeekInfo().firstDay % 7,
        };
    }

    getDateOrder(): 'dmy' | 'ymd' | 'mdy' {
        const parts = this.SIMPLE_FORMATTER.formatToParts(new Date());
        return parts
            .filter((part) => part.type === 'day' || part.type === 'month' || part.type === 'year')
            .map((part) => part.type[0])
            .join('') as any;
    }

    /** Formats a human-readable string to be displayed in the control input field. */
    format(date: Date, displayTime: boolean, displaySeconds: boolean): string {
        if (displayTime && displaySeconds) {
            return this.SECONDS_FORMATTER.format(date);
        } else if (displayTime) {
            return this.TIME_FORMATTER.format(date);
        } else {
            return this.SIMPLE_FORMATTER.format(date);
        }
    }
}
