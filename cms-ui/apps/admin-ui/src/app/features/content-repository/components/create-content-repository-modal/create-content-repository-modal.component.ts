import { ContentRepositoryBO } from '@admin-ui/common';
import { ContentRepositoryHandlerService, ErrorHandler } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import {
    ContentRepositoryPropertiesFormData,
    ContentRepositoryPropertiesMode,
} from '../content-repository-properties/content-repository-properties.component';

@Component({
    selector: 'gtx-create-content-repository-modal',
    templateUrl: './create-content-repository-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateContentRepositoryModalComponent extends BaseModal<ContentRepositoryBO> implements IModalDialog, OnInit {

    public readonly ContentRepositoryPropertiesMode = ContentRepositoryPropertiesMode;

    public form: FormControl<ContentRepositoryPropertiesFormData>;
    public loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: ContentRepositoryHandlerService,
        private errorHandler: ErrorHandler,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new FormControl<ContentRepositoryPropertiesFormData>(null);
    }

    /**
     * If user clicks to create a new contentRepository
     */
    async buttonCreateEntityClicked(): Promise<void> {
        // Filter out property-type proeprties
        const { basepathType: _basepathType, urlType: _urlType, usernameType: _usernameType, ...formData } = this.form.value;
        // Normalize for REST call
        const normalized = (this.handler).normalizeForREST(formData as any);

        this.form.disable();
        this.loading = true;
        this.changeDetector.markForCheck();

        try {
            const created = await this.handler.createMapped(normalized as any).toPromise();
            this.closeFn(created);
        } catch (error) {
            this.form.enable();
            this.loading = false;
            this.changeDetector.markForCheck();
            this.errorHandler.catch(error, { notification: true });
        }
    }

}
