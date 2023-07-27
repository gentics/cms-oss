import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormArray, UntypedFormControl, ValidatorFn } from '@angular/forms';
import { CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import { DataSourceTagPartProperty, SelectOption, TagPropertyType } from '@gentics/cms-models';
import { BaseFormElementComponent, ISortableEvent, generateFormProvider } from '@gentics/ui-core';
import { isEqual, pick } from 'lodash';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

const validateSelectOption: ValidatorFn = (control: AbstractControl) => {
    return null;
}

@Component({
    selector: 'gtx-data-source-part-fill',
    templateUrl: './datasource-part-fill.component.html',
    styleUrls: ['./datasource-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(DataSourcePartFillComponent)],
})
export class DataSourcePartFillComponent extends BaseFormElementComponent<DataSourceTagPartProperty> implements OnInit {

    public form: UntypedFormArray;

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.form = new UntypedFormArray([]);
        this.rebuildForm(this.value?.options || []);

        this.subscriptions.push(combineLatest([
            this.form.valueChanges,
            this.form.statusChanges,
        ]).pipe(
            map(([value, status]) => status === 'VALID' ? value : null),
            distinctUntilChanged(isEqual),
        ).subscribe(value => {
            this.optionsUpdated(value);
        }));
    }

    protected onValueChange(): void {
        if ((this.value?.options || []).length !== (this.form?.length ?? 0) && (this.value as any) !== CONTROL_INVALID_VALUE) {
            this.rebuildForm(this.value?.options ?? []);
        }
    }

    protected onDisabledChange(): void {
        if (this.disabled) {
            this.form.disable({ emitEvent: false });
        } else {
            this.form.enable({ emitEvent: false });
        }
    }

    addOption(): void {
        this.form.push(new UntypedFormControl({ id: null, key: null, value: null }, validateSelectOption));
    }

    removeOption(index: number): void {
        this.form.removeAt(index);
    }

    sortOptions(event: ISortableEvent): void {
        const newOptions = event.sort(this.value.options);
        this.rebuildForm(newOptions);
        this.optionsUpdated(newOptions);
    }

    optionsUpdated(newOptions: SelectOption[]): void {
        const newValue: DataSourceTagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: TagPropertyType.DATASOURCE,
            options: newOptions,
        };

        this.triggerChange(newValue);
    }

    rebuildForm(options: SelectOption[]): void {
        if (!this.form) {
            return;
        }

        this.form.clear({ emitEvent: false });

        (options).forEach(option => {
            this.form.push(new UntypedFormControl(option, validateSelectOption), { emitEvent: false });
        })

        this.form.updateValueAndValidity();
    }

}
