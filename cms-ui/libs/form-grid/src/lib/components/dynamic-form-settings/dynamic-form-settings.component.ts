import { ChangeDetectionStrategy, Component, computed, HostBinding, input, model } from '@angular/core';
import { FormSettingConfiguration, FormTypeConfiguration, ItemInNode, ItemRef } from '@gentics/cms-models';

function sanitizeItemReference(item: ItemInNode): ItemRef {
    return {
        id: item.id,
        nodeId: item.nodeId,
        type: item.type as any,
        name: item.name,
    };
}

@Component({
    selector: 'gtx-dynamic-form-settings',
    templateUrl: './dynamic-form-settings.component.html',
    styleUrls: ['./dynamic-form-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class DynamicFormSettingsComponent {

    public readonly config = input.required<FormTypeConfiguration>();
    public readonly settings = input.required<FormSettingConfiguration[]>();
    public readonly data = model.required<Record<string, any>>();

    public readonly disabled = input.required<boolean>();

    public readonly visibleSettings = computed(() => {
        // TODO: Filter conditions
        return this.settings();
    });

    @HostBinding('class.has-settings')
    public readonly hasVisibleSettings = computed(() => this.visibleSettings().length > 0);

    public updateData(key: string, value: unknown): void {
        this.data.update((data) => {
            return {
                ...data,
                [key]: value,
            };
        });
    }

    public updateReferenceValue(id: string, value: ItemInNode | ItemInNode[]): void {
        if (value == null) {
            this.updateData(id, value);
            return;
        }

        if (Array.isArray(value)) {
            this.updateData(id, value.map(sanitizeItemReference));
        } else {
            this.updateData(id, sanitizeItemReference(value));
        }
    }
}
