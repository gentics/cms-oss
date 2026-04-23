import { ChangeDetectionStrategy, Component, computed, HostBinding, input, model } from '@angular/core';
import {
    FormElement,
    FormElementConfiguration,
    FormSchemaProperty,
    FormSettingConfiguration,
    FormTypeConfiguration,
    FormUserOptionReference,
    ItemInNode,
} from '@gentics/cms-models';
import { setByPath } from '@gentics/ui-core';
import { isSettingVisible } from '../../utils/conditions';
import { sanitizeItemReference } from '../../utils/sanitize';

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
    public readonly element = model.required<FormElement>();
    public readonly elementConfig = input.required<FormElementConfiguration>();
    public readonly elementSchema = model<FormSchemaProperty>();

    public readonly disabled = input.required<boolean>();

    public readonly visibleSettings = computed(() => {
        const elConf = this.elementConfig();
        const el = this.element();
        const elSchema = this.elementSchema();

        return this.settings().filter((setting) => isSettingVisible(setting, elConf, el, elSchema));
    });

    @HostBinding('class.has-settings')
    public readonly hasVisibleSettings = computed(() => this.visibleSettings().length > 0);

    public updateData(setting: FormSettingConfiguration, value: unknown): void {
        const path = setting.propertyPath
            ? setting.propertyPath
            : ['formGridOptions', setting.id];

        if (setting.backend) {
            this.elementSchema.update((data) => {
                const copy = structuredClone(data);
                setByPath(copy, path, value);
                return copy;
            });
        } else {
            this.element.update((data) => {
                const copy = structuredClone(data);
                setByPath(copy, path, value);
                return copy;
            });
        }
    }

    public updateUserValue(setting: FormSettingConfiguration, value: number | string | (string | number)[]): void {
        let actualValue: FormUserOptionReference | null = null;

        if (value != null) {
            actualValue = {
                userReference: value as string | string[],
            };
        }

        this.updateData(setting, actualValue);
    }

    public updateReferenceValue(setting: FormSettingConfiguration, value: ItemInNode | ItemInNode[]): void {
        if (value == null) {
            this.updateData(setting, value);
            return;
        }

        if (Array.isArray(value)) {
            this.updateData(setting, value.map(sanitizeItemReference));
        } else {
            this.updateData(setting, sanitizeItemReference(value));
        }
    }
}
