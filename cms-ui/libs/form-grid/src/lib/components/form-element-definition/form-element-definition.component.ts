import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { FormControlConfiguration, FormPropertyValidation, FormSchemaProperty } from '@gentics/cms-models';

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
    public restricted = input.required<boolean>();

    public hasReadonlySettings = computed(() => {
        const d = this.schemaDraft();
        return [d.dateOnly, d.precision, d.searchReferenceValueMinLength, d.autoCompleteMinLength].some((v) => v != null);
    });

    public updateDraft(patch: Partial<FormSchemaProperty>): void {
        this.schemaDraft.set({ ...this.schemaDraft(), ...patch });
    }

    public updateValidation(patch: Partial<FormPropertyValidation>): void {
        this.updateDraft({ validation: { ...this.schemaDraft().validation, ...patch } });
    }

    public updateType(value: string | number | (string | number)[] | null): void {
        const type = Array.isArray(value) ? value[0] : value;
        this.updateDraft({ type: type != null ? String(type) : undefined });
    }

    public updateName(value: string | number | null): void {
        this.updateDraft({ name: value != null ? String(value) : undefined });
    }

    public updateValidationMinValue(value: string | number | null): void {
        this.updateValidation({ minValue: this.toNumber(value) });
    }

    public updateValidationMaxValue(value: string | number | null): void {
        this.updateValidation({ maxValue: this.toNumber(value) });
    }

    public updateValidationMinLength(value: string | number | null): void {
        this.updateValidation({ minLength: this.toNumber(value) });
    }

    public updateValidationMaxLength(value: string | number | null): void {
        this.updateValidation({ maxLength: this.toNumber(value) });
    }

    public updateValidationRegex(value: string | number | null): void {
        const regex = value != null ? String(value) : undefined;
        this.updateValidation({
            regexValidation: regex
                ? { errorMessage: this.schemaDraft().validation?.regexValidation?.errorMessage ?? {}, regex }
                : undefined,
        });
    }

    private toNumber(value: string | number | null): number | undefined {
        if (value == null) {
            return undefined;
        }
        const n = Number(value);
        return isNaN(n) ? undefined : n;
    }
}
