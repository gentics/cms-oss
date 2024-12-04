import { createBlacklistValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { SelectOption } from '@gentics/cms-models';
import { FormProperties, generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-select-option-input',
    templateUrl: './select-option-input.component.html',
    styleUrls: ['./select-option-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SelectOptionInputComponent)],
})
export class SelectOptionInputComponent extends BasePropertiesComponent<SelectOption> implements OnChanges {

    @Input()
    public keyBlacklist: string[] = [];

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.keyBlacklist && this.form) {
            this.form.controls.key.updateValueAndValidity();
        }
    }

    protected createForm(): FormGroup<any> {
        return new FormGroup<FormProperties<SelectOption>>({
            id: new FormControl(this.safeValue('id')),
            key: new FormControl(this.safeValue('key'), [Validators.required, createBlacklistValidator(() => this.keyBlacklist)]),
            value: new FormControl(this.safeValue('value'), Validators.required),
        });
    }

    protected configureForm(value: SelectOption, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: SelectOption): SelectOption {
        return value;
    }
}
