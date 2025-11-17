import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { Subscription } from 'rxjs';
import { KnownDateFormatName } from '../../../common/models';
import { formatI18nDate } from '../../../common/utils/i18n';
import { I18nService } from '../../providers/i18n/i18n.service';

/**
 * A pipe written for `ngx-translate` to translate dates and times into the user's date/time format
 * Unlike angulars DatePipe, it allows for the locale to be changed at runtime without refreshing
 * the page, by listenening to ngx-translate's `TranslateService`.
 *
 * Usage:
 * `<p>Last changed: {{ dateObject | translatePipe:'date' }}`
 * `<p>Last changed: {{ timestamp | translatePipe:'dateTime' }}`
 * `<p>The current time is {{ timestamp | translatePipe:'time' }}`
 *
 * When changing language manually to a different language than the user agents default language,
 * the list from `navigator.languages` is used to determine a best-match format.
 * Example:
 * - User agent reports ['de', 'en-GB', 'en-US', 'en']
 * - Language is set from 'de' to 'en'
 * - => Date and time are formatted using `en-GB`, not `en-US`
 */
@Pipe({
    name: 'gtxI18nDate',
    pure: false,
    standalone: false,
})
export class I18nDatePipe implements OnDestroy, PipeTransform {
    private subscription: Subscription;

    constructor(
        private translate: I18nService,
        private changeDetector: ChangeDetectorRef,
    ) {
        this.subscription = translate.onLanguageChange().subscribe(() => {
            this.changeDetector.markForCheck();
        });
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    transform(value: Date | number, format: KnownDateFormatName = 'date'): string {
        return formatI18nDate(value, this.translate.getCurrentLanguage(), format);
    }
}
