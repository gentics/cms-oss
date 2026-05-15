import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import {
    FormControlConfiguration,
    FormElement,
    FormSchema,
    FormSchemaProperty,
    FormSettingType,
    FormTypeConfiguration,
} from '@gentics/cms-models';
import { FormGridEditMode } from '../../models';

@Component({
    selector: 'gtx-form-element-definition',
    templateUrl: './form-element-definition.component.html',
    styleUrls: ['./form-element-definition.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormElementDefinitionComponent {

    public readonly FormGridEditMode = FormGridEditMode;

    public readonly mode = input.required<FormGridEditMode>();
    public readonly config = input.required<FormTypeConfiguration>();
    public readonly schema = input.required<FormSchema>();

    public readonly element = model.required<FormElement>();
    public readonly elementConfig = input.required<FormControlConfiguration>();
    public readonly elementSchema = model.required<FormSchemaProperty>();

    public readonly controls = computed(() => {
        return this.config().controls || {};
    });

    public visibleSettings = computed(() => {
        const all = this.elementConfig().settings || [];
        return all.filter((setting) => setting.type !== FormSettingType.TRANSLATION && setting.backend);
    });

    public updateElementSchema(patch?: Partial<FormSchemaProperty>): void {
        this.elementSchema.set({ ...this.elementSchema(), ...patch });
    }

    public updateFormGridOptions(data: Record<string, any>): void {
        this.elementSchema.update((el) => {
            return {
                ...el,
                formGridOptions: data as any,
            };
        });
    }
}
