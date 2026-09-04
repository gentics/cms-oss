import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Optional,
    Output,
} from '@angular/core';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { toValidNumber } from '@gentics/common';
import { DateTimePickerFormatProvider } from '../../providers/date-time-picker-format-provider/date-time-picker-format-provider.service';
import { ModalService } from '../../providers/modal/modal.service';
import { generateFormProvider } from '../../utils';
import { BaseFormElementComponent } from '../base-form-element/base-form-element.component';
import { DateTimePickerModal } from '../date-time-picker-modal/date-time-picker-modal.component';

/**
 * A form control for selecting a date and (optionally) a time.
 *
 * Depends on [ModalService](#/modal-service), which in turn
 * requires that the [`<gtx-overlay-host>`](#/overlay-host) is present in the app.
 *
 * ```html
 * <gtx-date-time-picker
 *     label="Date of Birth"
 *     displayTime="false"
 *     format="Do MMMM YYYY"
 *     [(ngModel)]="dateOfBirth"
 * ></gtx-date-time-picker>
 * ```
 */
@Component({
    selector: 'gtx-date-time-picker',
    templateUrl: './date-time-picker.component.html',
    styleUrls: ['./date-time-picker.component.scss'],
    providers: [generateFormProvider(DateTimePickerComponent)],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class DateTimePickerComponent
    extends BaseFormElementComponent<number>
    implements OnInit {

    /**
     * Set to overwrite texts and date formatting in the modal.
     */
    @Input()
    public formatProvider: DateTimePickerFormatProvider;

    /** Sets the date picker to be auto-focused. Handled by `AutofocusDirective`. */
    @Input()
    public autofocus = false;

    /** If true the clear button is displayed, which allows the user to clear the selected date. */
    @Input()
    public clearable: boolean = null;

    /** Value to set on the ngModel when the DatePicker is cleared. */
    @Input()
    public emptyValue: any = null;

    /** The minimum date allowed, e.g. `new Date(2015, 2, 12)`. */
    @Input()
    public min: Date;

    /** The maximum date allowed, e.g. `new Date(2031, 1, 30)`. */
    @Input()
    public max: Date;

    /** If true, the year may be selected from a Select control. */
    @Input()
    public selectYear = false;

    /** Set to `false` to omit the time picker part of the component. Defaults to `true`. */
    @Input()
    public displayTime = true;

    /** Set to `false` to omit the seconds of the time picker part. Defaults to `true`. */
    @Input()
    public displaySeconds = true;

    /** Placeholder which is shown if nothing is selected. */
    @Input()
    public placeholder = '';

    /** Fires when the "clear" button is clicked on a clearable DateTimePicker. */
    @Output()
    public clear = new EventEmitter<any>();

    /** The formatted date string which is displayed in the input to the user. */
    public displayValue = '';

    /** @internal */
    private dateValue: Date | null = null;

    constructor(
        changeDetector: ChangeDetectorRef,
        @Optional()
        private defaultFormatProvider: DateTimePickerFormatProvider,
        private modalService: ModalService,
    ) {
        super(changeDetector);
        this.booleanInputs.push('selectYear', 'displayTime', 'displaySeconds', 'clearable', 'autofocus');

        if (!defaultFormatProvider) {
            this.defaultFormatProvider = new DateTimePickerFormatProvider();
        }
    }

    ngOnInit(): void {
        this.subscriptions.push(
            (this.formatProvider || this.defaultFormatProvider).changed$.subscribe(() => this.updateDisplayValue()),
        );
    }

    protected onValueChange(): void {
        const timestamp = toValidNumber(this.value);

        if (timestamp == null) {
            this.dateValue = null;
            this.updateDisplayValue();
            return;
        }

        // No actual change
        if (this.dateValue != null && this.dateValue.getTime() === timestamp) {
            return;
        }

        // to milliseconds for the correct date
        this.dateValue = new Date(timestamp * 1000);

        this.updateDisplayValue();
    }

    handleEnterKey(event: KeyboardEvent): void {
        if (event.code === 'Enter' && !this.disabled) {
            this.showModal();
        }
    }

    async showModal(): Promise<void> {
        if (this.disabled) {
            return;
        }

        this.triggerTouch();

        try {
            const dialog = await this.modalService.fromComponent(DateTimePickerModal, {
                padding: false,
            }, {
                timestamp: this.dateValue == null || this.dateValue.getTime() === 0
                    ? Date.now() / 1000
                    : this.dateValue.getTime(),
                formatProvider: (this.formatProvider || this.defaultFormatProvider),
                displayTime: this.displayTime,
                displaySeconds: this.displaySeconds,
                min: this.min,
                max: this.max,
                selectYear: this.selectYear,
            });
            const timestamp: number = await dialog.open();
            this.triggerChange(timestamp);
        } catch (err) {
            // Ignore user close
            if (wasClosedByUser(err)) {
                return;
            }
            console.error('Error while opening the DateTimePicker Modal!', err);
        }
    }

    /** Format date to a human-readable string for displaying in the component's input field. */
    updateDisplayValue(): void {
        if (!this.value || !this.dateValue) {
            this.displayValue = '';
        } else {
            this.displayValue = (this.formatProvider || this.defaultFormatProvider).format(this.dateValue, this.displayTime, this.displaySeconds);
        }

        this.changeDetector.markForCheck();
    }

    /** Clear input value of DateTimePicker and emit `emptyValue` as value. */
    clearDateTime(): void {
        if (this.disabled || !this.dateValue) {
            return;
        }

        this.triggerTouch();
        this.triggerChange(this.emptyValue);
        this.clear.emit();
    }
}
