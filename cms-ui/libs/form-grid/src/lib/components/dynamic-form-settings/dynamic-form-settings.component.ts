import { ChangeDetectionStrategy, Component, computed, effect, HostBinding, input, model, output, untracked } from '@angular/core';
import { FormControl, UntypedFormGroup, Validators } from '@angular/forms';
import {
    FormElement,
    FormElementConfiguration,
    FormSchemaProperty,
    FormSettingConfiguration,
    FormSettingType,
    FormTypeConfiguration,
    FormUserOptionReference,
    ItemInNode,
} from '@gentics/cms-models';
import { getValueByPath, setByPath } from '@gentics/common';
import { isSettingVisible } from '../../utils/conditions';
import { sanitizeItemReference } from '../../utils/sanitize';
import { Subscription } from 'rxjs';
import { setEnabled } from '@gentics/ui-core';

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
    public readonly languages = input.required<string[]>();
    public readonly validChange = output<boolean>();

    public readonly visibleSettings = computed(() => {
        const elConf = this.elementConfig();
        const el = this.element();
        const elSchema = this.elementSchema();

        return this.settings().filter((setting) => isSettingVisible(setting, elConf, el, elSchema));
    });

    private readonly elementId = computed(() => this.element().id);

    @HostBinding('class.has-settings')
    public hasVisibleSettings = false;

    public form!: UntypedFormGroup;

    constructor() {
        // Ugly workaround, as HostBindings don't work with signals
        effect(() => {
            this.hasVisibleSettings = this.visibleSettings().length > 0;
        });

        effect((cleanup) => {
            const settings = this.settings() || [];
            // only needed to re-create the whole form if the element-id changes.
            this.elementId();

            // Prevents changes to the data to re-create the entire form
            const disabled = untracked(() => this.disabled());
            const schema = untracked(() => this.elementSchema());
            const el = untracked(() => this.element());

            const controlSubs: Subscription[] = [];
            this.form = new UntypedFormGroup({});
            setEnabled(this.form, !disabled);

            // Create the form from the settings with the initial data and validators
            for (const setting of settings) {
                const path = setting.propertyPath
                    ? setting.propertyPath
                    : ['formGridOptions', setting.id];
                const control = new FormControl(getValueByPath(setting.backend ? schema : el, path));
                this.form.setControl(setting.id, control);

                if (setting.required) {
                    control.setValidators(Validators.required);
                }

                controlSubs.push(control.valueChanges.subscribe((value) => {
                    switch (setting.type) {
                        case FormSettingType.USER: {
                            this.updateUserValue(setting, value);
                            break;
                        }

                        case FormSettingType.REFERENCE:
                            this.updateReferenceValue(setting, value);
                            break;

                        default:
                            this.updateData(setting, value);
                            break;
                    }
                }));
            }

            // Setup the validation observer
            const statusSub = this.form.statusChanges.subscribe((status) => {
                this.validChange.emit(status === 'VALID');
            });

            cleanup(() => {
                statusSub.unsubscribe();
                controlSubs.forEach((s) => s.unsubscribe());
            });
        });

        effect(() => {
            const disabled = this.disabled();
            if (this.form != null) {
                setEnabled(this.form, !disabled);
            }
        });
    }

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

    public getTypeIfValid(setting: FormSettingConfiguration): string {
        if(Boolean(setting)) {
            return setting.type;
        }

        return "";
    }
}
