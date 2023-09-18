import { ContentRepositoryBO } from '@admin-ui/common';
import { ContentRepositoryHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import { ContentRepositoryPropertiesMode } from '../content-repository-properties/content-repository-properties.component';

@Component({
    selector: 'gtx-create-content-repository-modal',
    templateUrl: './create-content-repository-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateContentRepositoryModalComponent extends BaseModal<ContentRepositoryBO> implements IModalDialog, OnInit {

    public readonly ContentRepositoryPropertiesMode = ContentRepositoryPropertiesMode;

    /** form instance */
    form: UntypedFormControl;

    constructor(
        private handler: ContentRepositoryHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({});
    }

    /**
     * If user clicks to create a new contentRepository
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(contentRepositoryCreated => this.closeFn(contentRepositoryCreated));
    }

    private createEntity(): Promise<ContentRepositoryBO> {
        return this.handler.createMapped(this.form.value).toPromise();
    }

}
