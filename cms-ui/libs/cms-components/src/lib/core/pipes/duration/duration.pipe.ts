import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
    name: 'gtxDuration',
})
export class DurationPipe implements PipeTransform {

    private readonly SECOND_FORMATTER = Intl.NumberFormat(this.translate.currentLang, {
        style: 'unit',
        unit: 'second',
        unitDisplay: 'short',
    } as any);
    private readonly MINUTE_FORMATTER = Intl.NumberFormat(this.translate.currentLang, {
        style: 'unit',
        unit: 'minute',
        unitDisplay: 'short',
    } as any);
    private readonly HOUR_FORMATTER = Intl.NumberFormat(this.translate.currentLang, {
        style: 'unit',
        unit: 'hour',
        unitDisplay: 'short',
    } as any);

    constructor(
        private translate: TranslateService,
    ) {}

    transform(value: number): any {
        if (!Number.isInteger(value)) {
            return value;
        }

        if (value < 90) {
            return this.SECOND_FORMATTER.format(value);
        }

        value = value / 60;
        if (value > 9) {
            value = Math.round(value);
        }

        if (value < 60) {
            return this.MINUTE_FORMATTER.format(value);
        }

        const minutes = value % 60;
        value = Math.trunc(value / 60);

        return `${this.HOUR_FORMATTER.format(value)} ${this.MINUTE_FORMATTER.format(minutes)}`;
    }
}
