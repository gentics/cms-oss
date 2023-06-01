import { RoleOperations } from '@admin-ui/core';
import { LanguageDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Language, RoleBO, RoleCreateRequest } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';

@Component({
    selector: 'gtx-create-role-modal',
    templateUrl: './create-role-modal.component.html',
    styleUrls: [ './create-role-modal.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateRoleModalComponent extends BaseModal<RoleBO> implements OnInit {

    /** Current step (tab) of the entity creation wizzard */
    currentTab = String(1);

    public supportedLanguages$: Observable<Language[]>;

    /** form instance */
    form: UntypedFormControl;

    constructor(
        private roleOperations: RoleOperations,
        private languageData: LanguageDataService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({}, createNestedControlValidator());
        this.supportedLanguages$ = this.languageData.watchSupportedLanguages();
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
        const role: RoleCreateRequest = {
            ...this.form.value,
        };
        return this.roleOperations.create(role).toPromise();
    }

}
