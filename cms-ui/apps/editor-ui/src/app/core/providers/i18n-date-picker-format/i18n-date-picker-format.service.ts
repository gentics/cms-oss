import { Injectable } from '@angular/core';
import { DateTimePickerFormatProvider, DateTimePickerStrings } from '@gentics/ui-core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { delay, publishReplay, refCount, tap } from 'rxjs/operators';
import { I18nDatePipe } from '../../../shared/pipes/i18n-date/i18n-date.pipe';
import { ApplicationStateService } from '../../../state';

const noopChangeDetector: any = {
    markForCheck(): void { },
};

/**
 * A date format & string provider for DateTimePicker instances.
 *
 * It uses the {@see I18nDatePipe} implementation for date formats
 * and the {@see TranslateService} for texts in the date picker.
 */
@Injectable()
export class I18nDatePickerFormat implements DateTimePickerFormatProvider {

    strings: DateTimePickerStrings;

    changed$: Observable<any>;

    private datePipe: I18nDatePipe;

    constructor(
        private translate: TranslateService,
        private appState: ApplicationStateService,
    ) {
        this.datePipe = new I18nDatePipe(translate, noopChangeDetector);

        this.changed$ = appState.select(state => state.ui.language).pipe(
            // Added a delay since the 'currentLang' of `translate` hasn't been
            // updated at this point, which would in turn not update the
            // language properly and stay on the same.
            delay(100),
            tap(() => this.updateStrings()),
            publishReplay(1),
            refCount(),
        );

        this.updateStrings();
    }

    format(moment: any, displayTime: boolean, displaySeconds: boolean): string {
        return this.datePipe.transform(moment.toDate(), displayTime ? 'dateTime' : 'date');
    }

    private updateStrings(): void {
        const translation = this.translate.instant.bind(this.translate);

        this.strings = {
            hours: translation('modal.datepicker_hours'),
            minutes: translation('modal.datepicker_minutes'),
            seconds: translation('modal.datepicker_seconds'),
            okay: translation('common.okay_button'),
            cancel: translation('common.cancel_button'),
            months: translation('modal.datepicker_months'),
            weekdays: translation('modal.datepicker_weekdays'),
        };
    }
}

