import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
    name: 'duration',
})
export class DurationPipe implements PipeTransform {

    private readonly secFormatter = Intl.NumberFormat(this.translate.currentLang, {
        style: 'unit',
        unit: 'second',
        unitDisplay: 'short',
    } as any);
    private readonly minFormatter = Intl.NumberFormat(this.translate.currentLang, {
        style: 'unit',
        unit: 'minute',
        unitDisplay: 'short',
    } as any);
    private readonly hourFormatter = Intl.NumberFormat(this.translate.currentLang, {
        style: 'unit',
        unit: 'hour',
        unitDisplay: 'short',
    } as any);

    constructor(
        private translate: TranslateService,
    ) {}

    transform(value: number, ...args: any[]): any {
        if (!Number.isInteger(value)) {
            return value;
        }

        if (value < 90) {
            return this.secFormatter.format(value);
        }

        value = value / 60;
        if (value > 9) {
            value = Math.round(value);
        }

        if (value < 60) {
            return this.minFormatter.format(value);
        }

        const minutes = value % 60;
        value = Math.trunc(value / 60);

        return `${this.hourFormatter.format(value)} ${this.minFormatter.format(minutes)}`;
    }
}
