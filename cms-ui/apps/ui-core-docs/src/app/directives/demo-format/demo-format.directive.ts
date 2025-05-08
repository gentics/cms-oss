import { Directive, forwardRef } from '@angular/core';
import { DateTimePickerFormatProvider } from '@gentics/ui-core';
import { DemoDateFormatService } from '../../providers/demo-date-format/demo-date-format.service';

@Directive({
    selector: '[demo-format]',
    providers: [{
            provide: DateTimePickerFormatProvider,
            useExisting: forwardRef(() => DemoDateFormatService),
        }],
    standalone: false
})
export class DemoFormatDirective { }
