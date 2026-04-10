import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { FormElement, FormElementConfiguration, FormTypeConfiguration } from '@gentics/cms-models';

@Component({
    selector: 'gtx-form-element-settings',
    templateUrl: './form-element-settings.component.html',
    styleUrls: ['./form-element-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormElementSettingsComponent {

    public config = input.required<FormTypeConfiguration>();
    public element = model.required<FormElement>();
    public elementConfig = input.required<FormElementConfiguration>();

    public updateSettingValue(id: string, value: unknown): void {
        const copy = structuredClone(this.element());
        (copy.formGridOptions as any)[id] = value;
        this.element.set(copy);
    }
}
