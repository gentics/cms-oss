import { DataSourceHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
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

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: DataSourceHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({});
    }

    /**
     * If user clicks to create a new dataSource
     */
    buttonCreateEntityClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createEntity()
            .then(dataSourceCreated => {
                this.closeFn(dataSourceCreated);
            }, () => {
                this.form.enable({ emitEvent: false });
                this.loading = false;
                this.changeDetector.markForCheck();
            });
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
