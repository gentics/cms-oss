import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableTagFamilyProperties } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider } from '@gentics/ui-core';

export enum TagFamilyPropertiesMode {
    CREATE,
    EDIT,
}

@Component({
    selector: 'gtx-mesh-tag-family-properties',
    templateUrl: './tag-family-properties.component.html',
    styleUrls: ['./tag-family-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(TagFamilyPropertiesComponent)],
})
export class TagFamilyPropertiesComponent extends BasePropertiesComponent<EditableTagFamilyProperties> {

    public readonly TagFamilyPropertiesMode = TagFamilyPropertiesMode;

    @Input()
    public mode: TagFamilyPropertiesMode;

    protected createForm(): FormGroup<FormProperties<EditableTagFamilyProperties>> {
        return new FormGroup<FormProperties<EditableTagFamilyProperties>>({
            name: new FormControl(this.value?.name, Validators.required),
        });
    }

    protected configureForm(_value: EditableTagFamilyProperties, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableTagFamilyProperties): EditableTagFamilyProperties {
        return value;
    }
}
