import { ErrorHandler } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { LicenseCheckResult } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseModal, FormProperties, setControlsEnabled } from '@gentics/ui-core';
import { lastValueFrom, Subscription } from 'rxjs';

interface UploadForm {
    useFile: boolean;
    licenseKey: string;
    licenseFile: File[];
    pushToContentRepositories: boolean;
}

function validateLicenseFormat(value: string): boolean {
    // JWT format is quite simple: [base64json].[base64json].[hash]
    const parts = value.split('.');
    if (parts.length !== 3) {
        return false;
    }

    try {
        const header = JSON.parse(atob(parts[0]));
        const payload = JSON.parse(atob(parts[1]));

        // Algorithm has to be set, issuer has to be correct, and a hash has to be present
        return typeof header.alg === 'string'
            && payload.iat > 0
            && parts[2].length > 32;
    } catch (err) {
        return false;
    }
}

@Component({
    selector: 'gtx-license-upload-modal',
    templateUrl: './license-upload-modal.component.html',
    styleUrls: ['./license-upload-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LicenseUploadModal extends BaseModal<LicenseCheckResult> implements OnInit, OnDestroy {

    public loading = false;
    public form: FormGroup<FormProperties<UploadForm>>;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private client: GCMSRestClientService,
        private errorHandler: ErrorHandler,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormGroup<FormProperties<UploadForm>>({
            useFile: new FormControl(true),
            licenseFile: new FormControl([], [Validators.required, Validators.minLength(1)]),
            licenseKey: new FormControl({ value: '', disabled: true }, [Validators.required, (ctrl) => {
                return validateLicenseFormat(ctrl.value) ? null : { invalidFormat: true };
            }]),
            pushToContentRepositories: new FormControl(false),
        });

        this.subscriptions.push(this.form.controls.useFile.valueChanges.subscribe(useFile => {
            if (this.form.enabled) {
                this.configureForm({ useFile });
                this.changeDetector.markForCheck();
            }
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    protected configureForm(value: Partial<UploadForm>): void {
        setControlsEnabled(this.form, ['licenseFile'], value.useFile);
        setControlsEnabled(this.form, ['licenseKey'], !value.useFile);
        this.form.updateValueAndValidity();
    }

    async upload(): Promise<void> {
        if (this.loading) {
            return;
        }

        const value = this.form.value;

        this.form.disable();
        this.loading = true;
        this.changeDetector.markForCheck();

        let licenseKey: string;
        if (value.useFile) {
            licenseKey = (await value.licenseFile[0].text() || '').trim();
        } else {
            licenseKey = value.licenseKey;
        }

        try {
            const res = await lastValueFrom(this.client.license.update({
                licenseKey,
                pushToContentRepositories: value.pushToContentRepositories,
            }));
            this.closeFn(res.checkResult);
        } catch (err) {
            this.errorHandler.catch(err);

            this.form.enable();
            this.configureForm(this.form.value);
            this.loading = false;
            this.changeDetector.markForCheck();
        }
    }
}
