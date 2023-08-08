import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormControl, FormGroup } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { ListType, OverviewSetting, SelectType } from '@gentics/cms-models';
import { FormProperties, generateFormProvider } from '@gentics/ui-core';

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

    public form: FormGroup<FormProperties<OverviewSetting>>;
    public availableListTypes: ListType[] = [ListType.FILE, ListType.FOLDER, ListType.IMAGE, ListType.PAGE];
    public availableSelectTypes: SelectType[] = [SelectType.AUTO, SelectType.FOLDER, SelectType.MANUAL];

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
    }

    protected createForm(): FormGroup<FormProperties<OverviewSetting>> {
        return new FormGroup<FormProperties<OverviewSetting>>({
            listTypes: new FormControl(this.value?.listTypes || []),
            selectTypes: new FormControl(this.value?.selectTypes || []),
            hideSortOptions: new FormControl(this.value?.hideSortOptions ?? false),
            stickyChannel: new FormControl(this.value?.stickyChannel ?? false),
        });
    }

    protected configureForm(_value: OverviewSetting, _loud?: boolean): void {
        // Nothing to do
    }

    protected assembleValue(value: OverviewSetting): OverviewSetting {
        return value;
    }
}
