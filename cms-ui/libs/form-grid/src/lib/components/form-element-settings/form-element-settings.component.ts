import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { FormElement, FormElementConfiguration, FormSchema, FormTypeConfiguration, ItemInNode, ItemRef } from '@gentics/cms-models';

function sanitizeItemReference(item: ItemInNode): ItemRef {
    return {
        id: item.id,
        nodeId: item.nodeId,
        type: item.type as any,
        name: item.name,
    };
}

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

    public readonly disabled = input.required<boolean>();

    public updateSettingValue(id: string, value: unknown): void {
        const copy = structuredClone(this.element());
        (copy.formGridOptions as any)[id] = value;
        this.element.set(copy);
    }

    public updateReferenceValue(id: string, value: ItemInNode | ItemInNode[]): void {
        if (value == null) {
            this.updateSettingValue(id, value);
            return;
        }

        if (Array.isArray(value)) {
            this.updateSettingValue(id, value.map(sanitizeItemReference));
        } else {
            this.updateSettingValue(id, sanitizeItemReference(value));
        }
    }
}
