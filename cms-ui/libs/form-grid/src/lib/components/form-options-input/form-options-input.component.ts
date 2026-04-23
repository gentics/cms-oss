import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormSelectOptionValue } from '@gentics/cms-models';
import { BaseFormElementComponent, cancelEvent, generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-form-options-input',
    templateUrl: './form-options-input.component.html',
    styleUrls: ['./form-options-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(FormOptionsInputComponent)],
    standalone: false,
})
export class FormOptionsInputComponent extends BaseFormElementComponent<FormSelectOptionValue[]> {

    protected override onValueChange(): void {
        console.log(this.value);
    }

    public addNewOption(event?: Event): void {
        cancelEvent(event);
        this.triggerChange([
            ...this.value || [],
            {
                value: '',
                label: {},
            },
        ]);
    }

    public removeOption(index: number): void {
        // Create a clone so changes are detected
        const newItems = [...this.value || []];
        newItems.splice(index, 1);
        this.triggerChange(newItems);
    }

    public updateOptionValue(index: number, value: string | number): void {
        // Create a clone so changes are detected
        const newItems = [...this.value || []];
        newItems[index].value = value as string;
        this.triggerChange(newItems);
    }
}
