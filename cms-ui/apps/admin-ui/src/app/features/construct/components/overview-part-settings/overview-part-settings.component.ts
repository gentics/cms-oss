import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormGroup, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { ListType, OverviewSetting, SelectType } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-overview-part-settings',
    templateUrl: './overview-part-settings.component.html',
    styleUrls: ['./overview-part-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(OverviewPartSettingsComponent)],
})
export class OverviewPartSettingsComponent
    extends BasePropertiesComponent<OverviewSetting>
    implements ControlValueAccessor, OnInit, OnDestroy {

    public form: UntypedFormGroup;
    public availableListTypes: ListType[] = [ListType.FILE, ListType.FOLDER, ListType.IMAGE, ListType.PAGE];
    public availableSelectTypes: SelectType[] = [SelectType.AUTO, SelectType.FOLDER, SelectType.MANUAL];

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    protected createForm(): FormGroup<any> {
        return new UntypedFormGroup({
            listTypes: new UntypedFormControl([]),
            selectTypes: new UntypedFormControl([]),
            hideSortOptions: new UntypedFormControl(false),
            stickyChannel: new UntypedFormControl(false),
        });
    }

    protected configureForm(value: OverviewSetting, loud?: boolean): void {
        // Nothing to do
    }

    protected assembleValue(value: OverviewSetting): OverviewSetting {
        return value;
    }
}
