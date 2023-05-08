import { PackageOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { AbstractControl, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { PackageCreateRequest } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-package-modal',
    templateUrl: './create-package-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreatePackageModalComponent extends BaseModal<string> implements OnInit {

    /** form instance */
    form: UntypedFormGroup;

    /** Convenience getter for confirm packagename input */
    get packageName(): AbstractControl {
        return this.form.get('name');
    }

    constructor(
        private entityOperations: PackageOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            name: new UntypedFormControl( null ),
        });
    }

    /** Get form validity state */
    formIsValid(): boolean {
        return this.form.valid;
    }

    /** Get form validity state */
    allIsValid(): boolean {
        return this.formIsValid();
    }

    /**
     * If package clicks to create a new package
     */
    buttonCreatePackageClicked(): void {
        this.createPackage().then(() => this.closeFn(this.packageName.value));
    }

    private createPackage(): Promise<void> {
        // assemble payload with conditional properties
        const gtxPackage: PackageCreateRequest = {
            name: this.form.value.name,
        };
        return this.entityOperations.addPackage(gtxPackage).toPromise();
    }

}
