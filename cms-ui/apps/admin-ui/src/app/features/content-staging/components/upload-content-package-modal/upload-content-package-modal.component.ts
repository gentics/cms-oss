import { BO_ID, ContentPackageBO } from '@admin-ui/common';
import { ContentPackageOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy } from '@angular/core';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-upload-content-package-modal',
    templateUrl: './upload-content-package-modal.component.html',
    styleUrls: ['./upload-content-package-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class UploadContentPackageModalComponent extends BaseModal<void> implements OnDestroy {

    @Input()
    public contentPackage: ContentPackageBO;

    public file: File;
    public loading = false;

    private subscription: Subscription;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private entityOperations: ContentPackageOperations,
    ) {
        super();
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    fileSelected(file: File | File[]): void {
        if (file == null) {
            this.file = null;
        } else if (Array.isArray(file)) {
            this.file = file[0];
        } else {
            this.file = file;
        }
    }

    performUpload(): void {
        if (!this.file || this.loading) {
            return;
        }

        if (this.subscription) {
            this.subscription.unsubscribe();
        }

        this.loading = true;
        this.changeDetector.markForCheck();

        this.subscription = this.entityOperations.upload(this.contentPackage[BO_ID], this.file).subscribe(
            () => this.closeFn(),
            () => {
                /* Nothing to do, error notification is done in the upload function */
                this.loading = false;
                this.changeDetector.markForCheck();
            },
        );
    }
}
