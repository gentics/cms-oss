import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnInit } from '@angular/core';
import { AlohaDateTimePickerComponent } from '@gentics/aloha-models';
import { DateTimePickerFormatProvider, DateTimePickerStrings, MomentLike, generateFormProvider } from '@gentics/ui-core';
import { BehaviorSubject, NEVER, Observable } from 'rxjs';
import { AlohaIntegrationService } from '../../providers';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-date-time-picker-renderer',
    templateUrl: './aloha-date-time-picker-renderer.component.html',
    styleUrls: ['./aloha-date-time-picker-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaDateTimePickerRendererComponent)],
})
export class AlohaDateTimePickerRendererComponent
    extends BaseAlohaRendererComponent<AlohaDateTimePickerComponent, Date>
    implements Required<Partial<DateTimePickerFormatProvider>>, OnInit {

    private stringChange = new BehaviorSubject<void>(null);

    strings: DateTimePickerStrings;
    changed$: Observable<any> = this.stringChange.asObservable();

    public timestamp: number | null;
    public self;

    constructor(
        changeDetector: ChangeDetectorRef,
        element: ElementRef<HTMLElement>,
        aloha: AlohaIntegrationService,
        private formatter: DateTimePickerFormatProvider,
    ) {
        super(changeDetector, element, aloha);
        this.self = this;
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.updateStrings();
        this.subscriptions.push(this.formatter.changed$.subscribe(() => {
            this.updateStrings();
            this.stringChange.next();
            this.changeDetector.markForCheck();
        }));
    }

    public format(date: MomentLike, displayTime: boolean, displaySeconds: boolean): string {
        return this.formatter.format(date, displayTime, displaySeconds);
    }

    public handleValueChange(timestamp: number): void {
        this.triggerChange(new Date(timestamp));
    }

    protected updateStrings(): void {
        const tmp: DateTimePickerStrings = {
            seconds: this.formatter.strings.seconds,
            okay: this.formatter.strings.okay,
            cancel: this.formatter.strings.cancel,

            // ---

            hours: this.settings.hoursLabel || this.formatter.strings.hours,
            minutes: this.settings.minutesLabel || this.formatter.strings.minutes,
            months: this.settings.monthNames || this.formatter.strings.months,
            monthsShort: this.settings.monthShort || this.formatter.strings.monthsShort,
            weekdays: this.settings.weekdayNames || this.formatter.strings.weekdays,
            weekdaysShort: this.settings.weekdayShort || this.formatter.strings.weekdaysShort,
            weekdaysMin: this.settings.weekdayMinimal || this.formatter.strings.weekdaysMin,
            weekStart: this.settings.weekStart ?? this.formatter.strings.weekStart,
        };

        this.strings = tmp;
    }

    protected override onValueChange(): void {
        super.onValueChange();

        this.timestamp = this.value == null ? null : this.value.getTime();
    }
}
