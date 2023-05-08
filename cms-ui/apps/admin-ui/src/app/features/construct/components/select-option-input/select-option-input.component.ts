import { blacklistValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { SelectOption } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

@Component({
    selector: 'gtx-select-option-input',
    templateUrl: './select-option-input.component.html',
    styleUrls: ['./select-option-input.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SelectOptionInputComponent)],
})
export class SelectOptionInputComponent extends BaseFormElementComponent<SelectOption> implements OnInit, OnChanges {

    @Input()
    public keyBlacklist: string[] = [];

    public form: UntypedFormGroup;

    ngOnInit(): void {
        this.form = new UntypedFormGroup({
            id: new UntypedFormControl(null),
            key: new UntypedFormControl(null, [Validators.required, blacklistValidator(this.keyBlacklist)]),
            value: new UntypedFormControl(null, Validators.required),
        });

        this.subscriptions.push(combineLatest([
            this.form.valueChanges,
            this.form.statusChanges,
        ]).pipe(
            map(([value, status]) => status === 'VALID' ? value : null),
            distinctUntilChanged(isEqual),
        ).subscribe(value => {
            this.triggerChange(value);
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.keyBlacklist && this.form) {
            this.form.get('key').setValidators([Validators.required, blacklistValidator(this.keyBlacklist)]);
        }
    }

    protected onValueChange(): void {
        try {
            this.form.setValue(this.value || {});
        } catch (err) {
            console.warn('Error while updating select-option-input form', err);
        }
    }
}
