import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import {
    FormCondition,
    FormConditionSourceSchema,
    FormConditionSourceSetting,
    FormElement,
    FormElementConfiguration,
    FormSchema,
    FormSelectOption,
} from '@gentics/cms-models';
import { BaseFormElementComponent } from '@gentics/ui-core';

type SourceType = keyof FormConditionSourceSetting | keyof FormConditionSourceSchema;
const SOURCE_TYPES: SourceType[] = [
    'schema',
    /*
     * A setting as source don't actually make sense here, as these are for setting-conditions,
     * and not for element conditions. They just use the same condition source format.
     */
    // 'setting',
];
const SOURCE_TYPE_OPTIONS: { value: SourceType; translationKey: string }[] = SOURCE_TYPES.map((type) => ({
    value: type,
    translationKey: `form_grid.condition_source_type_${type}`,
}));

@Component({
    selector: 'gtx-form-condition-input',
    templateUrl: './form-condition-input.component.html',
    styleUrls: ['./form-condition-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormConditionInputComponent extends BaseFormElementComponent<FormCondition> {

    public readonly SOURCE_TYPE_OPTIONS = SOURCE_TYPE_OPTIONS;

    public readonly schema = input.required<FormSchema>();
    public readonly element = input.required<FormElement>();
    public readonly elementConfig = input.required<FormElementConfiguration>();
    public readonly elementMap = input.required<Record<string, FormElement>>();

    public readonly schemaOptions = computed<FormSelectOption[]>(() => {
        const elementMap = this.elementMap();

        return Object.keys(this.schema()?.properties || {}).map((key) => {
            // Can't have itself as a option
            if (key === this.element().id) {
                return null;
            }

            const uiEl = elementMap[key];
            if (!uiEl) {
                return null;
            }

            return {
                value: key,
                labelI18n: uiEl.label,
            };
        }).filter((option) => option != null);
    });

    public readonly settingOptions = computed<FormSelectOption[]>(() => {
        return (this.elementConfig().settings || []).map((setting) => {
            return {
                value: setting.id,
                labelI18n: setting.labelI18n,
            };
        });
    });

    public readonly sourceType = signal<SourceType | null>(null);
    public readonly hasSource = computed(() => {
        return this.sourceType() != null;
    });

    protected override onValueChange(): void {
        if (SOURCE_TYPES.length === 1) {
            // If we only have one type available, then we simply select that one
            this.sourceType.set(SOURCE_TYPES[0]);
        } else if (this.value == null || (this.value as any).source == null) {
            this.sourceType.set(null);
        } else {
            this.sourceType.set(Object.keys((this.value as any).source)
                .find((key) => SOURCE_TYPES.includes(key as any)) as any ?? null);
        }
    }

    public updateSourceType(type: string | number | (string | number)[]): void {
        this.sourceType.set(type as any);
    }

    public updateSourceTarget(value: null | string | number | (string | number)[]): void {
        if (value == null) {
            this.triggerChange(null);
            return;
        }

        this.triggerChange({
            ...this.value,
            source: {
                [this.sourceType() as any]: value as string,
            },
        } as any);
    }

    public updateCompareValue(value: string | number): void {
        this.triggerChange({
            ...this.value,
            equals: value,
        } as any);
    }
}
