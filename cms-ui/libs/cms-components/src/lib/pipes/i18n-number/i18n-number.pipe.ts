import { Pipe, PipeTransform } from '@angular/core';
import { I18nService } from '../../providers/i18n/i18n.service';

const DEFAULT_FORMAT_OPTIONS: Intl.NumberFormatOptions = {
    style: 'decimal',
    minimumIntegerDigits: 1,
    maximumFractionDigits: 2,
    minimumFractionDigits: 0,
};

@Pipe({
    name: 'gtxI18nNumber',
    standalone: false,
})
export class I18nNumberPipe implements PipeTransform {

    constructor(
        protected i18n: I18nService,
    ) {}

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    transform(value: any, options?: Intl.NumberFormatOptions): string {
        if (typeof value !== 'number' || !Number.isFinite(value) || Number.isNaN(value)) {
            return '';
        }

        const lang = this.i18n.getCurrentLanguage();
        options ??= DEFAULT_FORMAT_OPTIONS;

        const formatter = new Intl.NumberFormat(lang, options);
        return formatter.format(value);
    }
}
