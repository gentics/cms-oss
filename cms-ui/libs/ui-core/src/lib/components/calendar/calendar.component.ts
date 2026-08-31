import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Optional,
    Output,
    SimpleChanges,
} from '@angular/core';
import { cancelEvent } from '@gentics/common';
import { Subscription } from 'rxjs';
import { DateTimePickerStrings } from '../../common';
import { DateTimePickerFormatProvider } from '../../providers';
import { generateFormProvider, normalizeToDate } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';

export interface Day {
    year: number;
    month: number;
    date: number;
}

interface Week {
    numInYear: number;
    days: Day[];
}

@Component({
    selector: 'gtx-calendar',
    templateUrl: './calendar.component.html',
    styleUrl: './calendar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(CalendarComponent),
    ],
    standalone: false,
})
export class CalendarComponent extends BaseFormElementComponent<number> implements OnInit {

    /**
     * Set to overwrite texts and date formatting in the modal.
     */
    @Input()
    public formatProvider: DateTimePickerFormatProvider;

    /**
     * Minimal date that can be selected
     */
    @Input({ transform: normalizeToDay })
    public min: Date | Day;

    /**
     * Maximal date that can be selected
     */
    @Input({ transform: normalizeToDay })
    public max: Date | Day;

    /**
     * When the value changes to another day, and the user is on another month/year/...,
     * and this is set to `true`, it'll switch the view to display the current value.
     */
    @Input()
    public followValue = false;

    /**
     * If overflow days, i.E. days which aren't part of the current visible month,
     * but which are part of the first/last week, should be displayed.
     */
    @Input()
    public displayOverflowDays = false;

    /**
     * If it should show the number of the week in a own column
     */
    @Input()
    public displayWeeks = false;

    /**
     * The year that is actively being displayed
     */
    @Input()
    public activeYear: number;

    /**
     * The month that is actively being displayed
     */
    @Input()
    public activeMonth: number;

    @Output()
    public activeYearChange = new EventEmitter<number>();

    @Output()
    public activeMonthChange = new EventEmitter<number>();

    /**
     * The `value` parsed as Date object for easier handling.
     */
    public dateValue: Day | null;

    /**
     * The weeks to render
     */
    public weeks: Week[] = [];
    /**
     * The column headers, which tell which day of the week it is.
     */
    public dayIndicators: string[];

    /**
     * The day when the week starts.
     */
    public startDate = 0;

    public formatStrings: DateTimePickerStrings;

    private providerSubscription: Subscription;

    constructor(
        changeDetector: ChangeDetectorRef,
        @Optional()
        private defaultFormatProvider: DateTimePickerFormatProvider,
    ) {
        super(changeDetector);
    }

    public ngOnInit(): void {
        // set start value
        const currentValueDate = new Date(this.value);
        this.updateActiveYear(currentValueDate.getFullYear());
        this.updateActiveMonth(currentValueDate.getMonth());
        this.updateFormatProvider();
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.formatProvider && !changes.formatProvider.firstChange) {
            this.updateFormatProvider();
        }
        if (
            (changes.activeYear && !changes.activeYear.firstChange)
            || (changes.activeMonth && !changes.activeMonth.firstChange)
        ) {
            this.rebuildWeeks();
        }
    }

    public onValueChange(): void {
        const tmp = normalizeToDate(this.value);
        const anyDateNull = tmp == null || this.dateValue == null;

        this.dateValue = tmp == null
            ? null
            : {
                year: tmp.getFullYear(),
                month: tmp.getMonth(),
                date: tmp.getDate(),
            };

        if (this.activeYear == null || this.activeMonth == null) {
            const dateToShow = tmp == null ? new Date() : tmp;
            this.updateActiveYear(dateToShow.getFullYear());
            this.updateActiveMonth(dateToShow.getMonth());
        }

        if (!this.followValue || anyDateNull) {
            return;
        }

        if (this.activeYear !== tmp.getFullYear() || this.activeMonth !== tmp.getMonth()) {
            this.updateActiveYear(tmp.getFullYear());
            this.updateActiveMonth(tmp.getMonth());

            this.rebuildWeeks();
        }
    }

    public handleDaySelect(day: Day, event?: MouseEvent): void {
        cancelEvent(event);

        let newDate = normalizeToDate(this.value);

        if (newDate == null) {
            newDate = this.toFixedDate(day.year, day.month, day.date);
        } else {
            // Create a copy of the date
            newDate = new Date(newDate);
            newDate.setUTCFullYear(day.year);
            newDate.setUTCMonth(day.month);
            newDate.setUTCDate(day.date);
        }

        let needsRebuild = false;
        if (newDate.getUTCFullYear() !== this.activeYear) {
            this.updateActiveYear(newDate.getUTCFullYear());
            needsRebuild = true;
        }
        if (newDate.getUTCMonth() !== this.activeMonth) {
            this.updateActiveMonth(newDate.getUTCMonth());
            needsRebuild = true;
        }
        if (needsRebuild) {
            this.rebuildWeeks();
        }

        this.triggerChange(newDate.getTime());
    }

    public goToPreviousMonth(): void {
        if (this.activeMonth === 0) {
            this.activeMonth = 11;
            this.activeYear -= 1;
        } else {
            this.activeMonth -= 1;
        }
        this.rebuildWeeks();
    }

    public goToNextMonth(): void {
        if (this.activeMonth === 11) {
            this.activeMonth = 0;
            this.activeYear += 1;
        } else {
            this.activeMonth += 1;
        }
        this.rebuildWeeks();
    }

    private updateFormatProvider(): void {
        const provider = this.formatProvider ?? this.defaultFormatProvider;
        if (this.providerSubscription != null) {
            this.providerSubscription.unsubscribe();
        }
        if (provider.changed$ == null) {
            this.providerSubscription = null;
        } else {
            this.providerSubscription = provider.changed$.subscribe(() => {
                this.updateFormat();
            });
        }
        this.updateFormat();
    }

    private updateFormat(): void {
        const provider = this.formatProvider ?? this.defaultFormatProvider;
        this.formatStrings = provider.strings;
        this.rebuildWeeks();
    }

    private updateActiveYear(year: number): void {
        this.activeYear = year;
        this.activeYearChange.emit(year);
    }

    private updateActiveMonth(month: number): void {
        this.activeMonth = month;
        this.activeMonthChange.emit(month);
    }

    private rebuildWeeks(): void {
        this.startDate = (this.formatStrings.weekStart ?? 0) % 7;
        this.dayIndicators = this.formatStrings.weekdaysMin
          || this.formatStrings.weekdaysShort
          || this.formatStrings.weekdays;

        this.weeks = [];
        const displayDate = this.toFixedDate(this.activeYear, this.activeMonth + 1, 1);
        const startOffset = displayDate.getDay() - this.startDate;
        let weekInYearCounter = this.getWeekNumber(displayDate);
        let currentWeek: Week = {
            numInYear: weekInYearCounter++,
            days: [],
        };
        this.weeks.push(currentWeek);

        if (startOffset !== 0) {
            // We need to fill the week with overflow dates from the previous month to make the week complete
            const prevMonth = this.getLastDayOfMonth(this.activeMonth === 0
                // In case it's january, we need to skip back one year and december
                ? this.toFixedDate(this.activeYear - 1, 11, 28)
                : this.toFixedDate(this.activeYear, this.activeMonth /* Already -1, as it's 0 indexed */, 28));

            const monthEnd = prevMonth.getDate();
            for (let i = startOffset - 1; i >= 0; i--) {
                currentWeek.days.push({
                    year: prevMonth.getFullYear(),
                    month: prevMonth.getMonth(),
                    date: monthEnd - i,
                });
            }
        }
        const endOfCurrentMonth = this.getLastDayOfMonth(displayDate);

        for (let i = 0; i < endOfCurrentMonth.getDate(); i++) {
            if (currentWeek.days.length === 7) {
                if (weekInYearCounter > 52) {
                    weekInYearCounter = 1;
                }
                currentWeek = {
                    days: [],
                    numInYear: weekInYearCounter++,
                };
                this.weeks.push(currentWeek);
            }
            currentWeek.days.push({
                year: displayDate.getUTCFullYear(),
                month: displayDate.getMonth(),
                date: i + 1,
            });
        }

        let nextYear: number;
        let nextMonth: number;
        if (this.activeMonth === 11) {
            nextYear = this.activeYear + 1;
            nextMonth = 0;
        } else {
            nextYear = this.activeYear;
            nextMonth = this.activeMonth + 1;
        }

        const daysToFill = 7 - currentWeek.days.length;
        for (let i = 0; i < daysToFill; i++) {
            currentWeek.days.push({
                year: nextYear,
                month: nextMonth,
                date: i + 1,
            });
        }
    }

    private getDateAsDay(date: Date | Day): Day | null {
        if (date == null) {
            return null;
        }

        if (date instanceof Date) {
            return {
                year: date.getFullYear(),
                month: date.getMonth(),
                // month: date.getMonth() + 1,
                date: date.getDate(),
            };
        }

        return date;
    }

    public isPreviousMonthDisabled(): boolean {
        const min = this.getDateAsDay(this.min);

        if (min == null) {
            return false;
        }

        return (
            min.year > this.activeYear
            || (min.year === this.activeYear && min.month >= this.activeMonth)
        );
    }

    public isNextMonthDisabled(): boolean {
        const max = this.getDateAsDay(this.max);
        if (max == null) {
            return false;
        }

        return (
            max.year < this.activeYear
            || (max.year === this.activeYear && max.month <= this.activeMonth)
        );
    }

    public isDayDisabled(day: Day): boolean {
        const min = this.getDateAsDay(this.min);

        if (min == null) {
            return false;
        }

        if (day.year !== min.year) {
            return day.year < min.year;
        }

        if (day.month !== min.month) {
            return day.month < min.month;
        }

        return day.date < min.date;
    }

    public isSelectedDay(day: Day): boolean {
        return (
            this.dateValue != null
            && day.year === this.dateValue.year
            && day.month === this.dateValue.month
            && day.date === this.dateValue.date
        );
    }

    /**
     * Wrapper around the {@link DateConstructor Date constructor}, which takes a ISO string.
     * This is because to prevent time zones to mess around with the values we handle.
     */
    private toFixedDate(year: number, month: number, date: number): Date {
        return new Date(`${year}-${month < 10 ? '0' + month : month}-${date < 10 ? '0' + date : date}`);
    }

    private getLastDayOfMonth(prevMonth: Date): Date {
    // We need to get the last available day in the month, so we slowly go forwards
        for (let i = 28; i <= 31; i++) {
            const tmp = this.toFixedDate(prevMonth.getFullYear(), prevMonth.getMonth() + 1, i);
            if (tmp.getMonth() !== prevMonth.getMonth()) {
                break;
            }
            prevMonth = tmp;
        }

        return prevMonth;
    }

    // Source - https://stackoverflow.com/a/6117889
    // Posted by RobG, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-08-11, License - CC BY-SA 4.0

    /* For a given date, get the ISO week number
    *
    * Algorithm is to find nearest thursday, it's year
    * is the year of the week number. Then get weeks
    * between that date and the first day of that year.
    *
    * Note that dates in one year can be weeks of previous
    * or next year, overlap is up to 3 days.
    *
    * e.g. 2014/12/29 is Monday in week  1 of 2015
    *      2012/1/1   is Sunday in week 52 of 2011
    */
    private getWeekNumber(d: Date): number {
    // Copy date so don't modify original
        d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
        // Set to nearest Thursday: current date + 4 - current day number
        // Make Sunday's day number 7
        d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay() || 7));
        // Get first day of year
        const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
        // Calculate full weeks to nearest Thursday
        const weekNo = Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
        // Return array of year and week number
        return weekNo;
    }

}

export function normalizeToDay(value: Date | Day): Day {
    if (value == null) {
        return null;
    }

    return (value instanceof Date)
        ? { year: value.getUTCFullYear(), month: value.getUTCMonth(), date: value.getUTCDate() }
        : value;
}
