import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { DateTimePickerFormatProvider } from '../../providers/date-time-picker-format-provider/date-time-picker-format-provider.service';
import { BaseModal } from '../base-modal/base-modal.component';

/**
 * The modal powering the `DateTimePicker` component.
 * For Input documentation, see the `DateTimePickerControls` component.
 */
@Component({
    selector: 'gtx-date-time-picker-modal',
    templateUrl: './date-time-picker-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DateTimePickerModal extends BaseModal<number> {

    @Input()
    public timestamp: number;

    @Input()
    public displayTime = true;

    @Input()
    public displaySeconds = true;

    @Input()
    public min: Date;

    @Input()
    public max: Date;

    @Input()
    public selectYear: boolean;

    constructor(
        public formatProvider: DateTimePickerFormatProvider,
    ) {
        super();
    }

    updateTimestamp(timestamp: number): void {
        this.timestamp = timestamp;
    }

    okayClicked(): void {
        this.closeFn(this.timestamp);
    }
}
