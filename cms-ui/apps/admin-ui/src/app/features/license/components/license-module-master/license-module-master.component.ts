import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ModalCloseError, ModalClosingReason } from '@gentics/cms-integration-api-models';
import { License, LicenseCheckResult, LicenseStatus } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ModalService } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { v4 } from 'uuid';
import { LicenseUploadModal } from '../license-upload-modal/license-upload-modal.component';

const mockLicense: License = {
    uuid: window.crypto?.randomUUID?.() || v4(),
    features: {
        CN: 'Gentics Content.Node',
        TTM: 'Feature Tag Type Migration / Template migration',
    },
    issuedAt: 1730733206.000000000,
    validUntil: 0.0,
}

/*
{ "uuid":"a76bf124-44e0-4c21-b5ee-49a71ec01a66","features":{"CN":"Gentics Content.Node","TTM":"Feature Tag Type Migration / Template migration"},"iat":1730733206,"exp":0,"iss":"Gentics","sub":"Example"}
*/

@Component({
    selector: 'gtx-license-module-master',
    templateUrl: './license-module-master.component.html',
    styleUrls: ['./license-module-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LicenseModuleMasterComponent implements OnInit, OnDestroy {

    public loading = false;
    public status: LicenseStatus = LicenseStatus.MISSING;
    public license?: License;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private modals: ModalService,
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
                this.status = info.checkResult.status;
                this.license = info.checkResult.license;
                this.loading = false;
                this.changeDetector.markForCheck();
            },
            error: err => {
                console.error(err);
                this.status = LicenseStatus.VALID;
                this.license = mockLicense;
                this.loading = false;
                this.changeDetector.markForCheck();
            },
        }));
    }

    async openUploadModal(): Promise<void> {
        const modal = await this.modals.fromComponent(LicenseUploadModal, {
            closeOnOverlayClick: false,
        }, {});
        try {
            const res: LicenseCheckResult = await modal.open();

            this.status = res.status;
            this.license = res.license;
            this.changeDetector.markForCheck();
        } catch (err) {
            // Ignore closing notifications
            if (err instanceof ModalCloseError && err.reason !== ModalClosingReason.ERROR) {
                return;
            }
        }
    }
}
