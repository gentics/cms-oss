import { RoleOperations } from '@admin-ui/core';
import { LanguageHandlerService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
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

    public supportedLanguages$: Observable<Language[]>;

    /** form instance */
    form: UntypedFormControl;

    constructor(
        private roleOperations: RoleOperations,
        private languageHandler: LanguageHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({});
        this.supportedLanguages$ = this.languageHandler.getSupportedLanguages();
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
