import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { Subscription } from 'rxjs';
import { formatRelativeI18nDate } from '../../../common/utils';
import { I18nService } from '../../providers/i18n/i18n.service';

/**
 * Formats a date relative to the current date/time, e.g. "3 minutes ago".
 */
@Pipe({
    name: 'gtxI18nRelativeDate',
    pure: false,
    standalone: false,
})
export class I18nRelativeDatePipe implements PipeTransform, OnDestroy {

    private subscription: Subscription | null = null;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private translation: I18nService,
    ) {
        this.subscription = this.translation.onLanguageChange().subscribe(() => {
            this.changeDetector.markForCheck();
        });
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
            this.subscription = null;
        }
    }

    transform(date: Date | string | number): string {
        // Strings must be parsed into a Date object; numbers (Unix seconds from the REST API)
        // and Date objects are passed as-is — formatRelativeI18nDate handles the conversion.
        const value: Date | number = typeof date === 'string' ? new Date(date) : date;
        return formatRelativeI18nDate(value, this.translation.getCurrentLanguage());
    }

}
