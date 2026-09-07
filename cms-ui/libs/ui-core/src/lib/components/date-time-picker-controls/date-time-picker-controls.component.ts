import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    HostBinding,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Optional,
    Output,
    SimpleChanges,
} from '@angular/core';
import { dateInYears } from '@gentics/common';
import { NEVER, Subscription } from 'rxjs';
import { startWith } from 'rxjs/operators';
import { BaseFormElementComponent } from '../../components/base-form-element/base-form-element.component';
import { DateTimePickerFormatProvider } from '../../providers/date-time-picker-format-provider/date-time-picker-format-provider.service';
import { generateFormProvider, normalizeToDate } from '../../utils';

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
    standalone: false,
})
export class DateTimePickerControlsComponent
    extends BaseFormElementComponent<number>
    implements OnInit, OnChanges, OnDestroy {

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

    @Output()
    public override valueChange = new EventEmitter<number>();

    /**
     * When `true`, the controls use the "compact" (small screen) styling for all screen sizes. Defaults to `false`
     */
    @Input()
    @HostBinding('class.compact')
    public compact = false;

    public dateValue: Date | null = null;

    /** The order of how the date is supposed to be displayed. */
    public dateOrder: 'dmy' | 'ymd' | 'mdy' = 'mdy';

    /** List of selectable years */
    public years: number[] = [];

    public selectedYear: number;

    /** The time value */
    public time: any = {
        h: 0,
        m: 0,
        s: 0,
    };

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
        this.updateDateValue(normalizeToDate(this.value));
        this.updateYears();
    }

    override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if ((changes.min && !changes.min.firstChange) || (changes.max && !changes.max.firstChange)) {
            this.convertRanges();
            this.updateYears();
            this.updateDateValue(this.dateValue);
        }
        if (changes.formatProvider && !changes.formatProvider.firstChange) {
            this.setupProviderChangeHook();
        }
    }

    override ngOnDestroy(): void {
        super.ngOnDestroy();

        if (this.providerSubscription) {
            this.providerSubscription.unsubscribe();
        }
    }

    protected onValueChange(force = false): void {
        const timestamp = asCleanNumber(this.value);

        // If the value is actually the same, then we don't need a change
        if (!force && this.dateValue != null && this.dateValue.getTime() === timestamp) {
            return;
        }

        this.updateDateValue(normalizeToDate(this.value));
    }

    protected updateDateValue(date: Date | null): void {
        if (date == null) {
            this.dateValue = null;
            return;
        }

        // Clamp the value
        if (this.min > date) {
            date = new Date(this.min);
        } else if (this.max < date) {
            date = new Date(this.max);
        }

        // Same value, skip it
        if (this.dateValue != null && date.getTime() === this.dateValue.getTime()) {
            return;
        }

        this.dateValue = date;
        this.selectedYear = this.dateValue.getFullYear();
        this.updateTimeObject(this.dateValue);
        this.triggerChange(this.dateValue.getTime());
    }

    protected convertRanges(): void {
        this.min = toDate(this.min) || dateInYears(-80);
        this.max = toDate(this.max) || dateInYears(20);
    }

    protected updateYears(): void {
        const minYear = this.min.getFullYear();
        const maxYear = this.max.getFullYear();

        this.years = [];
        for (let year = minYear; year <= maxYear; year++) {
            this.years.push(year);
        }
    }

    protected setupProviderChangeHook(): void {
        // Update strings and date format when format provider emits a change
        this.providerSubscription = (this.formatProvider.changed$ || NEVER)
            .pipe(startWith(null))
            .subscribe(() => {
                this.updateTimeObject(this.dateValue);
                this.determineDateOrder();
            });
    }

    /**
     * Update the this.value in accordance with the input of one of the
     * time fields (h, m, s).
     */
    public updateTime(unit: TimeUnit, value: number, event?: Event): void {
        this.handleBlur(event);

        const newValue = this.updateByUnits(this.dateValue, unit, value);
        this.updateDateValue(newValue);
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

    public setYear(year: number): void {
        const newValue = new Date(this.dateValue);
        newValue.setFullYear(year);
        this.updateDateValue(newValue);
    }

    private updateByUnits(date: Date, unit: TimeUnit, value: number): Date {
        switch (unit) {
            case 'hours':
                date.setHours(date.getHours() + value);
                break;
            case 'minutes':
                date.setMinutes(date.getMinutes() + value);
                break;
            case 'seconds':
                date.setSeconds(date.getSeconds() + value);
                break;
            default:
        }

        return date;
    }

    private determineDateOrder(): void {
        this.dateOrder = this.formatProvider.getDateOrder();
    }

    /**
     * Increment or decrement the value and update the time object.
     */
    private addToTime(unit: TimeUnit, increment: number): void {
        const newValue = new Date(this.dateValue);
        this.updateByUnits(newValue, unit, increment);
        this.updateDateValue(newValue);
    }

    /**
     * Update the time object based on the value of this.value.
     */
    private updateTimeObject(date: Date): void {
        if (date == null) {
            return;
        }

        this.time.h = date.getHours();
        this.time.m = date.getMinutes();
        this.time.s = date.getSeconds();
    }
}
