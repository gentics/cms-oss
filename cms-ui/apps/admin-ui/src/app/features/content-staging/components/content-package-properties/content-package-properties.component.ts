import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { ContentPackageCreateRequest, ContentPackageSaveRequest } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export enum ContentPackagePropertiesMode {
    UPDATE = 'update',
    CREATE = 'create',
}

type ContentPackagePropertiesValue = ContentPackageSaveRequest | ContentPackageCreateRequest;

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
export class ContentPackagePropertiesComponent extends BasePropertiesComponent<ContentPackagePropertiesValue> {

    @Input()
    public mode: ContentPackagePropertiesMode = ContentPackagePropertiesMode.UPDATE;

    protected override createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            name: new UntypedFormControl(this.value?.name, Validators.required),
            description: new UntypedFormControl(this.value?.description),
        });
    }

    protected override configureForm(value: ContentPackagePropertiesValue, loud?: boolean): void {
        // Nothing to do
    }

    protected override assembleValue(value: ContentPackagePropertiesValue): ContentPackagePropertiesValue {
        return value;
    }
}
