import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import { ListType, OverviewSetting, SelectType } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

@Component({
    selector: 'gtx-overview-part-settings',
    templateUrl: './overview-part-settings.component.html',
    styleUrls: ['./overview-part-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(OverviewPartSettingsComponent)],
})
export class OverviewPartSettingsComponent extends BaseFormElementComponent<OverviewSetting> implements ControlValueAccessor, OnInit, OnDestroy {

    public form: UntypedFormGroup;
    public availableListTypes: ListType[] = [ListType.FILE, ListType.FOLDER, ListType.IMAGE, ListType.PAGE];
    public availableSelectTypes: SelectType[] = [SelectType.AUTO, SelectType.FOLDER, SelectType.MANUAL];

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.form = new UntypedFormGroup({
            listTypes: new UntypedFormControl([]),
            selectTypes: new UntypedFormControl([]),
            hideSortOptions: new UntypedFormControl(false),
            stickyChannel: new UntypedFormControl(false),
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

    protected onValueChange(): void {
        if (this.form && (this.value as any) !== CONTROL_INVALID_VALUE) {
            this.form.setValue({
                listTypes: [],
                selectTypes: [],
                hideSortOptions: false,
                stickyChannel: false,
                ...this.value,
            });
        }
    }

    setDisabledState(isDisabled: boolean): void {
        super.setDisabledState(isDisabled);

        if (isDisabled) {
            this.form.disable({ emitEvent: false });
        } else {
            this.form.enable({ emitEvent: false });
        }
    }
}
