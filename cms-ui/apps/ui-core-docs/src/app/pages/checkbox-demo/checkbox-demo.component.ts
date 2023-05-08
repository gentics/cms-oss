import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './checkbox-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckboxDemoPage {

    @InjectDocumentation('checkbox.component')
    documentation: IDocumentation;

    someBoolean = false;
    /* eslint-disable @typescript-eslint/naming-convention */
    checkStates: any = {
        A: true,
        B: false,
        C: 'indeterminate',
    };
    checkText = 'checked';
    statelessIsChecked = false;
}
