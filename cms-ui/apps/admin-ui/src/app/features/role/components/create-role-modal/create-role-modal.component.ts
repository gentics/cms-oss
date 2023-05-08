import { RoleOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Raw, RoleBO } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-role-modal',
    templateUrl: './create-role-modal.component.html',
    styleUrls: [ './create-role-modal.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateRoleModalComponent implements IModalDialog, OnInit {

    /** Current step (tab) of the entity creation wizzard */
    currentTab = String(1);

    /** form instance */
    form: UntypedFormGroup;

    constructor(
        private roleOperations: RoleOperations,
    ) {
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            name: new UntypedFormControl(null),
            description: new UntypedFormControl(null),
        });
    }

    closeFn = (entityCreated: RoleBO) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (entityCreated: RoleBO) => {
            close(entityCreated);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    /** Get form validity state */
    isValid(): boolean {
        return this.form.valid;
    }

    /** Programmatic tab set */
    setActiveTab(index: string): void {
        this.currentTab = String(index);
    }

    /**
     * Returns TRUE if parameter index is index of active tab
     */
    tabIndexIsActive(index: number): boolean {
        return this.currentTab === String(index);
    }

    /**
     * If user clicks to create a new role
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(roleCreated => this.closeFn(roleCreated));
    }

    private createEntity(): Promise<RoleBO> {
        // assemble payload with conditional properties
        const role: Partial<RoleBO<Raw>> = {
            name: this.form.value.name,
            description: this.form.value.description,
        };
        return this.roleOperations.create(role).toPromise();
    }

}
