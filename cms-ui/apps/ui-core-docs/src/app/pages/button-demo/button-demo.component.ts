import { ChangeDetectionStrategy, Component } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './button-demo.component.html',
    styleUrls: ['./button-demo.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ButtonDemoPage {

    @InjectDocumentation('button.component')
    documentation: IDocumentation;

    buttonIsDisabled = false;
    clickCount = 0;
    formResult: any;
    demoForm = new UntypedFormGroup({
        firstName: new UntypedFormControl('John'),
        lastName: new UntypedFormControl('Doe'),
    });
}
