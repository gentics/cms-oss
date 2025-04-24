import { AppStateService } from '@admin-ui/state';
import { Pipe, PipeTransform } from '@angular/core';

const DEFAULT_FORMAT_OPTIONS: Intl.NumberFormatOptions = {
    style: 'decimal',
    minimumIntegerDigits: 1,
    maximumFractionDigits: 2,
    minimumFractionDigits: 0,
};

@Pipe({ name: 'gtxI18nNumber' })
export class I18nNumberPipe implements PipeTransform {

    constructor(
        protected appState: AppStateService,
    ) {}

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    transform(value: any, options?: Intl.NumberFormatOptions): string {
        if (typeof value !== 'number' || !Number.isFinite(value) || Number.isNaN(value)) {
            return '';
        }

        // Could be done with a subscription and an unpure pipe, but since
        // changing the language requires a page/app reload anyways, there's no point.
        // And pure pipes are always better.
        const lang = this.appState.now.ui.language;
        options ??= DEFAULT_FORMAT_OPTIONS;

        const formatter = new Intl.NumberFormat(lang, options);
        return formatter.format(value);
    }
}
