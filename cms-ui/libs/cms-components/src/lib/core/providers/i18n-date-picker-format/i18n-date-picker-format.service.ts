import { Injectable } from '@angular/core';
import { DateTimePickerFormatProvider, DateTimePickerStrings, Moment } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { formatI18nDate } from '../../../common/utils/i18n';
import { I18nService } from '../i18n/i18n.service';

/**
 * A date format & string provider for DateTimePicker instances.
 */
@Injectable()
export class I18nDatePickerFormatService implements DateTimePickerFormatProvider {

    strings: DateTimePickerStrings;

    changed$: Observable<any>;

    constructor(
        private translate: I18nService,
    ) {
        this.changed$ = translate.onLanguageChange().pipe(
            tap(() => this.updateStrings()),
        );

        this.updateStrings();
    }

    format(moment: Moment, displayTime: boolean, _displaySeconds: boolean): string {
        return formatI18nDate(moment.toDate(), this.translate.getCurrentLanguage(), displayTime ? 'dateTime' : 'date');
    }

    private updateStrings(): void {
        const months = [
            this.translate.instant('date.datepicker_month_january'),
            this.translate.instant('date.datepicker_month_february'),
            this.translate.instant('date.datepicker_month_march'),
            this.translate.instant('date.datepicker_month_april'),
            this.translate.instant('date.datepicker_month_may'),
            this.translate.instant('date.datepicker_month_june'),
            this.translate.instant('date.datepicker_month_july'),
            this.translate.instant('date.datepicker_month_august'),
            this.translate.instant('date.datepicker_month_september'),
            this.translate.instant('date.datepicker_month_october'),
            this.translate.instant('date.datepicker_month_november'),
            this.translate.instant('date.datepicker_month_december'),
        ];

        const monthsShort = [
            this.translate.instant('date.datepicker_month_short_january'),
            this.translate.instant('date.datepicker_month_short_february'),
            this.translate.instant('date.datepicker_month_short_march'),
            this.translate.instant('date.datepicker_month_short_april'),
            this.translate.instant('date.datepicker_month_short_may'),
            this.translate.instant('date.datepicker_month_short_june'),
            this.translate.instant('date.datepicker_month_short_july'),
            this.translate.instant('date.datepicker_month_short_august'),
            this.translate.instant('date.datepicker_month_short_september'),
            this.translate.instant('date.datepicker_month_short_october'),
            this.translate.instant('date.datepicker_month_short_november'),
            this.translate.instant('date.datepicker_month_short_december'),
        ];

        const weekdays = [
            this.translate.instant('date.datepicker_weekday_sunday'),
            this.translate.instant('date.datepicker_weekday_monday'),
            this.translate.instant('date.datepicker_weekday_tuesday'),
            this.translate.instant('date.datepicker_weekday_wednesday'),
            this.translate.instant('date.datepicker_weekday_thursday'),
            this.translate.instant('date.datepicker_weekday_friday'),
            this.translate.instant('date.datepicker_weekday_saturday'),
        ];

        const weekdaysShort = [
            this.translate.instant('date.datepicker_weekday_short_sunday'),
            this.translate.instant('date.datepicker_weekday_short_monday'),
            this.translate.instant('date.datepicker_weekday_short_tuesday'),
            this.translate.instant('date.datepicker_weekday_short_wednesday'),
            this.translate.instant('date.datepicker_weekday_short_thursday'),
            this.translate.instant('date.datepicker_weekday_short_friday'),
            this.translate.instant('date.datepicker_weekday_short_saturday'),
        ];

        const weekdaysMin = [
            this.translate.instant('date.datepicker_weekday_minimal_sunday'),
            this.translate.instant('date.datepicker_weekday_minimal_monday'),
            this.translate.instant('date.datepicker_weekday_minimal_tuesday'),
            this.translate.instant('date.datepicker_weekday_minimal_wednesday'),
            this.translate.instant('date.datepicker_weekday_minimal_thursday'),
            this.translate.instant('date.datepicker_weekday_minimal_friday'),
            this.translate.instant('date.datepicker_weekday_minimal_saturday'),
        ];

        let weekStart = 1;
        try {
            weekStart = parseInt(this.translate.instant('date.datepicker_week_start'), 10);
            if (!Number.isInteger(weekStart)) {
                console.warn('Could not determine week-start from translation "date.datepicker_week_start"!');
                weekStart = 1;
            }
        } catch (err) {}

        this.strings = {
            okay: this.translate.instant('common.okay_button'),
            cancel: this.translate.instant('common.cancel_button'),
            hours: this.translate.instant('date.datepicker_hours'),
            minutes: this.translate.instant('date.datepicker_minutes'),
            seconds: this.translate.instant('date.datepicker_seconds'),
            months,
            monthsShort,
            weekdays,
            weekdaysShort,
            weekdaysMin,
            weekStart,
        };
    }
}
