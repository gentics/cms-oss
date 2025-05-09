import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableContentPackage } from '@gentics/cms-models';
import { FormProperties, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export enum ContentPackagePropertiesMode {
    UPDATE = 'update',
    CREATE = 'create',
}

@Component({
    selector: 'gtx-content-package-properties',
    templateUrl: './content-package-properties.component.html',
    styleUrls: ['./content-package-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ContentPackagePropertiesComponent),
        generateValidatorProvider(ContentPackagePropertiesComponent),
    ],
})
export class ContentPackagePropertiesComponent extends BasePropertiesComponent<EditableContentPackage> {

    @Input()
    public mode: ContentPackagePropertiesMode = ContentPackagePropertiesMode.UPDATE;

    protected override createForm(): FormGroup<FormProperties<EditableContentPackage>> {
        return new FormGroup<FormProperties<EditableContentPackage>>({
            name: new FormControl(this.safeValue('name'), Validators.required),
            description: new FormControl(this.safeValue('description')),
        });
    }

    protected override configureForm(value: EditableContentPackage, loud?: boolean): void {
        // Nothing to do
    }

    protected override assembleValue(value: EditableContentPackage): EditableContentPackage {
        return value;
    }
}
