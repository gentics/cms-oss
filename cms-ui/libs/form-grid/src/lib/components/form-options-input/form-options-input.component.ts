import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { AbstractControl, ValidationErrors, Validator } from '@angular/forms';
import { cancelEvent } from '@gentics/common';
import { BaseFormElementComponent, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { DefaultableFormSelectOptionValue } from '../../models';

@Component({
    selector: 'gtx-form-options-input',
    templateUrl: './form-options-input.component.html',
    styleUrls: ['./form-options-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(FormOptionsInputComponent),
        generateValidatorProvider(FormOptionsInputComponent),
    ],
    standalone: false,
})
export class FormOptionsInputComponent extends BaseFormElementComponent<DefaultableFormSelectOptionValue[]> implements Validator {

    @Input()
    public placeholder: string;

    @Input()
    public languages: string[] = [];

    public invalidIndices: Set<number> = new Set();

    protected override onValueChange(): void {}

    public validate(_control: AbstractControl): ValidationErrors | null {
        if (!Array.isArray(this.value) || this.value.length === 0) {
            return null;
        }

        this.invalidIndices = new Set();
        const duplicates: Record<number, number[]> = {};
        let hasDupes = false;
        for (let i = 0; i < this.value.length; i++) {
            for (let k = i + 1; k < this.value.length; k++) {
                if (this.value[i].value === this.value[k].value) {
                    this.invalidIndices.add(i);
                    this.invalidIndices.add(k);
                    duplicates[i] ??= [];
                    duplicates[i].push(k);
                    duplicates[k] ??= [];
                    duplicates[k].push(i);
                    hasDupes = true;
                }
            }
        }

        return hasDupes ? { duplicates } : null;
    }

    public addNewOption(event?: Event): void {
        cancelEvent(event);
        this.triggerTouch();
        this.triggerChange([
            ...this.value || [],
            {
                value: '',
                label: {},
            },
        ]);
    }

    public removeOption(index: number): void {
        this.triggerTouch();
        // Create a clone so changes are detected
        const newItems = [...this.value || []];
        newItems.splice(index, 1);
        this.triggerChange(newItems);
    }

    public updateOptionValue(index: number, value: string | number): void {
        this.triggerTouch();
        // Create a clone so changes are detected
        const newItems = [...this.value || []];
        newItems[index].value = value as string;
        for (const lang of this.languages) {
            const option = newItems[index];
            if (
                !option.label[lang]
                // eslint-disable-next-line no-underscore-dangle
                || option._defaulted?.includes?.(lang)
            ) {
                option.label[lang] = value as string;
                // eslint-disable-next-line no-underscore-dangle
                option._defaulted ??= [];
                // eslint-disable-next-line no-underscore-dangle
                if (!option._defaulted.includes(lang)) {
                    // eslint-disable-next-line no-underscore-dangle
                    option._defaulted.push(lang);
                }
            }
        }
        this.triggerChange(newItems);
    }
}
