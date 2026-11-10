import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormGroup, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { AnyModelType, PortalImplementation, PortalImplementationType } from '@gentics/cms-models';
import { BaseFormPropertiesComponent, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

/** Maximum length of keyword and name (see PortalImplementation.MAX_LENGTH in the backend) */
const MAX_LENGTH = 255;

@Component({
    selector: 'gtx-portal-implementation-properties',
    templateUrl: './portal-implementation-properties.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(PortalImplementationPropertiesComponent),
        generateValidatorProvider(PortalImplementationPropertiesComponent),
    ],
    standalone: false,
})
export class PortalImplementationPropertiesComponent extends BaseFormPropertiesComponent<PortalImplementation> {

    public readonly MAX_LENGTH = MAX_LENGTH;

    /** Possible types of portal implementations */
    public readonly TYPES = Object.values(PortalImplementationType);

    protected createForm(): FormGroup<any> {
        return new UntypedFormGroup({
            keyword: new UntypedFormControl(this.safeValue('keyword'), [Validators.required, Validators.maxLength(MAX_LENGTH)]),
            name: new UntypedFormControl(this.safeValue('name'), [Validators.required, Validators.maxLength(MAX_LENGTH)]),
            description: new UntypedFormControl(this.safeValue('description')),
            type: new UntypedFormControl(this.safeValue('type') || PortalImplementationType.PHP, Validators.required),
        });
    }

    protected configureForm(value: PortalImplementation<AnyModelType>, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: PortalImplementation<AnyModelType>): PortalImplementation<AnyModelType> {
        return value;
    }
}
