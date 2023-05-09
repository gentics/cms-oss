import { Injectable } from '@angular/core';
import { NEVER, Observable } from 'rxjs';
import { DateTimePickerStrings, DEFAULT_DATE_TIME_PICKER_STRINGS } from '../../common';

/**
 * A simplified subset of the Moment interface which we expose via the first arg of the
 * DateTimePickerFormatProvider.format() method.
 */
export interface MomentLike {
    format(format?: string): string;

    year(): number;
    quarter(): number;
    month(): number;
    day(): number;
    date(): number;
    hour(): number;
    hours(): number;
    minute(): number;
    minutes(): number;
    second(): number;
    seconds(): number;
    millisecond(): number;
    milliseconds(): number;
    weekday(): number;
    isoWeekday(): number;
    weekYear(): number;
    isoWeekYear(): number;
    week(): number;
    weeks(): number;
    isoWeek(): number;
    isoWeeks(): number;
    weeksInYear(): number;
    isoWeeksInYear(): number;
    dayOfYear(): number;

    toArray(): number[];
    toDate(): Date;
    toISOString(): string;
    toJSON(): string;
    unix(): number;

    isLeapYear(): boolean;
    zone(): number;
    utcOffset(): number;
    daysInMonth(): number;
    isDST(): boolean;
}

/**
 * Format provider to localize the DateTimePicker component.
 */
@Injectable()
export class DateTimePickerFormatProvider {

    /** Texts uses by the DateTimePicker modal. */
    strings: DateTimePickerStrings = DEFAULT_DATE_TIME_PICKER_STRINGS;

    /** May emit a value when the translations or the date format changed. */
    changed$: Observable<any> = NEVER;

    /** Formats a human-readable string to be displayed in the control input field. */
    format(date: MomentLike, displayTime: boolean, displaySeconds: boolean): string {
        let formatString = displayTime ? (displaySeconds ? 'L, LTS' : 'L, LT') : 'L';
        return date.format(formatString);
    }
}
