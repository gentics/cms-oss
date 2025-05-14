import { AppStateService } from '@admin-ui/state';
import { Pipe, PipeTransform } from '@angular/core';

/** Temporal duration */
interface Duration {
    years?: number;
    months?: number;
    weeks?: number;
    days?: number;
    hours?: number;
    minutes?: number;
    seconds?: number;
    milliseconds?: number;
    microseconds?: number
    nannoseconds?: number;
}

type SegmentStyle = 'long' | 'short' | 'narrow';
type SegmentDisplay = 'always' | 'auto';
type FineSegmentStyle = SegmentStyle | '2-digit' | 'numeric'

interface FormattingOptions {
    localeMatcher?: 'lookup' | 'best fit';
    numberingSystem?: 'arab' | 'hand' | 'mathsans';
    style?: SegmentStyle | 'digital';
    years?: SegmentStyle;
    yearsDisplay?: SegmentDisplay;
    months?: SegmentStyle;
    monthsDisplay?: SegmentDisplay;
    weeks?: SegmentStyle;
    weeksDisplay?: SegmentDisplay;
    days?: SegmentStyle;
    daysDisplay?: SegmentDisplay;
    hours?: SegmentStyle;
    hoursDisplay?: SegmentDisplay;
    minutes?: FineSegmentStyle;
    minutesDisplay?: SegmentDisplay;
    seconds?: FineSegmentStyle;
    secondsDisplay?: SegmentDisplay;
    milliseconds?: FineSegmentStyle;
    millisecondsDisplay?: SegmentDisplay;
    microseconds?: FineSegmentStyle;
    microsecondsDisplay?: SegmentDisplay;
    nannoseconds?: FineSegmentStyle;
    nannosecondsDisplay?: SegmentDisplay;
    fractionalDigits?: 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9;
}

const DEFAULT_FORMAT_OPTIONS: FormattingOptions = {
    style: 'short',
};

@Pipe({
    name: 'gtxI18nDuration',
    standalone: false,
})
export class I18nDurationPipe implements PipeTransform {

    constructor(
        protected appState: AppStateService,
    ) {}

    transform(value: number | Duration, options?: FormattingOptions): string {
        if (typeof value === 'number') {
            if (!Number.isFinite(value) || Number.isNaN(value)) {
                return '';
            }

            // const milliseconds = value % 1000;
            let tmp = Math.floor(value / 1000);
            const seconds = tmp % 60;
            tmp = Math.floor(tmp / 60);
            const minutes = tmp % 60;
            tmp = Math.floor(tmp / 60);
            const hours = tmp % 24;
            const days = Math.floor(tmp / 24);

            value = {
                // milliseconds,
                seconds,
                minutes,
                hours,
                days,
            };
        }

        const lang = this.appState.now.ui.language;
        options ??= DEFAULT_FORMAT_OPTIONS;

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const formatter = new (Intl as any).DurationFormat(lang, options);
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        return formatter.format(value);
    }
}
