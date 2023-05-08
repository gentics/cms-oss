import { DataSourceOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { DataSourceBO, DataSourceCreateRequest, Raw } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-data-source-modal',
    templateUrl: './create-data-source-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateDataSourceModalComponent implements IModalDialog, OnInit {

    /** form instance */
    form: UntypedFormGroup;

    constructor(
        private dataSources: DataSourceOperations,
    ) {
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            name: new UntypedFormControl(null),
        });
    }

    closeFn = (entityCreated: DataSourceBO) => {};
    cancelFn = (val?: any) => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (entityCreated: DataSourceBO) => {
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

    private createEntity(): Promise<DataSourceBO> {
        // assemble payload with conditional properties
        const dataSource: DataSourceCreateRequest = {
            name: this.form.value.name,
            type: 'STATIC',
        };
        return this.dataSources.create(dataSource).toPromise();
    }

}
