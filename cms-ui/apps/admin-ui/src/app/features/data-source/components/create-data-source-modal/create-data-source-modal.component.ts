import { DataSourceHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { DataSource, DataSourceCreateRequest } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-data-source-modal',
    templateUrl: './create-data-source-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateDataSourceModalComponent extends BaseModal<DataSource> implements OnInit {

    /** form instance */
    form: UntypedFormControl;

    constructor(
        private handler: DataSourceHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({}, createNestedControlValidator());
    }

    /**
     * If user clicks to create a new dataSource
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(dataSourceCreated => this.closeFn(dataSourceCreated));
    }

    private createEntity(): Promise<DataSource> {
        // assemble payload with conditional properties
        const dataSource: DataSourceCreateRequest = {
            ...this.form.value,
            type: 'STATIC',
        };
        return this.handler.createMapped(dataSource).toPromise();
    }

}
