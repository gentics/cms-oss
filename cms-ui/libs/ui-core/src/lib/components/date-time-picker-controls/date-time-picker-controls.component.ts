/* eslint-disable @typescript-eslint/no-unsafe-argument */
/* eslint-disable @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-member-access, @typescript-eslint/no-unsafe-call */
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostBinding,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Optional,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { locale } from 'moment';
import { NEVER, Subscription } from 'rxjs';
import { startWith } from 'rxjs/operators';
import {
    DEFAULT_DATE_TIME_PICKER_STRINGS,
    DateTimePickerStrings,
    Moment,
    getInstance,
    rome,
    unix,
} from '../../common';
import { BaseFormElementComponent } from '../../components/base-form-element/base-form-element.component';
import { DateTimePickerFormatProvider } from '../../providers/date-time-picker-format-provider/date-time-picker-format-provider.service';
import { generateFormProvider } from '../../utils';

// http://ecma-international.org/ecma-262/5.1/#sec-15.9.1.1
const MAX_DATE_MILLISECONDS = 8640000000000000;
// On default, only show 200 years in each direction
const MAX_YEAR_RANGE = 200;

type TimeUnit = 'hours' | 'minutes' | 'seconds';

function asCleanNumber(value: any): number {
    if (value == null) {
        return 0;
    }
    value = Number(value);
    if (!Number.isFinite(value)) {
        return 0;
    }
    return value;
}

function toDate(value: null | Date | number | string): Date | null {
    if (value == null) {
        return null;
    }

    if (typeof value === 'object') {
        if (value instanceof Date && value.toString() !== 'Invalid Date') {
            return value;
        }
        return null;
    }

    return new Date(value);
}

/**
 * The controls (calendar view, year & time inputs) powering the `DateTimePicker` component
 * Can be used as a stand-alone component.
 */
@Component({
    selector: 'gtx-date-time-picker-controls',
    templateUrl: './date-time-picker-controls.component.html',
    styleUrls: ['./date-time-picker-controls.component.scss'],
    providers: [generateFormProvider(DateTimePickerControlsComponent)],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DateTimePickerControlsComponent
    extends BaseFormElementComponent<number>
    implements OnInit, OnChanges, AfterViewInit, OnDestroy {

    private static momentLocales: [DateTimePickerStrings, string][] = [[DEFAULT_DATE_TIME_PICKER_STRINGS, 'en']];

    /**
     * Set to overwrite texts and date formatting in the modal.
     */
    @Input()
    public formatProvider: DateTimePickerFormatProvider;

    /**
     * The minimum date allowable. E.g. `new Date(2015, 2, 12)`
     */
    @Input()
    public min: Date | null;

    /**
     * The maximum date allowable. E.g. `new Date(2031, 1, 30)`
     */
    @Input()
    public max: Date | null;

    /**
     * If true, the year may be selected from a Select control
     */
    @Input()
    public selectYear = false;

    /**
     * Set to `false` to omit the time picker part of the component. Defaults to `true`
     */
    @Input()
    public displayTime = false;

    /**
     * Set to `false` to omit the seconds of the time picker part. Defaults to `true`
     */
    @Input()
    public displaySeconds = false;

    /**
     * When `true`, the controls use the "compact" (small screen) styling for all screen sizes. Defaults to `false`
     */
    @Input()
    @HostBinding('class.compact')
    public compact = false;

    /** Container instance to where rome is getting bound to. */
    @ViewChild('calendarContainer', { static: true })
    protected calendarContainer: ElementRef<Element>;

    /** The order of how the date is supposed to be displayed. */
    public dateOrder: 'dmy' | 'ymd' | 'mdy' = 'mdy';

    /** List of selectable years */
    public years: number[] = [];

    public selectedYear: number;

    /** The value as timestamp to be used with momentjs */
    public momentValue: Moment = getInstance();

    /** The time value */
    public time: any = {
        h: 0,
        m: 0,
        s: 0,
    };

    /**
     * cal is an instance of a Rome calendar, for the API see https://github.com/bevacqua/rome#rome-api
     */
    private calendarInstance: any;

    private providerSubscription: Subscription;

    constructor(
        changeDetector: ChangeDetectorRef,
        @Optional()
        private defaultFormatProvider: DateTimePickerFormatProvider,
    ) {
        super(changeDetector);
        this.booleanInputs.push('selectYear', 'displayTime', 'displaySeconds', 'compact');
    }

    ngOnInit(): void {
        if (this.defaultFormatProvider == null) {
            this.defaultFormatProvider = new DateTimePickerFormatProvider();
        }

        if (this.formatProvider == null) {
            this.formatProvider = this.defaultFormatProvider;
        }

        this.setupProviderChangeHook();
        this.convertRanges();
        // Cleanup the value
        this.value = asCleanNumber(this.value);
        this.initializeTimestamp();
        this.updateYears();
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if ((changes.min && !changes.min.firstChange) || (changes.max && !changes.max.firstChange)) {
            this.convertRanges();
            this.onRangeChange();
        }
        if (changes.formatProvider && !changes.formatProvider.firstChange) {
            this.setupProviderChangeHook();
        }
    }

    /**
     * Initialize the Rome widget instance.
     */
    ngAfterViewInit(): void {
        const calendarEl = this.calendarContainer.nativeElement;

        this.calendarInstance = rome(calendarEl, this.getRomeConfig()).on('data', () => {
            this.handleRomeValueChange();
        });
    }

    ngOnDestroy(): void {
        super.ngOnDestroy();

        if (this.providerSubscription) {
            this.providerSubscription.unsubscribe();
        }

        if (this.calendarInstance) {
            this.calendarInstance.off('data');
            this.calendarInstance.destroy();
            this.calendarInstance = undefined;
        }
    }

    protected initializeTimestamp(): void {
        // Only initialize the value if there is none set yet
        if (this.value !== 0) {
            this.updateMomentValue(this.value);
            return;
        }

        // Set the current value to now, as we want to provide a senseful default.
        // Perform checks if `now` would be in range and clamp it accordingly.
        const now = new Date().getTime();

        // Check if the timestamp is out of (min/max) bounds.
        if (this.min != null) {
            const minTime = this.min.getTime();
            if (now < minTime) {
                this.updateMomentValue(minTime / 1000);
                this.triggerChange(this.getUnixTimestamp());
                return;
            }
        }

        if (this.max != null) {
            const maxTime = this.max.getTime();
            if (now > maxTime) {
                this.updateMomentValue(maxTime / 1000);
                this.triggerChange(this.getUnixTimestamp());
                return;
            }
        }

        // If it's inbounds, we can set the timestamp
        this.updateMomentValue(now / 1000);
        this.triggerChange(this.getUnixTimestamp());
    }

    protected onValueChange(force = false): void {
        const timestamp = asCleanNumber(this.value);

        // If the value is actually the same, then we don't need a change
        if (!force && this.getUnixTimestamp() === timestamp) {
            return;
        }

        this.updateMomentValue(timestamp);
        this.updateCalendarOptions();
    }

    protected handleRomeValueChange(): void {
        this.momentValue = this.calendarInstance.getMoment();
        this.updateInternalValues();
        this.triggerChange(this.getUnixTimestamp());
    }

    protected updateMomentValue(timestamp: number): void {
        this.momentValue = unix(timestamp);
        this.updateInternalValues();
    }

    protected updateInternalValues(): void {
        this.selectedYear = this.momentValue.year();
        this.updateTimeObject(this.momentValue);
        this.updateCalendar(this.momentValue);
    }

    protected convertRanges(): void {
        this.min = toDate(this.min);
        this.max = toDate(this.max);
    }

    protected updateCalendarOptions(): void {
        if (this.calendarInstance) {
            this.calendarInstance.options(this.getRomeConfig());
            // `options` call destroys and restores the instance.
            // Therefore, we have to re-assign the event listener, otherwise that one is lost as well.
            this.calendarInstance.on('data', () => {
                this.handleRomeValueChange();
            });
            this.calendarInstance.show();
        }
    }

    protected updateYears(): void {
        // Default min/max with proper values
        const minSet = this.min != null;
        const maxSet = this.max != null;

        const min = this.min || new Date(-MAX_DATE_MILLISECONDS);
        const max = this.max || new Date(MAX_DATE_MILLISECONDS);

        let minYear = min.getFullYear();
        let maxYear = max.getFullYear();

        /*
         * We don't want a date select which is stupidly long when no
         * ranges are provided. Therefore limit it to 200 years +- from now.
         */
        if (!minSet && !maxSet) {
            const thisYear = new Date().getFullYear();

            if (MAX_YEAR_RANGE < maxYear - minYear) {
                minYear = thisYear - Math.floor(MAX_YEAR_RANGE / 2);
                maxYear = thisYear + Math.floor(MAX_YEAR_RANGE / 2);
            }
        }

        this.years = [];
        for (let year = minYear; year <= maxYear; year ++) {
            this.years.push(year);
        }
    }

    protected onRangeChange(): void {
        this.updateYears();

        const min = this.min || new Date(-MAX_DATE_MILLISECONDS);
        const max = this.max || new Date(MAX_DATE_MILLISECONDS);

        this.updateCalendarOptions();

        // If the current value would be out of range, limit it and trigger a change
        if (this.momentValue.isBefore(min)) {
            this.momentValue = unix(min.getTime());
            this.updateInternalValues();
            this.triggerChange(this.getUnixTimestamp());
        } else if (this.momentValue.isAfter(max)) {
            this.momentValue = unix(max.getTime());
            this.updateInternalValues();
            this.triggerChange(this.getUnixTimestamp());
        }
    }

    protected getRomeConfig(): any {
        const romeConfig: any = {
            appendTo: this.calendarContainer.nativeElement,
            time: false,
            initialValue: this.momentValue,
        };
        if (this.min) {
            romeConfig.min = this.min;
        }
        if (this.max) {
            romeConfig.max = this.max;
        }
        if (this.value != null) {
            romeConfig.weekdayFormat = this.momentValue.localeData().weekdaysMin();
            romeConfig.weekStart = this.momentValue.localeData().firstDayOfWeek();
        }

        return romeConfig;
    }

    protected setupProviderChangeHook(): void {
        // Update strings and date format when format provider emits a change
        this.providerSubscription = (this.formatProvider.changed$ || NEVER)
            .pipe(startWith(null))
            .subscribe(() => {
                this.momentValue.locale(this.getMomentLocale());
                this.updateTimeObject(this.momentValue);

                // When the locale changes, re-initialize the calendar to update the
                // weekdays as these are only updated when initialized.
                this.updateCalendarOptions();

                this.determineDateOrder();
            });
    }

    /**
     * Update the this.value in accordance with the input of one of the
     * time fields (h, m, s).
     */
    public updateTime(unit: TimeUnit, value: number): void {
        const newValue = this.updateByUnits(this.momentValue.clone(), unit, value);
        if ((this.min && newValue.isBefore(this.min)) || (this.max && newValue.isAfter(this.max))) {
            // the new year is out of the allowed range
            return;
        }

        this.updateByUnits(this.momentValue, unit, value);
        this.triggerChange(this.getUnixTimestamp());

        this.updateTimeObject(this.momentValue);
        this.updateCalendar(this.momentValue);
    }

    /**
     * Handler for the incrementing the time values when up or down arrows are pressed.
     */
    public timeKeyHandler(unit: TimeUnit, e: KeyboardEvent): void {
        if (e.code === 'ArrowUp') {
            e.preventDefault();
            this.incrementTime(unit);
        }

        if (e.code === 'ArrowDown') {
            e.preventDefault();
            this.decrementTime(unit);
        }
    }

    public incrementTime(unit: TimeUnit): void {
        this.addToTime(unit, 1);
    }

    public decrementTime(unit: TimeUnit): void {
        this.addToTime(unit, -1);
    }

    public formatWith(formatString: string): string {
        return this.momentValue.format(formatString);
    }

    public getUnixTimestamp(): number {
        return this.momentValue.unix();
    }

    public setYear(year: number): void {
        const newValue = this.momentValue.clone().year(year);
        if (newValue.isBefore(this.min) || newValue.isAfter(this.max)) {
            // the new year is out of the allowed range
            return;
        }
        this.momentValue.year(year);
        this.triggerChange(this.getUnixTimestamp());

        this.updateCalendar(this.momentValue);
    }

    private updateByUnits(moment: Moment, unit: TimeUnit, value: number): Moment {
        switch (unit) {
            case 'hours':
                moment.hour(value);
                break;
            case 'minutes':
                moment.minute(value);
                break;
            case 'seconds':
                moment.second(value);
                break;
            default:
        }

        return moment;
    }

    /**
     * Create a momentjs locale from the (possibly localized) strings.
     *
     * @internal
     */
    private getMomentLocale(): string {
        const localeStrings = this.formatProvider.strings;
        const momentLocales = DateTimePickerControlsComponent.momentLocales;

        for (const [strings, locale] of momentLocales) {
            if (strings === localeStrings) {
                return locale;
            }
        }

        const newLocale: string = locale(`x-gtx-date-picker-${momentLocales.length}`, {
            months: localeStrings.months,
            monthsShort: localeStrings.monthsShort
                || (
                    localeStrings.months
                    && localeStrings.months.map(month => month.substr(0, 3))
                ),
            weekdays: localeStrings.weekdays,
            weekdaysMin: localeStrings.weekdaysMin
                || (
                    localeStrings.weekdays
                    && localeStrings.weekdays.map(weekday => weekday.substr(0, 2))
                ),
            week: {
                dow: localeStrings.weekStart ?? 0,
            },
        });
        momentLocales.push([localeStrings, newLocale]);

        return newLocale;
    }

    private determineDateOrder(): void {
        // Stringify 1999-08-22 with the dateProvider to determine the date order (D-M-Y, M-D-Y or Y-M-D).
        const time: string = this.formatProvider.format(unix(935272800000), false, false);
        const yearPos = time.indexOf('99');
        const monthPos = time.indexOf('8');
        const dayPos = time.indexOf('22');

        if (dayPos < monthPos && monthPos < yearPos) {
            this.dateOrder =  'dmy';
        } else if (monthPos < dayPos) {
            this.dateOrder =  'mdy';
        } else {
            this.dateOrder =  'ymd';
        }
    }

    /**
     * Increment or decrement the value and update the time object.
     */
    private addToTime(unit: TimeUnit, increment: number): void {
        const newValue = this.momentValue.clone().add(increment, unit);

        if ((this.min && newValue.isBefore(this.min)) || (this.max && newValue.isAfter(this.max))) {
            // the new time is out of the allowed range
            return;
        }

        this.momentValue.add(increment, unit);
        this.updateInternalValues();
        this.triggerChange(this.getUnixTimestamp());
    }

    /**
     * Update the time object based on the value of this.value.
     */
    private updateTimeObject(date: Moment): void {
        this.time.h = date.hour();
        this.time.m = date.minute();
        this.time.s = date.second();
    }

    /**
     * Update the Rome calendar widget with the current value.
     */
    private updateCalendar(value: Moment): void {
        if (this.calendarInstance) {
            this.calendarInstance.setValue(value);
        }
    }
}
