import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

/* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment */
const PROVIDER_SOURCE = require('!!raw-loader!../../providers/demo-date-format/demo-date-format.service.ts');

const TWO_WEEKS = 60 * 60 * 24 * 14 * 1000;
const TWO_WEEKS_AGO = Date.now() - TWO_WEEKS;
const TWO_WEEKS_HENCE = Date.now() + TWO_WEEKS;

@Component({
    templateUrl: './date-time-picker-controls-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DateTimePickerControlsDemoPage {

    @InjectDocumentation('date-time-picker-controls.component')
    documentation: IDocumentation;

    /* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-call, @typescript-eslint/no-unsafe-member-access */
    demoProviderSource = PROVIDER_SOURCE.default.split('\n').slice(3).join('\n');

    timestamp: number = Math.round(Date.now() / 1000);
    min = new Date(TWO_WEEKS_AGO);
    max = new Date(TWO_WEEKS_HENCE);
}
