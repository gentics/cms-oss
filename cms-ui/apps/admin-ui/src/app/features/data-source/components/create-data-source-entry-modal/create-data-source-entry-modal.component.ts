import { DataSourceEntryHandlerService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { DataSourceEntry, DataSourceEntryCreateRequest, Raw } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-data-source-entry-modal',
    templateUrl: './create-data-source-entry-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateDataSourceEntryModalComponent extends BaseModal<DataSourceEntry<Raw>> implements OnInit {

    @Input()
    public datasourceId: string | number;

    /** form instance */
    form: UntypedFormControl;

    constructor(
        private handler: DataSourceEntryHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new UntypedFormControl({});
    }

    /**
     * If user clicks to create a new dataSource
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(dataSourceCreated => this.closeFn(dataSourceCreated));
    }

    private createEntity(): Promise<DataSourceEntry> {

        // assemble payload with conditional properties
        const dataSourceEntry: DataSourceEntryCreateRequest = this.form.value;
        return this.handler.createMapped(this.datasourceId, dataSourceEntry).toPromise();
    }
}
