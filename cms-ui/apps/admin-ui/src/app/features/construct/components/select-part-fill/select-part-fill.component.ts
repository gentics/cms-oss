import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { DataSourceEntry, SelectTagPartProperty, TagPropertyType } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { pick } from'lodash-es'

@Component({
    selector: 'gtx-select-part-fill',
    templateUrl: './select-part-fill.component.html',
    styleUrls: ['./select-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SelectPartFillComponent)],
})
export class SelectPartFillComponent extends BaseFormElementComponent<SelectTagPartProperty> {

    @Input()
    public multiple = false;

    @Input()
    public clearable = false;

    @Input()
    public entries: DataSourceEntry[] = [];

    public selectedEntries: number[] = [];

    constructor(changeDetector: ChangeDetectorRef) {
        super(changeDetector);
        this.booleanInputs.push('multiple', 'clearable');
    }

    protected onValueChange(): void {
        this.selectedEntries = (this.value?.selectedOptions ?? []).map(option => Number(option.id));
        if (!this.multiple && this.selectedEntries.length > 1) {
            this.selectedEntries = [this.selectedEntries[0]];
            this.triggerChangeWithCurrentSelection();
        }
    }

    selectChanged(ids: number | number[]): void {
        if (ids == null) {
            this.selectedEntries = [];
        } else if (!Array.isArray(ids)) {
            this.selectedEntries = [ids];
        } else {
            this.selectedEntries = ids;
        }

        this.triggerChangeWithCurrentSelection();
    }

    private triggerChangeWithCurrentSelection(): void {
        const newValue: SelectTagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: this.multiple ? TagPropertyType.MULTISELECT : TagPropertyType.SELECT,
            options: this.entries,
            selectedOptions: (this.entries || [])
                .filter(entry => this.selectedEntries.findIndex(tmp => tmp === (entry.dsId || entry.id)) > -1)
                .map(entry => ({
                    id: entry.dsId || entry.id,
                    key: entry.key,
                    value: entry.value,
                })),
        };

        this.triggerChange(newValue);
    }

}
