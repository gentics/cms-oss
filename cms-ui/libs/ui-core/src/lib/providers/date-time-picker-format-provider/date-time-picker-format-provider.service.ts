import { Injectable } from '@angular/core';
import { NEVER, Observable } from 'rxjs';
import { DateTimePickerStrings, DEFAULT_DATE_TIME_PICKER_STRINGS } from '../../common';

const SIMPLE_FORMATTER = new Intl.DateTimeFormat(undefined, {
    dateStyle: 'long',
});
const TIME_FORMATTER = new Intl.DateTimeFormat(undefined, {
    dateStyle: 'long',
    timeStyle: 'short',
});
const SECONDS_FORMATTER = new Intl.DateTimeFormat(undefined, {
    dateStyle: 'long',
    timeStyle: 'medium',
});
const LOCALE = new Intl.Locale(navigator.language.split('-')[0]);

/**
 * Format provider to localize the DateTimePicker component.
 */
@Injectable()
export class DateTimePickerFormatProvider {

    /** Texts uses by the DateTimePicker modal. */
    strings: DateTimePickerStrings = {
        ...DEFAULT_DATE_TIME_PICKER_STRINGS,
        // Cast to any as we don't have the types for it yet
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        weekStart: (LOCALE as any).getWeekInfo().firstDay % 7,
    };

    /** May emit a value when the translations or the date format changed. */
    changed$: Observable<any> = NEVER;

    getDateOrder(): 'dmy' | 'ymd' | 'mdy' {
        const parts = SIMPLE_FORMATTER.formatToParts(new Date());
        return parts
            .filter((part) => part.type === 'day' || part.type === 'month' || part.type === 'year')
            .map((part) => part.type[0])
            .join('') as any;
    }

    /** Formats a human-readable string to be displayed in the control input field. */
    format(date: Date, displayTime: boolean, displaySeconds: boolean): string {
        if (displayTime && displaySeconds) {
            return SECONDS_FORMATTER.format(date);
        } else if (displayTime) {
            return TIME_FORMATTER.format(date);
        } else {
            return SIMPLE_FORMATTER.format(date);
        }
    }
}
