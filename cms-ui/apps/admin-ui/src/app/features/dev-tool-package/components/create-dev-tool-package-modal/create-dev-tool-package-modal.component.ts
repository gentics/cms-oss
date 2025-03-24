import { DevToolPackageBO } from '@admin-ui/common';
import { DevToolPackageHandlerService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
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

    constructor(
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
        this.createPackage().then(pkg => this.closeFn(pkg.name));
    }

    private createPackage(): Promise<DevToolPackageBO> {
        // assemble payload with conditional properties
        const gtxPackage: PackageCreateRequest = {
            name: this.form.value.name,
        };
        return this.handler.createMapped(gtxPackage).toPromise();
    }

}
