import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { Subscription } from 'rxjs';

import { GtxI18nRelativeDateService } from './i18n-relative-date.service';

/**
 * Formats a date relative to the current date/time, e.g. "3 minutes ago".
 */
@Pipe({
    name: 'gtxI18nRelativeDate',
    pure: false,
})
export class GtxI18nRelativeDatePipe implements PipeTransform, OnDestroy {

    private subscription = new Subscription();
    private lastInput: Date | string;
    private lastOutput = '';

    constructor(
        private changeDetector: ChangeDetectorRef,
        private relativeDateService: GtxI18nRelativeDateService) { }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    transform(date: Date | string, direction?: 'past' | 'future', secondsPrecision: number = 1): string {
        if (date === this.lastInput) {
            return this.lastOutput;
        }

        const dateObj = typeof date === 'string' ? new Date(date) : date;
        const format$ = this.relativeDateService.observableFormat(dateObj, direction, secondsPrecision);
        this.subscription.unsubscribe();
        this.lastOutput = '';

        this.subscription = format$.subscribe(formatted => this.textEmitted(formatted));

        return this.lastOutput;
    }

    private textEmitted(formatted: string): void {
        if (this.lastOutput && formatted !== this.lastOutput) {
            this.changeDetector.markForCheck();
        }
        this.lastOutput = formatted;
    }

}
