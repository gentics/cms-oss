import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { FormElement, FormElementConfiguration, FormSchema, FormSchemaProperty, FormTypeConfiguration } from '@gentics/cms-models';

@Component({
    selector: 'gtx-form-element-settings',
    templateUrl: './form-element-settings.component.html',
    styleUrls: ['./form-element-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormElementSettingsComponent {

    public readonly config = input.required<FormTypeConfiguration>();
    public readonly schema = input.required<FormSchema>();
    public readonly elementMap = input.required<Record<string, FormElement>>();

    public readonly element = model.required<FormElement>();
    public readonly elementConfig = input.required<FormElementConfiguration>();
    public readonly elementSchema = input<FormSchemaProperty>();

    public readonly disabled = input.required<boolean>();

    public readonly visibleSettings = computed(() => {
        const all = this.elementConfig().settings || [];
        return all.filter((setting) => !setting.backend);
    });

    public updateSettingValue(id: string, value: unknown): void {
        const copy = structuredClone(this.element());
        (copy.formGridOptions as any)[id] = value;
        this.element.set(copy);
    }

    public updateFormGridOptions(data: Record<string, any>): void {
        this.element.update((el) => {
            return {
                ...el,
                formGridOptions: data as any,
            };
        });
    }
}
