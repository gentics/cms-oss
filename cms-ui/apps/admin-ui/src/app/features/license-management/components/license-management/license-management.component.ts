import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { License, LicenseCheckResult, LicenseStatus } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ModalService } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { ContentRepositoryLicenseTableLoaderService } from '../../providers';
import { LicenseUploadModal } from '../license-upload-modal/license-upload-modal.component';

@Component({
    selector: 'gtx-license-management',
    templateUrl: './license-management.component.html',
    styleUrls: ['./license-management.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LicenseManagementComponent implements OnInit, OnDestroy {

    @Output()
    public licenseChange = new EventEmitter<License | null>();

    public loading = false;
    public status: LicenseStatus = LicenseStatus.MISSING;
    public license?: License;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private modals: ModalService,
        private tableLoader: ContentRepositoryLicenseTableLoaderService,
    ) {}

    ngOnInit(): void {
        this.loadLicenseInfo();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    loadLicenseInfo(): void {
        // Don't load it multiple times
        if (this.loading) {
            return;
        }

        this.loading = true;
        this.changeDetector.markForCheck();

        this.subscriptions.push(this.client.license.info().subscribe({
            next: (info) => {
                this.status = info.checkResult.licenseResult;
                this.license = info.checkResult.license;
                this.loading = false;
                this.changeDetector.markForCheck();
                this.licenseChange.emit(this.license);
            },
            error: err => {
                console.error(err);
                this.status = LicenseStatus.MISSING;
                this.license = null;
                this.loading = false;
                this.changeDetector.markForCheck();
                this.licenseChange.emit(this.license);
            },
        }));
    }

    async openUploadModal(): Promise<void> {
        const modal = await this.modals.fromComponent(LicenseUploadModal, {
            closeOnOverlayClick: false,
        }, {});
        try {
            const res: LicenseCheckResult = await modal.open();

            this.status = res.licenseResult;
            this.license = res.license;
            this.changeDetector.markForCheck();
            this.licenseChange.emit(this.license);
            // In case that the license was pushed to the CRs, we have to reload their info in the tables.
            this.tableLoader.reload();
        } catch (err) {
            // Ignore closing notifications
            if (err instanceof ModalCloseError && err.reason !== ModalClosingReason.ERROR) {
                return;
            }
        }
    }
}
