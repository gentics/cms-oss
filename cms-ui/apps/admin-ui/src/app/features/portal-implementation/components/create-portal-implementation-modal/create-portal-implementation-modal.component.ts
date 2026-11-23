import { PortalImplementationBO } from '@admin-ui/common';
import { PortalImplementationHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { PortalImplementationCreateRequest } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-portal-implementation-modal',
    templateUrl: './create-portal-implementation-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class CreatePortalImplementationModalComponent extends BaseModal<PortalImplementationBO> implements OnInit {

    /** form instance */
    form: UntypedFormControl;

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: PortalImplementationHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new UntypedFormControl({});
    }

    /**
     * If user clicks to create a new portal implementation
     */
    buttonCreateEntityClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createEntity()
            .then(created => {
                this.closeFn(created);
            }, () => {
                this.form.enable({ emitEvent: false });
                this.loading = false;
                this.changeDetector.markForCheck();
            });
    }

    private createEntity(): Promise<PortalImplementationBO> {
        const value = this.form.value;
        const request: PortalImplementationCreateRequest = {
            keyword: value.keyword,
            name: value.name,
            description: value.description,
            type: value.type,
        };
        return this.handler.createMapped(request).toPromise();
    }
}
