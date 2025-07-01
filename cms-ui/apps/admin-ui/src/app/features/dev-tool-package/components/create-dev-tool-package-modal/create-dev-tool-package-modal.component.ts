import { DevToolPackageBO } from '@admin-ui/common';
import { DevToolPackageHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { PackageCreateRequest } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-dev-tool-package-modal',
    templateUrl: './create-dev-tool-package-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateDevToolPackageModalComponent extends BaseModal<string> implements OnInit {

    /** form instance */
    form: UntypedFormGroup;

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: DevToolPackageHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            name: new UntypedFormControl( null ),
        });
    }

    /**
     * If package clicks to create a new package
     */
    buttonCreatePackageClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createPackage().then(pkg => {
            this.closeFn(pkg.name);
        }, () => {
            this.form.enable({ emitEvent: false });
            this.loading = false;
            this.changeDetector.markForCheck();
        });
    }

    private createPackage(): Promise<DevToolPackageBO> {
        // assemble payload with conditional properties
        const gtxPackage: PackageCreateRequest = {
            name: this.form.value.name,
        };
        return this.handler.createMapped(gtxPackage).toPromise();
    }

}
