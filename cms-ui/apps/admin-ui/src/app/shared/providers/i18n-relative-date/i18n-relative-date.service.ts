import { I18nService } from '@admin-ui/core';
import { InitializableServiceBase } from '@admin-ui/shared/providers/initializable-service-base';
import { AppStateService } from '@admin-ui/state';
import { Injectable, } from '@angular/core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { distinctUntilChanged, filter, map, switchMapTo } from 'rxjs/operators';

export interface RelativeTimeTranslation {
    /** Translation key used for the i18nService */
    key: string;

    /** Difference in seconds/minutes/days/months */
    count: number;
}

const MILLISECONDS_PER_DAY = 86400000;

@Injectable()
export class I18nRelativeDateService extends InitializableServiceBase {

    /** App-wide time that ticks every second, so all sources update at the same time */
    currentTime = new BehaviorSubject(new Date());

    private subscription = new Subscription();

    constructor(
        private i18n: I18nService,
        private state: AppStateService,
    ) {
        super();
    }

    protected onServiceInit(): void {
        const interval = setInterval(() => {
            this.currentTime.next(new Date());
        }, 1000);
        this.subscription.add(() => clearInterval(interval));
    }

    protected onServiceDestroy(): void {
        this.subscription.unsubscribe();
    }

    /**
     * Formats a date object relative to the current time.
     *
     * @param direction limits the time to be in the past/future
     *     to fix display for users with a slightly incorrect system time
     * @param secondsPrecision round diffence < 1 minute to a near value
     *
     * @example
     *     format(nowPlus45s); // -> 'in 45 seconds'
     *     format(nowMinus120s); // -> '2 minutes ago'
     */
    format(date: Date, direction?: 'future' | 'past', secondsPrecision: number = 1): string {
        const { key, count } = this.getTranslationParams(date, direction, secondsPrecision);
        return this.i18n.instant(key, { count });
    }

    /** Formats a date and emits the translation when it changes. */
    observableFormat(date: Date, direction?: 'future' | 'past', secondsPrecision: number = 1): Observable<string> {
        this.init();

        const millisecondDiff = Math.abs(date.getTime() - this.currentTime.value.getTime());
        let time$: Observable<any> = this.currentTime;

        // For differences of >= 1 day, only refresh once per minute
        if (millisecondDiff > MILLISECONDS_PER_DAY) {
            time$ = this.currentTime.pipe(
                filter((currDate, index) => !index || !currDate.getSeconds()),
            );
        }

        const language$ = this.state.select(state => state.ui.language);

        const translationObservable = time$.pipe(
            map(() => this.getTranslationParams(date, direction, secondsPrecision)),
            distinctUntilChanged((a, b) => a.key === b.key && a.count === b.count),
            map(({ key, count }) => this.i18n.instant(key, { count })),
        );

        // When the clock ticks or the UI language is changed, format date
        return language$.pipe(switchMapTo(translationObservable));
    }

    /**
     * Plumbing function that returns the input for the translation pipe.
     * Can be used for distinctUntilChanged or to cache results.
     */
    getTranslationParams(date: Date, direction?: 'future' | 'past', secondsPrecision: number = 1): RelativeTimeTranslation {
        const now = this.currentTime.value;
        if ((direction === 'future' && date < now) || (direction === 'past' && date > now)) {
            date = now;
        }

        const translation = (shortKey: string, count: number) => ({
            key: 'common.time_' + (date < now ? shortKey + '_ago' : 'in_' + shortKey),
            count,
        });

        const seconds = Math.floor(Math.abs((date.getTime() - now.getTime()) / 1000));

        if (seconds < Math.max(5, secondsPrecision)) {
            return translation('a_moment', 0);
        }

        if (seconds < 60) {
            const roundedSeconds = Math.min(seconds / secondsPrecision) * secondsPrecision;
            const displaySeconds = Math.max(5, secondsPrecision, Math.min(roundedSeconds, 60 - secondsPrecision));
            return translation('seconds', displaySeconds);
        }

        const minutes = Math.floor(seconds / 60);
        if (minutes < 2) {
            return translation('one_minute', 1);
        } else if (minutes < 60) {
            return translation('minutes', minutes);
        }

        const hours = Math.floor(minutes / 60);
        if (hours === 1) {
            return translation('one_hour', 1);
        }

        const sameDay = hours < 24 && date.getDate() === now.getDate();
        if (hours < 4 || sameDay) {
            return translation('hours', hours);
        }

        // Calculate full days: "16:30 monday" is 2 days ago at "08:15 wednesday"
        const dayToday = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
        const dayThen = Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
        const days = Math.floor(Math.abs(dayThen - dayToday) / MILLISECONDS_PER_DAY);

        if (days <= 1) {
            return translation('one_day', 1);
        } else if (days === 2) {
            return translation('two_days', 2);
        } else if (days < 30) {
            return translation('days', days);
        }

        const months = Math.floor(days / (365 / 12));
        if (months < 2) {
            return translation('one_month', 1);
        } else if (days < 365) {
            return translation('months', months);
        } else if (days < 365 * 2) {
            return translation('one_year', 1);
        }

        return translation('years', Math.floor(months / 12));
    }

}

