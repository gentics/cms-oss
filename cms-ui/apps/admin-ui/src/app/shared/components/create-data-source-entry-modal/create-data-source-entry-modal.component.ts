import { DataSourceEntryOperations } from '@admin-ui/core/providers/operations/datasource-entry';
import { SelectState } from '@admin-ui/state';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { DataSourceEntryBO, DataSourceEntryCreateRequest, NormalizableEntityType } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { combineLatest, Observable } from 'rxjs';
import { filter, map, switchMap, take } from 'rxjs/operators';

@Component({
    selector: 'gtx-create-data-source-entry-modal',
    templateUrl: './create-data-source-entry-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateDataSourceEntryModalComponent implements IModalDialog, OnInit {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<string>;

    /** form instance */
    form: UntypedFormGroup;

    constructor(
        private dataSourceEntries: DataSourceEntryOperations,
    ) { }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            key: new UntypedFormControl(null),
            value: new UntypedFormControl(null),
            /*order: new FormControl(null),*/
        });
    }

    closeFn = (entityCreated: DataSourceEntryBO) => { };
    cancelFn = (val?: any) => { };

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (entityCreated: DataSourceEntryBO) => {
            close(entityCreated);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    /** Get form validity state */
    isValid(): boolean {
        return this.form.valid;
    }

    /**
     * If user clicks to create a new dataSource
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(dataSourceCreated => this.closeFn(dataSourceCreated));
    }

    private createEntity(): Promise<DataSourceEntryBO> {

        // assemble payload with conditional properties
        const dataSourceEntry: DataSourceEntryCreateRequest = {
            key: this.form.value.key,
            value: this.form.value.value,
        };
        return this.getParentEntityId().pipe(
            switchMap((dataSourceId: string) => {
                return this.dataSourceEntries.create(dataSourceEntry, dataSourceId);
            }),
        ).toPromise();
    }

    private getParentEntityId(): Observable<string> {
        return combineLatest([
            this.focusEntityType$,
            this.focusEntityId$,
        ]).pipe(
            map(([focusEntityType, focusEntityId]) => focusEntityType === 'dataSource' ? focusEntityId : undefined),
            filter((id: string | undefined) => id != null),
            take(1),
        );
    }

}
