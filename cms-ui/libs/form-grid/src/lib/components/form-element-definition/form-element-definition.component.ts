import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import {
    FormControlConfiguration,
    FormOptionsType,
    FormPropertyValidation,
    FormSchema,
    FormSchemaProperty,
    FormSelectOptionValue,
} from '@gentics/cms-models';
import { toValidNumber } from '@gentics/ui-core';

@Component({
    selector: 'gtx-form-element-definition',
    templateUrl: './form-element-definition.component.html',
    styleUrls: ['./form-element-definition.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormElementDefinitionComponent {

    public readonly FormOptionsType = FormOptionsType;

    public readonly schema = input.required<FormSchema>();
    public readonly controls = input.required<Record<string, FormControlConfiguration>>();

    public readonly elementSchema = model.required<FormSchemaProperty>();
    public readonly elementConfig = input.required<FormControlConfiguration>();

    public readonly disabled = input.required<boolean>();

    public hasReadonlySettings = computed(() => {
        const ref = this.elementSchema();
        return [ref.dateOnly, ref.precision, ref.searchReferenceValueMinLength, ref.autoCompleteMinLength].some((v) => v != null);
    });

    public updateElementSchema(patch: Partial<FormSchemaProperty>): void {
        this.elementSchema.set({ ...this.elementSchema(), ...patch });
    }

    public updateValidation(patch: Partial<FormPropertyValidation>): void {
        this.updateElementSchema({ validation: { ...this.elementSchema().validation, ...patch } });
    }

    public updateType(value: string | number | (string | number)[] | null): void {
        const type = Array.isArray(value) ? value[0] : value;
        this.updateElementSchema({ type: type != null ? `${type}` : undefined });
    }

    public updateName(value: string | number | null): void {
        this.updateElementSchema({ name: value != null ? `${value}` : undefined });
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

    public updateStaticOptions(value: FormSelectOptionValue[]): void {
        this.updateElementSchema({ staticOptions: value });
    }

    public updateValidationRegex(value: string | number | null): void {
        const regex = value != null ? `${value}` : undefined;
        this.updateValidation({
            regexValidation: regex
                ? { errorMessage: this.elementSchema().validation?.regexValidation?.errorMessage ?? {}, regex }
                : undefined,
        });
    }
}
