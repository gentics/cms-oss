import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { FormControlConfiguration, FormPropertyValidation, FormSchemaProperty } from '@gentics/cms-models';
import { toValidNumber } from '@gentics/ui-core';

@Component({
    selector: 'gtx-form-element-definition',
    templateUrl: './form-element-definition.component.html',
    styleUrls: ['./form-element-definition.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormElementDefinitionComponent {

    public readonly disabled = input.required<boolean>();
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
        this.updateDraft({ type: type != null ? `${type}` : undefined });
    }

    public updateName(value: string | number | null): void {
        this.updateDraft({ name: value != null ? `${value}` : undefined });
    }

    public updateValidationMinValue(value: string | number | null): void {
        this.updateValidation({ minValue: toValidNumber(value) ?? undefined });
    }

    public updateValidationMaxValue(value: string | number | null): void {
        this.updateValidation({ maxValue: toValidNumber(value) ?? undefined });
    }

    public updateValidationMinLength(value: string | number | null): void {
        this.updateValidation({ minLength: toValidNumber(value) ?? undefined });
    }

    public updateValidationMaxLength(value: string | number | null): void {
        this.updateValidation({ maxLength: toValidNumber(value) ?? undefined });
    }

    public updateValidationRegex(value: string | number | null): void {
        const regex = value != null ? `${value}` : undefined;
        this.updateValidation({
            regexValidation: regex
                ? { errorMessage: this.schemaDraft().validation?.regexValidation?.errorMessage ?? {}, regex }
                : undefined,
        });
    }
}
