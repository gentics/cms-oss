import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { FormControlConfiguration, FormSchemaProperty } from '@gentics/cms-models';

@Component({
    selector: 'gtx-form-element-definition',
    templateUrl: './form-element-definition.component.html',
    styleUrls: ['./form-element-definition.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormElementDefinitionComponent {

    public schemaDraft = model.required<Partial<FormSchemaProperty>>();
    public controls = input.required<Record<string, FormControlConfiguration>>();

    public updateDraft(patch: Partial<FormSchemaProperty>): void {
        this.schemaDraft.set({ ...this.schemaDraft(), ...patch });
    }
}
