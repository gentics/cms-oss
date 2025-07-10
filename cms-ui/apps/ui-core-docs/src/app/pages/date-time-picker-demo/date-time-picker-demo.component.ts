import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

/* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment */
const MODELS_SOURCE = require('!!raw-loader!../../../../../../libs/ui-core/src/lib/common/date-time-picker.ts');
/* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment */
const PROVIDER_SOURCE = require('!!raw-loader!../../directives/demo-format/demo-format.directive.ts');

@Component({
    templateUrl: './date-time-picker-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DateTimePickerDemoPage {

    @InjectDocumentation('date-time-picker.component')
    documentation: IDocumentation;

    /* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-member-access */
    stringsInterfaceSource: string = MODELS_SOURCE.default;
    /* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-call, @typescript-eslint/no-unsafe-member-access */
    demoProviderSource = PROVIDER_SOURCE.default.split('\n').slice(3).join('\n');

    timestamp = 1457971763;
}
