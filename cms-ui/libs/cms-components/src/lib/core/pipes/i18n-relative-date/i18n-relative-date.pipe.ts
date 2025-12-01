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
        if (typeof date === 'string') {
            date = new Date(date);
        }

        return formatRelativeI18nDate(date, this.translation.getCurrentLanguage());
    }

}
