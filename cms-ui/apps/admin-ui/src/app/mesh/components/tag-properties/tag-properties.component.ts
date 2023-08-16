import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableTagProperties } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider } from '@gentics/ui-core';

export enum TagPropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-tag-properties',
    templateUrl: './tag-properties.component.html',
    styleUrls: ['./tag-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(TagPropertiesComponent)],
})
export class TagPropertiesComponent extends BasePropertiesComponent<EditableTagProperties> {

    public readonly TagPropertiesMode = TagPropertiesMode;

    @Input()
    public mode: TagPropertiesMode;

    protected createForm(): FormGroup<FormProperties<EditableTagProperties>> {
        return new FormGroup<FormProperties<EditableTagProperties>>({
            name: new FormControl(this.value?.name, Validators.required),
        });
    }

    protected configureForm(_value: EditableTagProperties, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableTagProperties): EditableTagProperties {
        return value;
    }
}
