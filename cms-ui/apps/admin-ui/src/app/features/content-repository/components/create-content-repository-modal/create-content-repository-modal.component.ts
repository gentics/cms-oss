import { ContentRepositoryBO } from '@admin-ui/common';
import { ContentRepositoryHandlerService, ErrorHandler } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { EditableContentRepositoryProperties } from '@gentics/cms-models';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import { ContentRepositoryPropertiesMode } from '../content-repository-properties/content-repository-properties.component';

@Component({
    selector: 'gtx-create-content-repository-modal',
    templateUrl: './create-content-repository-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateContentRepositoryModalComponent extends BaseModal<ContentRepositoryBO> implements IModalDialog, OnInit {

    public readonly ContentRepositoryPropertiesMode = ContentRepositoryPropertiesMode;

    public form: FormControl<EditableContentRepositoryProperties>;
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
        this.form = new FormControl<EditableContentRepositoryProperties>({} as any);
    }

    /**
     * If user clicks to create a new contentRepository
     */
    async buttonCreateEntityClicked(): Promise<void> {
        const normalized = (this.handler).normalizeForREST(this.form.value as any);
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
