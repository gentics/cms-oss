import { ChangeDetectionStrategy, Component } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { IDocumentation } from '../../common/docs';
import { InjectDocumentation } from '../../common/docs-loader';

@Component({
    templateUrl: './input-demo.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InputDemoPage {

    @InjectDocumentation('input.component')
    documentation: IDocumentation;

    name = 'Foo';
    readonly: boolean;
    disabled: boolean;
    required: boolean;
    addressForm: UntypedFormGroup = new UntypedFormGroup({
        name: new UntypedFormGroup({
            first: new UntypedFormControl('John'),
            last: new UntypedFormControl('Doe'),
        }),
        address: new UntypedFormGroup({
            streetName: new UntypedFormControl(''),
        }),
    });
}
