import { ChangeDetectionStrategy, Component, Input, SimpleChanges, OnChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { EditableProjectProperties, ProjectCreateRequest } from '@gentics/mesh-models';
import { FormProperties, generateFormProvider, generateValidatorProvider, setControlsEnabled } from '@gentics/ui-core';

export enum ProjectPropertiesMode {
    CREATE = 'create',
    EDIT = 'edit',
}

@Component({
    selector: 'gtx-mesh-project-properties',
    templateUrl: './project-properties.component.html',
    styleUrls: ['./project-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ProjectPropertiesComponent),
        generateValidatorProvider(ProjectPropertiesComponent),
    ],
})
export class ProjectPropertiesComponent extends BasePropertiesComponent<EditableProjectProperties | ProjectCreateRequest>
    implements OnChanges {

    public readonly ProjectPropertiesMode = ProjectPropertiesMode;

    @Input()
    public mode: ProjectPropertiesMode;

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.mode && this.form) {
            this.updateFormModeControls();
        }
    }

    protected createForm(): FormGroup<FormProperties<EditableProjectProperties | ProjectCreateRequest>> {
        return new FormGroup<FormProperties<EditableProjectProperties | ProjectCreateRequest>>({
            name: new FormControl(this.value?.name, Validators.required),
            schema: new FormControl({ value: null, disabled: this.mode !== ProjectPropertiesMode.CREATE }, Validators.required),
            hostname: new FormControl({ value: '', disabled: this.mode !== ProjectPropertiesMode.CREATE }),
            pathPrefix: new FormControl({ value: '', disabled: this.mode !== ProjectPropertiesMode.CREATE }),
            ssl: new FormControl({ value: false, disabled: this.mode !== ProjectPropertiesMode.CREATE }),
        });
    }

    protected configureForm(_value: EditableProjectProperties | ProjectCreateRequest, _loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: EditableProjectProperties | ProjectCreateRequest): EditableProjectProperties | ProjectCreateRequest {
        return value;
    }

    protected updateFormModeControls(): void {
        setControlsEnabled(
            this.form,
            ['schema', 'hostname', 'pathPrefix', 'ssl'],
            this.mode === ProjectPropertiesMode.CREATE,
            { emitEvent: false },
        );
    }
}
